"""Freeze the latest measured run into a small reviewable report (no invented metrics)."""
import csv
import hashlib
import json
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
build=ROOT/'build/hybrid-search'
out=ROOT/'docs/hybrid-search'
r=json.loads((build/'evaluation.json').read_text())
totals={key:0 for key in ['tests','failures','errors','skipped']}
for path in (ROOT/'build/test-results/test').glob('TEST-*.xml'):
    suite=ET.parse(path).getroot()
    for key in totals: totals[key]+=int(suite.attrib[key])
log=(build/'checks.log').read_text()
python_tests=re.search(r'Ran (\d+) tests',log)
if not python_tests or 'BUILD SUCCESSFUL' not in log or totals['failures'] or totals['errors']:
    raise SystemExit('A complete passing checks.log and JUnit run are required')
reviews=list(csv.DictReader((build/'review.csv').open(encoding='utf-8-sig')))
shutil.copyfile(build/'review.csv',out/'review.csv')
compact={key:r[key] for key in ['model','dimension','evaluationScope','indexing']}
compact['tests']=totals
compact['pythonTests']=int(python_tests[1])
compact['sourceSha256']=hashlib.sha256((ROOT/'data/hybrid-search/onz_cocktails_105.csv').read_bytes()).hexdigest()
compact['modes']=[]
for mode in r['modes']:
    m={k:v for k,v in mode.items() if k!='results'}
    m['results']=[]
    for result in mode['results']:
        response=result['response']
        m['results'].append(dict(query=result['query'], relevantIds=result['relevantIds'], top5Hit=result['top5Hit'],filters=response['filters'],
            items=[{key:item[key] for key in ['cocktailId','nameKo','nameEn','matchReasons','debug']} for item in response['items']]))
    compact['modes'].append(m)
for key in ['exactScanExplain','hnswExplain']:
    plan=json.loads(r[key][0]['QUERY PLAN']['value'])[0]
    def strip_vectors(node):
        if isinstance(node,dict):return {k:strip_vectors(v) for k,v in node.items() if k not in ['Sort Key','Order By']}
        if isinstance(node,list):return [strip_vectors(v) for v in node]
        return node
    compact[key]=strip_vectors(plan)
compact['apiSample']=r['apiSample']
(out/'evaluation.json').write_text(json.dumps(compact,ensure_ascii=False,indent=2))
lines=['# 실제 구현 및 검증 결과','',
    '검증일: 2026-10-23. Java 17 / PostgreSQL 17.11 / pgvector 0.8.7 / 로컬 Ollama Qwen3 0.6B. 실제 모델 응답에서 **1024차원** 확인. 운영 DB/배포 설정은 변경하지 않았으며 임시 PostgreSQL 스키마에서 실제 SQL과 API를 실행했다.','',
    '## 데이터 및 테스트','',
    f'- 원본 CSV: 105행, 26컬럼, 고유 ID 105개. SHA-256 `{compact["sourceSha256"]}`.',
    '- 실제 로컬 모델 색인: 105/105 성공, 오류 0. 재실행 시 변경 없는 문서는 0건 재임베딩. 변경 1건 재색인·강제 재색인·dry-run·임베딩 실패 시 기존 레코드 보존 검증.',
    f'- 정규화 수동 검토: **{len(reviews)}항목 / {len({x["cocktail_id"] for x in reviews})}개 칵테일**. [원문 CSV](review.csv). 원문을 임의 수정하지 않음.',
    f'- Java 전체 회귀/통합 테스트: **{totals["tests"]} PASS, 0 FAIL, 0 SKIP**. Python 정규화 테스트: **{python_tests[1]} PASS**.',
    '- 실제 DB 마이그레이션, 키워드/코사인/RRF SQL, 이름/별칭, 정확 이름 우선, 접두 검색, 복수 베이스, 필수 재료 AND, 제외/도수 필터, 후보 절단 전 필터링, 페이지 일관성, RRF 기여 0, 모델/차원 불일치, 임베딩 장애 fallback, HTTP 타임아웃, API 400/403 계약, SQL 인젝션 입력을 검증했다.',
    '- Spring MockMvc로 실제 컨트롤러/ResponseDto 경로를 호출했다. 실제 로컬 모델과 DB로 `모히토` 요청 HTTP 200 / id=15 / mode=hybrid를 확인했다. 운영 HTTP 배포·실제 모바일 UI는 검증 범위 밖이다.','',
    '## 모드별 실측','',
    '25개 고정 smoke 질의. Top-5 성공은 사전에 지정한 관련 ID가 1개 이상 나오는 비율이다. 이름 top-1은 7개 질의다. 소규모 기능 확인용 라벨이며 독립된 사용자 품질 평가가 아니다. 워밍업 후 서비스 호출 1회씩 측정했으며 HTTP 전송 시간은 제외한다.','',
    '| 모드 | Top-5 | 이름 Top-1 | 필터 위반 | p50 | p95 |', '|---|---:|---:|---:|---:|---:|']
for m in r['modes']:
    lines.append(f'| {m["mode"]} | {m["top5Hits"]}/25 ({m["top5Hits"]/25:.0%}) | {m["nameTop1Hits"]}/7 | {m["filterViolations"]} | {m["p50Ms"]:.1f} ms | {m["p95Ms"]:.1f} ms |')
lines+=['','이 평가에서는 하이브리드가 키워드 단독보다 좋은 결과를 내지 않았다. 초기 RRF 가중치를 사용자 질의 라벨로 추가 조정해야 한다. 하이브리드 미적중 질의 `라임 없이`는 반환 결과가 라임 제외 조건은 지켰지만 좁게 지정된 관련 ID 집합에 들지 않은 경우다. 필터 준수와 관련성 라벨 적중을 분리해 기록했다.','',
    f'벡터 Top-5 EXPLAIN ANALYZE: exact scan **{compact["exactScanExplain"]["Execution Time"]:.3f} ms**, HNSW **{compact["hnswExplain"]["Execution Time"]:.3f} ms**. 105건 단일 워밍 실행 비교이며 ANN 품질/규모 벤치마크는 아니다. HNSW는 테스트 중에만 생성/삭제했고 운영 마이그레이션은 정확 선형 스캔을 유지했다.','',
    '## 하이브리드 실제 Top 5','', '| 질의 | 결과 (순서대로, ID) |','|---|---|']
hybrid=next(m for m in r['modes'] if m['mode']=='hybrid')
for result in hybrid['results']:
    lines.append('| '+result['query']+' | '+', '.join(f'{i["nameKo"]} ({i["cocktailId"]})' for i in result['response']['items'])+' |')
lines+=['','## 대표 결과의 원문 근거','']
for q in ['화이트 럼 라임','진 베이스 상큼한 칵테일','진과 레몬이 들어간','달지 않고 허브 향이 있는 칵테일']:
    result=next(x for x in hybrid['results'] if x['query']==q)
    lines+=['### '+q,'']
    for i in result['response']['items']:
        reasons=i['matchReasons'][:4]
        lines.append(f'- **{i["nameKo"]} ({i["cocktailId"]})**: '+ '; '.join(reasons))
    lines+=['']
lines+=['## 파일 및 재현','',
    '- [실행·마이그레이션·운영 가이드](README.md)',
    '- [축약 평가 JSON / 실제 API 응답](evaluation.json)',
    '- 원본 상세 평가/SQL 계획: `build/hybrid-search/evaluation.json`',
    '- 테스트 로그: `build/hybrid-search/checks.log`, HTML: `build/reports/tests/test/index.html`',
    '- 재현: README 환경변수 설정 후 `bash scripts/hybrid_search/run_checks.sh`; 이후 `python3 scripts/hybrid_search/report.py`.', '',
    '수정/추가 파일:', '']
files=['README.md','build.gradle','src/main/java/com/application/common/auth/config/EndpointAuthorizationPolicy.java','src/test/java/com/application/contract/EndpointAuthorizationContractTest.java']
for directory in ['data/hybrid-search','scripts/hybrid_search','src/main/java/com/application/domain/cocktail/search','src/main/resources/search','src/main/resources/db/hybrid','src/test/java/com/application/domain/cocktail/search','docs/hybrid-search']:
    files += [str(p.relative_to(ROOT)) for p in (ROOT/directory).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
files+=['docs/hybrid-search/RESULTS.md']
lines += ['- `'+f+'`' for f in sorted(set(files))]
lines+=['','기존 사용자 변경 `.gitignore`와 제공된 구현 프롬프트 파일은 보존했으며 이번 구현에서 수정하지 않았다.','',
    '## 남은 문제와 운영 반영 순서','',
    '1. 검토 9항목은 원문 확인이 필요하다. 특히 드라이 베르무트 수량 누락, 스위트 베르 약어, 130ml/1oz 불일치, 일부 장식 판정은 미확정이다.',
    '2. 첨부 105개만 색인했다. 운영 DB의 다른 칵테일과 관리자 수정 자동 동기화는 별도 백필/운영 작업이 필요하다.',
    '3. 모델 태그 변경을 자동 감지하는 digest 추적은 없다. 운영 모델 digest와 embedding.version을 고정하고 변경 시 전체 재색인해야 한다.',
    '4. 운영 PostgreSQL에 pgvector 설치·권한 확인 → 마이그레이션 → ID/이름 dry-run → 전체 색인 → ONZ_SEARCH_ENABLED=true → 실제 운영 HTTP/앱 호출 순으로 적용한다. 기존 Docker는 디스크 I/O 오류라 이번 실행에 사용하지 못했다.',
    '5. 대규모 데이터·동시 요청 부하·콜드 스타트는 미측정이다. 자연어 규칙은 명시된 패턴에 한정되며 단맛 보장은 없다.','']
(out/'RESULTS.md').write_text('\n'.join(lines))
print(json.dumps(dict(java=totals,python_tests=int(python_tests[1]),review_items=len(reviews),report=str(out/'RESULTS.md')),ensure_ascii=False))
