# 배포

## 왜 서버가 당겨오나

GitHub Actions 의 CD(`cd.yml`)는 러너가 EC2 로 SSH 를 연다. 그런데 러너의 출발지 IP 가
수시로 바뀌어 보안그룹에 고정할 수가 없다. 사람 IP 를 등록해도 바뀌면 또 끊긴다.
실제로 2026-08-23 부터 배포가 계속 실패했고, 그때마다 손으로 올렸다.

방향을 뒤집는다. **서버가 GitHub 을 내다본다.** 나가는 연결만 쓰므로 인바운드 포트도,
IP 허용목록도 필요 없다. IP 가 바뀌어도 상관없다.

self-hosted 러너는 쓰지 않는다. 이 저장소가 **공개**라, 누구나 포크 PR 로 러너에서
코드를 실행시킬 수 있다(GitHub 이 공식적으로 권고하지 않는 구성이다).

## 설치

```bash
sudo install -m 0755 deploy/onz-autodeploy.sh /usr/local/bin/
sudo install -m 0644 deploy/onz-autodeploy.{service,timer} /etc/systemd/system/
sudo touch /var/log/onz-autodeploy.log
sudo systemctl daemon-reload
sudo systemctl enable --now onz-autodeploy.timer
```

3분마다 `origin/dev` 를 확인하고, 앞서 있으면 fast-forward 로 당겨 빌드·재기동한다.

## 안전장치

전부 실제로 겪은 사고에서 나왔다.

| | |
|---|---|
| 추적 파일이 수정돼 있으면 중단 | 서버의 `docker-compose.yml` 이 git 과 달랐고, 그걸 덮어쓰자 api-server 가 nginx 와 80 번을 다퉈 프로덕션이 502 로 내려갔다 |
| fast-forward 아니면 중단 | 서버에서 히스토리가 갈린 걸 자동으로 풀려 들면 더 나빠진다 |
| 마이그레이션 전 DB 덤프 | 실패하면 배포를 시작하지 않는다. 최근 10개만 보관 |
| `--no-deps api-server` 만 | nginx 는 인증서를 물고 있어 재생성되면 HTTPS 가 끊긴다 |
| 헬스 실패 시 자동 롤백 | 직전 커밋으로 되돌려 다시 띄운다 |

## 상태 보기

```bash
sudo systemctl list-timers onz-autodeploy.timer
sudo tail -f /var/log/onz-autodeploy.log
sudo systemctl start onz-autodeploy.service   # 즉시 1회 실행
```

## 끌 때

```bash
sudo systemctl disable --now onz-autodeploy.timer
```

## 주의 — docker-compose.yml 은 서버와 정확히 같아야 한다

서버에만 있는 수정이 있으면 자동 배포가 멈춘다(위 안전장치 1). 설정을 바꿀 일이 생기면
**서버에서 직접 고치지 말고 이 저장소에 커밋해서 내려보낸다.**

특히 `nginx` 서비스 정의는 실제 컨테이너와 같아야 한다. 443 과 `/etc/letsencrypt`
마운트가 빠진 채로 `docker compose up -d` 를 돌리면 nginx 가 인증서 없이 다시 떠서
HTTPS 가 통째로 끊긴다.
