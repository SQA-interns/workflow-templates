-- Local seed data (no real personal data): three Users, two Groups, three Contacts with Version 1.
-- Loaded by scripts/seed-local.sh, which passes the emulator uids as :ana_uid, :marko_uid, :petra_uid.
-- Idempotent: running it again changes nothing except the Firebase uids.
BEGIN;

INSERT INTO app_user (id, firebase_uid, email, display_name, language, created_at) VALUES
  ('00000000-0000-4000-8000-00000000a001', :'ana_uid',   'ana.kovac@example.si',    'Ana Kovač',    'sl', now()),
  ('00000000-0000-4000-8000-00000000a002', :'marko_uid', 'marko.kranjc@example.si', 'Marko Kranjc', 'sl', now()),
  ('00000000-0000-4000-8000-00000000a003', :'petra_uid', 'petra.horvat@example.si', 'Petra Horvat', 'en', now())
ON CONFLICT (id) DO UPDATE SET firebase_uid = EXCLUDED.firebase_uid;

INSERT INTO contact_group (id, name, state, created_at, created_by) VALUES
  ('00000000-0000-4000-8000-0000000b0001', 'Konferenca 2025', 'active', now(), '00000000-0000-4000-8000-00000000a001'),
  ('00000000-0000-4000-8000-0000000b0002', 'Podjetje Beta',   'active', now(), '00000000-0000-4000-8000-00000000a003')
ON CONFLICT (id) DO NOTHING;

INSERT INTO membership (group_id, user_id, role) VALUES
  ('00000000-0000-4000-8000-0000000b0001', '00000000-0000-4000-8000-00000000a001', 'OWNER'),
  ('00000000-0000-4000-8000-0000000b0001', '00000000-0000-4000-8000-00000000a002', 'EDITOR'),
  ('00000000-0000-4000-8000-0000000b0001', '00000000-0000-4000-8000-00000000a003', 'VIEWER'),
  ('00000000-0000-4000-8000-0000000b0002', '00000000-0000-4000-8000-00000000a003', 'OWNER')
ON CONFLICT DO NOTHING;

-- Contacts of "Konferenca 2025"; search_text and sort_name as ADR-02 defines them.
INSERT INTO contact (id, group_id, first_name, last_name, email, phone, company, job_title, street, postal_code, city,
                     country, note, state, version_number, search_text, sort_name,
                     created_at, created_by, updated_at, updated_by)
SELECT c.id::uuid, '00000000-0000-4000-8000-0000000b0001', c.first_name, c.last_name, c.email, c.phone, c.company,
       c.job_title, c.street, c.postal_code, c.city, c.country, c.note, 'active', 1,
       translate(lower(concat_ws(' ', c.first_name, c.last_name, c.email, c.phone, c.company, c.city)),
                 'čšžćđ', 'cszcd'),
       coalesce(nullif(c.last_name, ''), nullif(c.first_name, ''), c.company),
       now(), '00000000-0000-4000-8000-00000000a001', now(), '00000000-0000-4000-8000-00000000a001'
FROM (VALUES
  ('00000000-0000-4000-8000-0000000c0001', 'Janez', 'Novak', 'janez.novak@example.si', '+386 41 123 456',
   'Podjetje d.o.o.', 'Direktor', 'Slovenska cesta 1', '1000', 'Ljubljana', 'SI', 'Govornik'),
  ('00000000-0000-4000-8000-0000000c0002', 'Maja', 'Zupan', 'maja.zupan@example.si', NULL,
   NULL, NULL, NULL, NULL, 'Gradec', 'AT', NULL),
  ('00000000-0000-4000-8000-0000000c0003', 'Žiga', 'Čuk', NULL, NULL,
   'Alfa d.o.o.', NULL, NULL, NULL, NULL, NULL, NULL)
) AS c(id, first_name, last_name, email, phone, company, job_title, street, postal_code, city, country, note)
ON CONFLICT (id) DO NOTHING;

INSERT INTO attribute (id, group_id, contact_id, key, value) VALUES
  ('00000000-0000-4000-8000-0000000d0001', '00000000-0000-4000-8000-0000000b0001',
   '00000000-0000-4000-8000-0000000c0001', 'kompetenca', 'spletnoProgramiranje')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tag (id, group_id, name) VALUES
  ('00000000-0000-4000-8000-0000000e0001', '00000000-0000-4000-8000-0000000b0001', 'udelezenec2025'),
  ('00000000-0000-4000-8000-0000000e0002', '00000000-0000-4000-8000-0000000b0001', 'neObvescaj')
ON CONFLICT (id) DO NOTHING;

INSERT INTO contact_tag (group_id, contact_id, tag_id) VALUES
  ('00000000-0000-4000-8000-0000000b0001', '00000000-0000-4000-8000-0000000c0001', '00000000-0000-4000-8000-0000000e0001'),
  ('00000000-0000-4000-8000-0000000b0001', '00000000-0000-4000-8000-0000000c0002', '00000000-0000-4000-8000-0000000e0001'),
  ('00000000-0000-4000-8000-0000000b0001', '00000000-0000-4000-8000-0000000c0002', '00000000-0000-4000-8000-0000000e0002')
ON CONFLICT DO NOTHING;

-- Version 1 `created` of each Contact (BR-05) with the snapshot after the change (ADR-07).
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
WHERE c.group_id = '00000000-0000-4000-8000-0000000b0001'
ON CONFLICT DO NOTHING;

COMMIT;
