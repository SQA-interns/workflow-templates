-- NFR-01, NFR-02 (specification §8): 10,000 Contacts in "Konferenca 2025", each with 5 Attributes,
-- 3 Tags and its Version 1. Fictitious data only. Load seed/seed.sql first (scripts/seed-local.sh):
-- it creates the Group and Ana Kovač, who creates the Contacts.
-- Ids are derived from md5 of a fixed text, so loading the file again changes nothing.
BEGIN;

-- 10 Tags; Contact n carries the Tags n, n+3 and n+7 (mod 10).
INSERT INTO tag (id, group_id, name)
SELECT md5('perf-tag-' || t)::uuid, '00000000-0000-4000-8000-0000000b0001', 'perf' || lpad(t::text, 2, '0')
FROM generate_series(0, 9) AS t
ON CONFLICT DO NOTHING;

INSERT INTO contact (id, group_id, first_name, last_name, email, phone, company, job_title, street, postal_code, city,
                     country, note, state, version_number, search_text, sort_name,
                     created_at, created_by, updated_at, updated_by)
SELECT c.id, '00000000-0000-4000-8000-0000000b0001', c.first_name, c.last_name, c.email, NULL, c.company,
       NULL, NULL, NULL, c.city, 'SI', NULL, 'active', 1,
       translate(lower(concat_ws(' ', c.first_name, c.last_name, c.email, c.company, c.city)), 'čšžćđ', 'cszcd'),
       c.last_name,
       now(), '00000000-0000-4000-8000-00000000a001', now(), '00000000-0000-4000-8000-00000000a001'
FROM (
  SELECT md5('perf-contact-' || n)::uuid AS id,
         (ARRAY['Ana', 'Janez', 'Maja', 'Marko', 'Petra', 'Žiga', 'Nina', 'Luka'])[1 + n % 8] AS first_name,
         'Priimek' || lpad(n::text, 5, '0') AS last_name,
         'oseba' || n || '@example.si' AS email,
         'Podjetje ' || lpad((n % 50)::text, 2, '0') AS company,
         (ARRAY['Ljubljana', 'Maribor', 'Celje', 'Kranj', 'Koper'])[1 + n % 5] AS city
  FROM generate_series(1, 10000) AS n
) AS c
ON CONFLICT (id) DO NOTHING;

INSERT INTO attribute (id, group_id, contact_id, key, value)
SELECT md5('perf-attribute-' || n || '-' || k)::uuid, '00000000-0000-4000-8000-0000000b0001',
       md5('perf-contact-' || n)::uuid,
       (ARRAY['kompetenca', 'jezik', 'regija', 'velikost', 'vir'])[k],
       (ARRAY['kompetenca', 'jezik', 'regija', 'velikost', 'vir'])[k] || ((n + k) % 10)
FROM generate_series(1, 10000) AS n, generate_series(1, 5) AS k
ON CONFLICT DO NOTHING;

INSERT INTO contact_tag (group_id, contact_id, tag_id)
SELECT '00000000-0000-4000-8000-0000000b0001', md5('perf-contact-' || n)::uuid, md5('perf-tag-' || (n + s) % 10)::uuid
FROM generate_series(1, 10000) AS n, unnest(ARRAY[0, 3, 7]) AS s
ON CONFLICT DO NOTHING;

-- Version 1 `created` with the snapshot after the change (ADR-07), as seed/seed.sql writes it.
INSERT INTO contact_version (group_id, contact_id, number, kind, changed_by, changed_at, changes, snapshot)
SELECT c.group_id, c.id, 1, 'created', c.created_by, c.created_at, '[]'::jsonb,
       jsonb_build_object(
         'fields', jsonb_build_object('firstName', c.first_name, 'lastName', c.last_name, 'email', c.email,
                                      'phone', c.phone, 'company', c.company, 'jobTitle', c.job_title,
                                      'street', c.street, 'postalCode', c.postal_code, 'city', c.city,
                                      'country', c.country, 'note', c.note),
         'attributes', coalesce((SELECT jsonb_agg(jsonb_build_object('key', a.key, 'value', a.value) ORDER BY a.key)
                                 FROM attribute a WHERE a.contact_id = c.id), '[]'::jsonb),
         'tags', coalesce((SELECT jsonb_agg(t.name ORDER BY t.name)
                           FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id WHERE ct.contact_id = c.id), '[]'::jsonb))
FROM contact c
WHERE c.group_id = '00000000-0000-4000-8000-0000000b0001' AND c.last_name LIKE 'Priimek_____'
ON CONFLICT DO NOTHING;

COMMIT;
ANALYZE contact;
ANALYZE attribute;
ANALYZE contact_tag;
