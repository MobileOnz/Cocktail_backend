#!/usr/bin/env bash
# ONZ 백엔드 자동 배포 — origin/dev 를 따라간다.
#
# 왜 당겨오는 방식인가
#   GitHub Actions 의 CD 는 러너가 EC2 로 SSH 를 열어야 하는데, 러너의 출발지 IP 가
#   수시로 바뀌어 보안그룹에 고정할 수 없다. 사람 IP 를 넣어도 바뀌면 또 끊긴다.
#   방향을 뒤집으면 나가는 연결만 쓰므로 인바운드도 IP 허용목록도 필요 없다.
#   레포가 공개라 self-hosted 러너는 쓰지 않는다 — 포크 PR 로 남이 서버에서
#   코드를 실행시킬 수 있다(GitHub 공식 권고).
#
# 안전장치 (전부 실제로 겪은 사고에서 나왔다)
#   1. 추적 파일이 수정돼 있으면 아무것도 하지 않는다.
#      서버의 docker-compose.yml 이 git 과 달랐던 적이 있고, 그걸 덮어쓰자
#      api-server 가 nginx 와 80 번 포트를 다퉈 프로덕션이 502 로 내려갔다.
#   2. fast-forward 가 아니면 멈춘다. 서버에서 히스토리가 갈린 상태를 자동으로
#      풀려고 들면 더 나빠진다.
#   3. 마이그레이션 전에 DB 를 덤프한다. 실패하면 배포를 시작하지 않는다.
#   4. api-server 만 갈아끼운다. nginx 는 건드리지 않는다 — 인증서를 물고 있어서
#      재생성되면 HTTPS 가 끊긴다.
#   5. 헬스가 돌아오지 않으면 직전 커밋으로 되돌려 다시 띄운다.

set -uo pipefail

REPO=/root/cocktail-api
BRANCH=dev
LOG=/var/log/onz-autodeploy.log
LOCK=/var/lock/onz-autodeploy.lock
HEALTH=http://localhost/onz/api/v2/magazine?size=1

log() { echo "$(date -Is) $*" >> "$LOG"; }

exec 9>"$LOCK"
flock -n 9 || exit 0

cd "$REPO" || { log "FAIL 저장소 없음: $REPO"; exit 1; }

# ── 안전장치 1: 서버에만 있는 수정이 있으면 손대지 않는다
DIRTY=$(git status --porcelain --untracked-files=no)
if [ -n "$DIRTY" ]; then
    log "HALT 추적 파일이 수정돼 있다. 자동 배포를 건너뛴다:"
    echo "$DIRTY" | sed 's/^/    /' >> "$LOG"
    exit 1
fi

git fetch -q origin "$BRANCH" 2>>"$LOG" || { log "FAIL git fetch"; exit 1; }

LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse "origin/$BRANCH")
[ "$LOCAL" = "$REMOTE" ] && exit 0

# ── 안전장치 2: fast-forward 만
if ! git merge-base --is-ancestor "$LOCAL" "$REMOTE"; then
    log "HALT fast-forward 불가 (local=${LOCAL:0:8} remote=${REMOTE:0:8})"
    exit 1
fi

log "START ${LOCAL:0:8} -> ${REMOTE:0:8}"

# ── 안전장치 3: 마이그레이션 전 백업
BACKUP="$REPO/backup_autodeploy_$(date +%Y%m%d_%H%M%S).sql"
if docker exec postgres-cocktail bash -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > "$BACKUP" 2>>"$LOG"; then
    log "  백업 $(basename "$BACKUP") ($(du -h "$BACKUP" | cut -f1))"
else
    log "FAIL 백업 실패 — 배포하지 않는다"
    rm -f "$BACKUP"; exit 1
fi
ls -1t "$REPO"/backup_autodeploy_*.sql 2>/dev/null | tail -n +11 | xargs -r rm -f

git merge --ff-only "origin/$BRANCH" >>"$LOG" 2>&1 || { log "FAIL merge"; exit 1; }

if ! docker compose build api-server >>"$LOG" 2>&1; then
    log "FAIL 빌드 실패 — 컨테이너는 이전 이미지로 계속 돈다"
    git reset -q --hard "$LOCAL"   # 추적 파일은 깨끗함이 위에서 보장됐다
    exit 1
fi

# ── 안전장치 4: api-server 만. nginx 는 제외한다
if ! docker compose up -d --no-deps api-server >>"$LOG" 2>&1; then
    log "FAIL 기동 실패"
    git reset -q --hard "$LOCAL"
    exit 1
fi

# ── 안전장치 5: 헬스 확인, 실패 시 되돌린다
for i in $(seq 1 36); do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$HEALTH" || true)
    if [ "$code" = "200" ]; then
        log "DONE ${REMOTE:0:8} (헬스 200, ${i}회차)"
        docker image prune -f >/dev/null 2>&1
        exit 0
    fi
    sleep 5
done

log "ROLLBACK ${REMOTE:0:8} 헬스 실패 → ${LOCAL:0:8} 로 되돌린다"
git reset -q --hard "$LOCAL"
docker compose build api-server >>"$LOG" 2>&1
docker compose up -d --no-deps api-server >>"$LOG" 2>&1
for i in $(seq 1 36); do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$HEALTH" || true)
    [ "$code" = "200" ] && { log "  롤백 성공 (헬스 200)"; exit 1; }
    sleep 5
done
log "  ROLLBACK 도 실패 — 사람이 봐야 한다"
exit 1
