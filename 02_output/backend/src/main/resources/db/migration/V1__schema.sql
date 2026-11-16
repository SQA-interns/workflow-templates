-- Schema of the Data model (business-rules.md) with D-20, D-23 and ADR-02, ADR-07.
-- Every timestamp is timestamptz in UTC (AR-07). Group-owned tables carry group_id (AR-04).
-- Erasing a Group (BR-08, D-19) cascades to everything it owns.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE app_user (
  id            uuid PRIMARY KEY,
  firebase_uid  varchar(128) NOT NULL UNIQUE,
  email         varchar(254),
  display_name  varchar(100),
  language      varchar(2)   NOT NULL DEFAULT 'sl' CHECK (language IN ('sl', 'en')),
  created_at    timestamptz  NOT NULL,
  deleted_at    timestamptz,
  -- D-20: a deleted User is anonymised (no e-mail, no display name), never removed
  CONSTRAINT app_user_anonymised CHECK (
    (deleted_at IS NULL AND email IS NOT NULL AND display_name IS NOT NULL)
    OR (deleted_at IS NOT NULL AND email IS NULL AND display_name IS NULL))
);
CREATE UNIQUE INDEX app_user_email_uq ON app_user (lower(email)) WHERE email IS NOT NULL;

CREATE TABLE contact_group (
  id          uuid PRIMARY KEY,
  name        varchar(100) NOT NULL,
  state       varchar(20)  NOT NULL CHECK (state IN ('active', 'inRecycleBin')),
  deleted_at  timestamptz,
  created_at  timestamptz  NOT NULL,
  created_by  uuid         NOT NULL REFERENCES app_user (id),
  CONSTRAINT contact_group_deleted CHECK ((state = 'inRecycleBin') = (deleted_at IS NOT NULL))
);
CREATE INDEX contact_group_due ON contact_group (deleted_at) WHERE state = 'inRecycleBin';

CREATE TABLE membership (
  group_id  uuid        NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  user_id   uuid        NOT NULL REFERENCES app_user (id),
  role      varchar(10) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
  PRIMARY KEY (group_id, user_id)
);
CREATE UNIQUE INDEX membership_one_owner ON membership (group_id) WHERE role = 'OWNER';
CREATE INDEX membership_user ON membership (user_id);

CREATE TABLE invitation (
  id           uuid PRIMARY KEY,
  group_id     uuid         NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  email        varchar(254) NOT NULL,
  role         varchar(10)  NOT NULL CHECK (role IN ('EDITOR', 'VIEWER')),
  token_hash   bytea        NOT NULL UNIQUE,
  state        varchar(10)  NOT NULL CHECK (state IN ('open', 'accepted', 'expired', 'revoked')),
  created_at   timestamptz  NOT NULL,
  created_by   uuid         NOT NULL REFERENCES app_user (id),
  expires_at   timestamptz  NOT NULL,
  accepted_at  timestamptz,
  accepted_by  uuid REFERENCES app_user (id)
);
CREATE INDEX invitation_group_open ON invitation (group_id, lower(email)) WHERE state = 'open';

CREATE TABLE contact (
  id              uuid PRIMARY KEY,
  group_id        uuid          NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  first_name      varchar(100),
  last_name       varchar(100),
  email           varchar(254),
  phone           varchar(30),
  company         varchar(200),
  job_title       varchar(100),
  street          varchar(200),
  postal_code     varchar(20),
  city            varchar(100),
  country         char(2),
  note            varchar(2000),
  state           varchar(20)   NOT NULL CHECK (state IN ('active', 'inRecycleBin', 'merged')),
  deleted_at      timestamptz,
  merged_into     uuid REFERENCES contact (id),
  version_number  integer       NOT NULL CHECK (version_number >= 1),
  search_text     text          NOT NULL,
  sort_name       varchar(200)  NOT NULL,
  created_at      timestamptz   NOT NULL,
  created_by      uuid          NOT NULL REFERENCES app_user (id),
  updated_at      timestamptz   NOT NULL,
  updated_by      uuid          NOT NULL REFERENCES app_user (id),
  CONSTRAINT contact_deleted CHECK ((state = 'inRecycleBin') = (deleted_at IS NOT NULL)),
  CONSTRAINT contact_merged CHECK ((state = 'merged') = (merged_into IS NOT NULL))
);
CREATE INDEX contact_group_state ON contact (group_id, state);
CREATE INDEX contact_sort ON contact (group_id, sort_name COLLATE "sl-x-icu", first_name COLLATE "sl-x-icu", company COLLATE "sl-x-icu") WHERE state = 'active';
CREATE INDEX contact_search ON contact USING gin (search_text gin_trgm_ops);
CREATE INDEX contact_email ON contact (group_id, lower(email)) WHERE state = 'active';
CREATE INDEX contact_due ON contact (deleted_at) WHERE state = 'inRecycleBin';
CREATE INDEX contact_merged_into ON contact (merged_into) WHERE merged_into IS NOT NULL;

CREATE TABLE attribute (
  id          uuid PRIMARY KEY,
  group_id    uuid         NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  contact_id  uuid         NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
  key         varchar(50)  NOT NULL,
  value       varchar(500) NOT NULL
);
CREATE UNIQUE INDEX attribute_key_uq ON attribute (contact_id, lower(key));
CREATE INDEX attribute_filter ON attribute (group_id, lower(key), lower(value));

CREATE TABLE tag (
  id        uuid PRIMARY KEY,
  group_id  uuid        NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  name      varchar(50) NOT NULL CHECK (position(',' IN name) = 0)
);
CREATE UNIQUE INDEX tag_name_uq ON tag (group_id, lower(name));

CREATE TABLE contact_tag (
  group_id    uuid NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  contact_id  uuid NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
  tag_id      uuid NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
  PRIMARY KEY (contact_id, tag_id)
);
CREATE INDEX contact_tag_tag ON contact_tag (tag_id);

CREATE TABLE contact_version (
  group_id       uuid        NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  contact_id     uuid        NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
  number         integer     NOT NULL CHECK (number >= 1),
  kind           varchar(30) NOT NULL CHECK (kind IN ('created', 'edited', 'restored', 'movedToRecycleBin',
                                                      'restoredFromRecycleBin', 'merged', 'mergeUndone')),
  changed_by     uuid        NOT NULL REFERENCES app_user (id),
  changed_at     timestamptz NOT NULL,
  restored_from  integer,
  changes        jsonb       NOT NULL,
  snapshot       jsonb       NOT NULL,
  PRIMARY KEY (contact_id, number)
);

CREATE TABLE contact_merge (
  id          uuid PRIMARY KEY,
  group_id    uuid        NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  primary_id  uuid        NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
  merged_id   uuid        NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
  merged_by   uuid        NOT NULL REFERENCES app_user (id),
  merged_at   timestamptz NOT NULL,
  undone_at   timestamptz,
  undone_by   uuid REFERENCES app_user (id),
  CONSTRAINT contact_merge_distinct CHECK (primary_id <> merged_id)
);
CREATE INDEX contact_merge_primary ON contact_merge (primary_id);

-- Audit entries hold no personal data except the acting User id (SR-04, SR-06, D-19).
CREATE TABLE audit_entry (
  id         uuid PRIMARY KEY,
  group_id   uuid        NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
  user_id    uuid        NOT NULL REFERENCES app_user (id),
  action     varchar(50) NOT NULL,
  target_id  uuid        NOT NULL,
  at         timestamptz NOT NULL
);
CREATE INDEX audit_entry_group ON audit_entry (group_id, at);
