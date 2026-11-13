import unittest
from pathlib import Path
from normalize import ROOT, documents, ingredients, load_dictionary, norm, read_csv

class NormalizeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.docs=documents(ROOT/'data/hybrid-search/onz_cocktails_105.csv')
        cls.dictionary=load_dictionary()
    def test_source_contract_and_determinism(self):
        rows=read_csv(ROOT/'data/hybrid-search/onz_cocktails_105.csv')
        self.assertEqual((len(rows),len(rows[0]),len({d['id'] for d in rows})),(105,26,105))
        self.assertTrue(all(not d['맛단계'] and not d['재료_목록'] for d in rows))
        self.assertEqual(self.docs,documents(ROOT/'data/hybrid-search/onz_cocktails_105.csv'))
        self.assertTrue(all(r['유래'] not in d['profile_text'] for r,d in zip(rows,self.docs)))
    def test_mojito_and_entities(self):
        d=next(d for d in self.docs if d['id']==15)
        required={t['normalized'] for t in d['terms'] if t['type']=='ingredient' and t['required']}
        self.assertTrue({'화이트 럼','럼','라임','민트'}<=required)
        self.assertIn('모히토',{t['normalized'] for t in d['terms']})
        ginger=ingredients('진저 비어 120ml (4 oz)',self.dictionary)[0]
        self.assertNotIn('진',ginger['entities'])
    def test_choice_garnish_quantity_and_unicode(self):
        parsed=ingredients('버번 또는 라이 위스키 45ml (1½ oz), 계란흰자(옵션), 라임 슬라이스,<br>압생트 ½ tsp, 앙고스투라 비터 2 dashes',self.dictionary)
        self.assertEqual([p['name'] for p in parsed],['버번 위스키','라이 위스키','계란 흰자','라임 슬라이스','압생트','앙고스투라 비터'])
        self.assertTrue(parsed[0]['alternative'] and parsed[1]['alternative'])
        self.assertTrue(parsed[2]['optional']); self.assertTrue(parsed[3]['garnish'])
        self.assertEqual(norm('  ＭＯＪＩＴＯ\u00a0  '),'mojito')
        self.assertEqual(norm('Piña Colada'),'piña colada')
    def test_aroma_is_explicit_and_multiple_bases(self):
        for d in self.docs:
            self.assertTrue(set(d['aroma_tags'])<=set(d['taste_tags']))
            self.assertTrue(all('향' in x or x=='플로럴' for x in d['aroma_tags']))
        self.assertEqual(next(d for d in self.docs if d['id']==77)['bases'],['보드카','진','럼','테킬라'])
    def test_quoted_multiline_and_duplicate_ids(self):
        import csv
        import tempfile
        rows=read_csv(ROOT/'data/hybrid-search/onz_cocktails_105.csv')
        rows[0]['유래']='첫 줄, 쉼표\n둘째 줄 "인용"'
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'source.csv'
            def write():
                with path.open('w',encoding='utf-8',newline='') as f:
                    writer=csv.DictWriter(f,fieldnames=list(rows[0])); writer.writeheader(); writer.writerows(rows)
            write()
            self.assertEqual(read_csv(path)[0]['유래'],rows[0]['유래'])
            rows[1]['id']=rows[0]['id']; write()
            with self.assertRaises(ValueError): read_csv(path)
    def test_ambiguous_data_remains_reviewable(self):
        p=ingredients('드라이 베르무트ml (⅓ oz)',self.dictionary)[0]
        self.assertTrue(p['needs_review'])
        self.assertEqual(p['original'],'드라이 베르무트ml (⅓ oz)')
        self.assertTrue(ingredients('다크 럼 130ml (1 oz)',self.dictionary)[0]['needs_review'])

if __name__=='__main__': unittest.main()
