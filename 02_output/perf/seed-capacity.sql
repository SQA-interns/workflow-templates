-- NFR-03 (specification §8): 50 Groups and 200 Users, one Group ("Konferenca 2025") with 10,000 Contacts.
-- Load seed/seed.sql and perf/seed-10k.sql first. Adds 197 Users (200 with the 3 of seed.sql), 48 Groups
-- (50 with the 2 of seed.sql), each owned by one of the new Users with three more Members and 100 Contacts.
-- The new Users have no emulator account; they only fill the tables. Fictitious data, idempotent.
BEGIN;

INSERT INTO app_user (id, firebase_uid, email, display_name, language, created_at)
SELECT md5('capacity-user-' || u)::uuid, 'capacity-user-' || u, 'clan' || u || '@example.si',
       'Član ' || lpad(u::text, 3, '0'), 'sl', now()
FROM generate_series(1, 197) AS u
ON CONFLICT (id) DO NOTHING;

INSERT INTO contact_group (id, name, state, created_at, created_by)
SELECT md5('capacity-group-' || g)::uuid, 'Skupina ' || lpad(g::text, 2, '0'), 'active', now(),
       md5('capacity-user-' || g)::uuid
FROM generate_series(1, 48) AS g
ON CONFLICT (id) DO NOTHING;

-- Group g: Owner User g, Editor User g+48, Viewers User g+96 and g+144 (User 193 … 197 own nothing).
INSERT INTO membership (group_id, user_id, role)
SELECT md5('capacity-group-' || g)::uuid, md5('capacity-user-' || (g + m.offset_by))::uuid, m.role
FROM generate_series(1, 48) AS g,
     (VALUES (0, 'OWNER'), (48, 'EDITOR'), (96, 'VIEWER'), (144, 'VIEWER')) AS m(offset_by, role)
ON CONFLICT DO NOTHING;

INSERT INTO contact (id, group_id, first_name, last_name, email, company, city, country, state, version_number,
                     search_text, sort_name, created_at, created_by, updated_at, updated_by)
SELECT md5('capacity-contact-' || g || '-' || n)::uuid, md5('capacity-group-' || g)::uuid,
       'Oseba', 'Priimek' || lpad(n::text, 3, '0'), 'oseba' || g || '-' || n || '@example.si',
       'Podjetje ' || lpad((n % 20)::text, 2, '0'), 'Ljubljana', 'SI', 'active', 1,
       'oseba priimek' || lpad(n::text, 3, '0') || ' oseba' || g || '-' || n || '@example.si podjetje '
         || lpad((n % 20)::text, 2, '0') || ' ljubljana',
       'Priimek' || lpad(n::text, 3, '0'),
       now(), md5('capacity-user-' || g)::uuid, now(), md5('capacity-user-' || g)::uuid
FROM generate_series(1, 48) AS g, generate_series(1, 100) AS n
ON CONFLICT (id) DO NOTHING;

INSERT INTO contact_version (group_id, contact_id, number, kind, changed_by, changed_at, changes, snapshot)
SELECT c.group_id, c.id, 1, 'created', c.created_by, c.created_at, '[]'::jsonb,
       jsonb_build_object(
         'fields', jsonb_build_object('firstName', c.first_name, 'lastName', c.last_name, 'email', c.email,
                                      'phone', NULL, 'company', c.company, 'jobTitle', NULL, 'street', NULL,
                                      'postalCode', NULL, 'city', c.city, 'country', c.country, 'note', NULL),
         'attributes', '[]'::jsonb, 'tags', '[]'::jsonb)
FROM contact c
JOIN contact_group g ON g.id = c.group_id
WHERE g.name LIKE 'Skupina __'
ON CONFLICT DO NOTHING;

COMMIT;
ANALYZE;
