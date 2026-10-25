-- Opt-in Flyway location, separate history table. Existing service migrations remain untouched.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE cocktail_search_document (
    cocktail_id bigint PRIMARY KEY REFERENCES cocktail(id),
    document jsonb NOT NULL,
    base_normalized text[] NOT NULL,
    required_ingredients text[] NOT NULL,
    all_ingredients text[] NOT NULL,
    abv_min integer NOT NULL,
    abv_max integer NOT NULL,
    season text NOT NULL,
    profile_text text NOT NULL,
    profile_embedding vector NOT NULL,
    embedding_model text NOT NULL,
    embedding_version text NOT NULL,
    embedding_dimension integer NOT NULL CHECK (embedding_dimension > 0),
    content_hash text NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (vector_dims(profile_embedding) = embedding_dimension),
    CHECK (0 <= abv_min AND abv_min <= abv_max AND abv_max <= 100)
);
CREATE TABLE cocktail_search_term (
    cocktail_id bigint NOT NULL REFERENCES cocktail_search_document(cocktail_id) ON DELETE CASCADE,
    term_type text NOT NULL CHECK (term_type IN ('name','alias','ingredient','base','tag')),
    term_original text NOT NULL,
    term_normalized text NOT NULL,
    is_required boolean NOT NULL DEFAULT false,
    source text NOT NULL,
    PRIMARY KEY (cocktail_id, term_type, term_normalized)
);
CREATE INDEX cocktail_search_term_exact_idx ON cocktail_search_term(term_normalized, term_type);
CREATE INDEX cocktail_search_term_trgm_idx ON cocktail_search_term USING gin(term_normalized gin_trgm_ops);
CREATE INDEX cocktail_search_base_idx ON cocktail_search_document USING gin(base_normalized);
CREATE INDEX cocktail_search_required_idx ON cocktail_search_document USING gin(required_ingredients);
-- Exact cosine scan for 105 documents. No fixed vector dimension or premature ANN index.
