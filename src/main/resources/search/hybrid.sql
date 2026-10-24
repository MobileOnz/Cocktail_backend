-- Adapted from platform-p2 hybrid_search: independent ranks, UNION ALL, reciprocal-rank sum.
-- ponytail: exact scoring scans 105 documents; use measured indexed candidate retrieval when this grows.
-- Both channels share this MATERIALIZED filter set (avoids the old OR/AND precedence bug).
WITH filtered AS MATERIALIZED (
 SELECT d.*, c.image_url AS current_image_url FROM cocktail_search_document d JOIN cocktail c ON c.id=d.cocktail_id
 WHERE d.base_normalized @> ARRAY(SELECT jsonb_array_elements_text(CAST(:bases AS jsonb)))
 AND NOT d.base_normalized && ARRAY(SELECT jsonb_array_elements_text(CAST(:excludedBases AS jsonb)))
 AND d.required_ingredients @> ARRAY(SELECT jsonb_array_elements_text(CAST(:required AS jsonb)))
 AND NOT d.all_ingredients && ARRAY(SELECT jsonb_array_elements_text(CAST(:excluded AS jsonb)))
 AND (CAST(:abvMin AS integer) IS NULL OR d.abv_min >= :abvMin)
 AND (CAST(:abvMax AS integer) IS NULL OR d.abv_max <= :abvMax)
 AND (CAST(:season AS text) IS NULL OR d.season = :season)
), scored AS (
 SELECT d.cocktail_id,
 coalesce(max(CASE
   WHEN t.term_type='name' AND t.term_original=:raw THEN 4
   WHEN t.term_type='name' AND t.term_normalized=:query THEN 3
   WHEN t.term_type='alias' AND t.term_normalized=:query THEN 2 ELSE 0 END),0) AS name_priority,
 coalesce(max(CASE
   WHEN t.term_type='name' AND t.term_original=:raw THEN 10000
   WHEN t.term_type='name' AND t.term_normalized=:query THEN 9000
   WHEN t.term_type='alias' AND t.term_normalized=:query THEN 8000
   WHEN t.term_type IN ('name','alias') AND starts_with(t.term_normalized,:query) THEN 6000
   WHEN :nameSearch AND t.term_type IN ('name','alias') AND length(:query)>=2 AND similarity(t.term_normalized,:query)>=0.3
     THEN 1000*similarity(t.term_normalized,:query) ELSE 0 END),0)
 + count(DISTINCT t.term_normalized) FILTER (WHERE t.term_normalized IN (SELECT jsonb_array_elements_text(CAST(:terms AS jsonb))))*100
 + CASE WHEN :filterOnly THEN 1 ELSE 0 END AS keyword_score
 FROM filtered d LEFT JOIN cocktail_search_term t USING(cocktail_id) GROUP BY d.cocktail_id
), keyword_candidates AS (
 SELECT cocktail_id, row_number() OVER (ORDER BY name_priority DESC,keyword_score DESC,cocktail_id) AS rank
 FROM scored WHERE keyword_score>0 AND :useKeyword
 ORDER BY name_priority DESC,keyword_score DESC,cocktail_id LIMIT :candidates
), distances AS MATERIALIZED (
 SELECT cocktail_id, CASE WHEN embedding_model=:model AND embedding_version=:version
     AND embedding_dimension=:dimension AND CAST(:embedding AS vector) IS NOT NULL
     THEN profile_embedding <=> CAST(:embedding AS vector) END AS distance
 FROM filtered
), vector_candidates AS (
 SELECT cocktail_id,row_number() OVER (ORDER BY distance,cocktail_id) AS rank
 FROM distances WHERE distance IS NOT NULL AND :useVector
 ORDER BY distance,cocktail_id LIMIT :candidates
), fused AS (
 SELECT cocktail_id,
 max(rank) FILTER (WHERE method='keyword') AS keyword_rank,
 max(rank) FILTER (WHERE method='vector') AS vector_rank,
 sum(weight / (:rrfK + rank)) AS rrf_score
 FROM (
  SELECT cocktail_id,rank,'keyword' AS method,CAST(:keywordWeight AS double precision) AS weight FROM keyword_candidates
  UNION ALL
  SELECT cocktail_id,rank,'vector',CAST(:vectorWeight AS double precision) FROM vector_candidates
 ) candidates GROUP BY cocktail_id
)
SELECT d.document::text, d.current_image_url, f.*, s.name_priority
FROM fused f JOIN filtered d USING(cocktail_id) JOIN scored s USING(cocktail_id)
ORDER BY CASE WHEN :useKeyword THEN s.name_priority ELSE 0 END DESC, f.rrf_score DESC, f.cocktail_id
