"""CSV -> deterministic search documents. Python standard library only; no DB writes."""
import argparse
import csv
import hashlib
import html
import json
from pathlib import Path
import re
import unicodedata

ROOT = Path(__file__).resolve().parents[2]
DICTIONARY = ROOT / 'src/main/resources/search/dictionary.json'
HEADERS = 'id 이름_한글 이름_영문 도수구간 최소도수 최대도수 스타일 베이스 맛단계 잔 계절 재료_원문 재료_목록 맛 분위기 제조단계수 제조순서 제조단계_출처 추천수 어려워요수 북마크수 유래 이미지_URL 잔이미지_URL 등록일 수정일'.split()

def norm(value):
    return re.sub(r'\s+', ' ', unicodedata.normalize('NFKC', value).casefold().replace('’', "'").replace('‘', "'")).strip()

def load_dictionary():
    return json.loads(DICTIONARY.read_text())

def ingredients(raw, dictionary):
    clean = html.unescape(re.sub(r'<br\s*/?>', ',', raw, flags=re.I))
    # Commas inside parentheses belong to the same item.
    parts = re.split(r',(?=[^()]*(?:\(|$))', clean)
    result = []
    for original in filter(str.strip, parts):
        original = original.strip()
        text = norm(original)
        optional = bool(re.search(r'옵션|선택|optional', text))
        alternative = '또는' in text
        # Keep descriptors (그린, 화이트, 신선); remove quantities and choice annotations.
        name = re.sub(r'\([^)]*(?:oz|옵션|선택|장식|가니시)[^)]*\)', '', text)
        name = re.sub(r'\s+(?:\d[\d./⁄\s-]*|[½⅓⅔¼¾⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞]+)\s*(?:ml|oz|tsp|tbsp|dash(?:es)?|drops?|개|장|조각|샷|방울)\b.*$', '', name)
        name = re.sub(r'(?<=\D)\d[\d./⁄\s-]*(?:ml|oz|tsp|tbsp|dash(?:es)?|drops?|개|장|조각|샷).*$', '', name)
        name = re.sub(r'\s*(?:약간|몇 방울|한 개)\s*$', '', name).strip()
        garnish = name in dictionary['garnishes'] or bool(re.search(r'장식|가니시|슬라이스|웨지|제스트|(?:^| )필(?: |$)|껍질| 림', name))
        names = [norm(n) for n in name.split('또는')]
        # Explicit shared-suffix alternatives; never infer from taste tags.
        names = [dictionary['ingredient_aliases'].get(n, n) for n in names]
        review = []
        volume = re.search(r'([0-9.]+)\s*ml\s*\(([^)]*)oz', original, flags=re.I)
        if volume:
            ounce = re.fullmatch(r'(\d+(?:\.\d+)?)?([½⅓⅔¼¾⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞])?', re.sub(r'\s+', '', volume[2]))
            if ounce and any(ounce.groups()):
                ml_equivalent = 30 * (float(ounce[1] or 0) + (unicodedata.numeric(ounce[2]) if ounce[2] else 0))
                if abs(float(volume[1]) - ml_equivalent) > max(5, ml_equivalent * .2):
                    review.append('source ml/oz quantities disagree; original preserved')
        if '/' in name and not alternative:
            review.append('ambiguous slash-separated ingredient')
        if re.search(r'\d|ml$|[<>]', name) and name not in dictionary['ingredient_parents']:
            review.append('unparsed quantity or descriptor')
        if name in dictionary['review_terms'] and ('unmeasured' not in dictionary['review_terms'][name] or not re.search(r'\d', text)):
            review.append(dictionary['review_terms'][name])
        if not re.search(r'\d|[½⅓⅔¼¾]|약간|몇 방울|한 개', text) and not garnish and dictionary['ingredient_aliases'].get(name, name) not in dictionary['unmeasured_core'] and not optional:
            review.append('unmeasured ingredient; core/garnish uncertain')
        for n in names:
            entities = list(dict.fromkeys([n] + dictionary['ingredient_parents'].get(n, [])))
            result.append(dict(original=original, name=n, entities=entities, optional=optional,
                               alternative=alternative, garnish=garnish, needs_review=bool(review),
                               review='; '.join(review), source='재료_원문'))
    return result

def read_csv(path):
    with open(path, encoding='utf-8-sig', newline='') as f:
        reader = csv.DictReader(f)
        if reader.fieldnames != HEADERS:
            raise ValueError('CSV headers differ from the specified 26 columns')
        rows = list(reader)
    if len(rows) != 105 or len({int(r['id']) for r in rows}) != 105:
        raise ValueError('Expected exactly 105 unique cocktail IDs')
    for r in rows:
        if None in r or any(r[k] is None for k in HEADERS):
            raise ValueError('Malformed CSV row')
        if any(not r[k].strip() for k in ['이름_한글','이름_영문','맛','재료_원문','베이스','분위기']):
            raise ValueError('Missing required source field')
        if not 0 <= int(r['최소도수']) <= int(r['최대도수']) <= 100:
            raise ValueError('Invalid ABV range')
    return rows

def documents(path):
    dictionary = load_dictionary()
    docs = []
    for r in read_csv(path):
        parsed = ingredients(r['재료_원문'], dictionary)
        taste = [x.strip() for x in r['맛'].split(',') if x.strip()]
        aroma = [x for x in taste if '향' in x or x == '플로럴']
        bases = list(dict.fromkeys(dictionary['base_aliases'].get(norm(x), norm(x)) for x in re.split('[,/]', r['베이스'])))
        terms = []
        for name in [r['이름_한글'],r['이름_영문']]:
            terms.append(dict(type='name', original=name, normalized=norm(name), required=False, source='이름'))
        for alias in dictionary['name_aliases'].get(r['이름_영문'], []):
            terms.append(dict(type='alias', original=alias, normalized=norm(alias), required=False, source='dictionary:name_aliases'))
        for base in bases:
            terms.append(dict(type='base', original=r['베이스'], normalized=base, required=True, source='베이스'))
        for p in parsed:
            for entity in p['entities']:
                terms.append(dict(type='ingredient', original=p['original'], normalized=entity,
                                  required=not any(p[k] for k in ['optional','alternative','garnish','needs_review']),
                                  source='재료_원문/dictionary', **{k:p[k] for k in ['optional','alternative','garnish','needs_review']}))
        profile = [f"칵테일: {r['이름_한글']} / {r['이름_영문']}", f"베이스: {', '.join(bases)}", f"맛·질감: {', '.join(taste)}"]
        if aroma:
            profile.append('향: ' + ', '.join(aroma))
        profile += ['사용 재료: ' + ', '.join(p['name'] + (' (대체)' if p['alternative'] else '') + (' (선택)' if p['optional'] else '') + (' (장식)' if p['garnish'] else '') for p in parsed),
                    '분위기: ' + r['분위기'], f"특징: {r['베이스']} 베이스. {r['맛']}."]
        doc = dict(id=int(r['id']), name_ko=r['이름_한글'], name_en=r['이름_영문'], base_raw=r['베이스'], bases=bases,
                   abv_min=int(r['최소도수']), abv_max=int(r['최대도수']), abv_band=r['도수구간'], style=r['스타일'], season=r['계절'],
                   taste_tags=taste, aroma_tags=aroma, mood_tags=[x.strip() for x in r['분위기'].split(',')], ingredients_raw=r['재료_원문'],
                   ingredients=parsed, terms=terms, image_url=r['이미지_URL'], profile_text='\n'.join(profile), normalization_version='onz-profile-v1',
                   dictionary_hash=hashlib.sha256(DICTIONARY.read_bytes()).hexdigest())
        doc['content_hash'] = hashlib.sha256(json.dumps(doc, sort_keys=True, ensure_ascii=False).encode()).hexdigest()
        docs.append(doc)
    return docs

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('csv', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT/'build/hybrid-search/documents.json')
    args = parser.parse_args()
    docs = documents(args.csv)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(docs, ensure_ascii=False, indent=2))
    reviews = [dict(cocktail_id=d['id'], name=d['name_ko'], original=p['original'], reason=p['review']) for d in docs for p in d['ingredients'] if p['needs_review']]
    with args.output.with_name('review.csv').open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['cocktail_id','name','original','reason'])
        writer.writeheader(); writer.writerows(reviews)
    print(json.dumps(dict(documents=len(docs), errors=0, review_items=len(reviews), output=str(args.output)), ensure_ascii=False))
