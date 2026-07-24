-- Converts every bigint/bigserial primary and foreign key in the schema to a UUID, so that no
-- record's id (and therefore no count of users/events/reservations/etc.) can be inferred by
-- guessing/incrementing a path parameter.
--
-- Existing rows keep their relationships: every table gets a random UUID for its id, and every
-- foreign key column is repointed to the new UUID of the row it used to reference by bigint id
-- (child.fk_old = parent.id_old). Freshly generated ids from the application afterwards are UUID
-- v7 (time-ordered), but that only matters for new rows created going forward -- the migrated
-- historical rows keep whatever random UUID they were assigned here, and ordering by id no longer
-- carries any signal for them (equivalent to Version 4). This runs once, in a single transaction:
-- either the whole database ends up converted, or (on failure) none of it does.

-- ============================================================================
-- Phase 1: give every table that owns an "id" column a new UUID id, keeping the old bigint id
-- around (renamed to id_old) so Phase 2 can still join child foreign keys against it.
-- Dropping each table's primary key CASCADEs into every foreign key constraint elsewhere in the
-- schema that references it -- all of those are recreated from scratch in Phase 4.
-- ============================================================================

ALTER TABLE users DROP CONSTRAINT users_pkey CASCADE;
ALTER TABLE users RENAME COLUMN id TO id_old;
ALTER TABLE users ADD COLUMN id uuid;
UPDATE users SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE users ALTER COLUMN id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE eventlocations DROP CONSTRAINT eventlocations_pkey CASCADE;
ALTER TABLE eventlocations RENAME COLUMN id TO id_old;
ALTER TABLE eventlocations ADD COLUMN id uuid;
UPDATE eventlocations SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE eventlocations ALTER COLUMN id SET NOT NULL;
ALTER TABLE eventlocations ADD CONSTRAINT eventlocations_pkey PRIMARY KEY (id);

ALTER TABLE events DROP CONSTRAINT events_pkey CASCADE;
ALTER TABLE events RENAME COLUMN id TO id_old;
ALTER TABLE events ADD COLUMN id uuid;
UPDATE events SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE events ALTER COLUMN id SET NOT NULL;
ALTER TABLE events ADD CONSTRAINT events_pkey PRIMARY KEY (id);

ALTER TABLE event_location_areas DROP CONSTRAINT event_location_areas_pkey CASCADE;
ALTER TABLE event_location_areas RENAME COLUMN id TO id_old;
ALTER TABLE event_location_areas ADD COLUMN id uuid;
UPDATE event_location_areas SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE event_location_areas ALTER COLUMN id SET NOT NULL;
ALTER TABLE event_location_areas ADD CONSTRAINT event_location_areas_pkey PRIMARY KEY (id);

ALTER TABLE event_location_entrances DROP CONSTRAINT event_location_entrances_pkey CASCADE;
ALTER TABLE event_location_entrances RENAME COLUMN id TO id_old;
ALTER TABLE event_location_entrances ADD COLUMN id uuid;
UPDATE event_location_entrances SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE event_location_entrances ALTER COLUMN id SET NOT NULL;
ALTER TABLE event_location_entrances ADD CONSTRAINT event_location_entrances_pkey PRIMARY KEY (id);

ALTER TABLE event_location_markers DROP CONSTRAINT event_location_markers_pkey CASCADE;
ALTER TABLE event_location_markers RENAME COLUMN id TO id_old;
ALTER TABLE event_location_markers ADD COLUMN id uuid;
UPDATE event_location_markers SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE event_location_markers ALTER COLUMN id SET NOT NULL;
ALTER TABLE event_location_markers ADD CONSTRAINT event_location_markers_pkey PRIMARY KEY (id);

ALTER TABLE seats DROP CONSTRAINT seats_pkey CASCADE;
ALTER TABLE seats RENAME COLUMN id TO id_old;
ALTER TABLE seats ADD COLUMN id uuid;
UPDATE seats SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE seats ALTER COLUMN id SET NOT NULL;
ALTER TABLE seats ADD CONSTRAINT seats_pkey PRIMARY KEY (id);

ALTER TABLE reservations DROP CONSTRAINT reservations_pkey CASCADE;
ALTER TABLE reservations RENAME COLUMN id TO id_old;
ALTER TABLE reservations ADD COLUMN id uuid;
UPDATE reservations SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE reservations ALTER COLUMN id SET NOT NULL;
ALTER TABLE reservations ADD CONSTRAINT reservations_pkey PRIMARY KEY (id);

ALTER TABLE eventuserallowance DROP CONSTRAINT eventuserallowance_pkey CASCADE;
ALTER TABLE eventuserallowance RENAME COLUMN id TO id_old;
ALTER TABLE eventuserallowance ADD COLUMN id uuid;
UPDATE eventuserallowance SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE eventuserallowance ALTER COLUMN id SET NOT NULL;
ALTER TABLE eventuserallowance ADD CONSTRAINT eventuserallowance_pkey PRIMARY KEY (id);

ALTER TABLE refresh_tokens DROP CONSTRAINT refresh_tokens_pkey CASCADE;
ALTER TABLE refresh_tokens RENAME COLUMN id TO id_old;
ALTER TABLE refresh_tokens ADD COLUMN id uuid;
UPDATE refresh_tokens SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE refresh_tokens ALTER COLUMN id SET NOT NULL;
ALTER TABLE refresh_tokens ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE webauthn_credentials DROP CONSTRAINT webauthn_credentials_pkey CASCADE;
ALTER TABLE webauthn_credentials RENAME COLUMN id TO id_old;
ALTER TABLE webauthn_credentials ADD COLUMN id uuid;
UPDATE webauthn_credentials SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE webauthn_credentials ALTER COLUMN id SET NOT NULL;
ALTER TABLE webauthn_credentials ADD CONSTRAINT webauthn_credentials_pkey PRIMARY KEY (id);

ALTER TABLE email_verification DROP CONSTRAINT email_verification_pkey CASCADE;
ALTER TABLE email_verification RENAME COLUMN id TO id_old;
ALTER TABLE email_verification ADD COLUMN id uuid;
UPDATE email_verification SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE email_verification ALTER COLUMN id SET NOT NULL;
ALTER TABLE email_verification ADD CONSTRAINT email_verification_pkey PRIMARY KEY (id);

ALTER TABLE email_seat_map_tokens DROP CONSTRAINT email_seat_map_tokens_pkey CASCADE;
ALTER TABLE email_seat_map_tokens RENAME COLUMN id TO id_old;
ALTER TABLE email_seat_map_tokens ADD COLUMN id uuid;
UPDATE email_seat_map_tokens SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE email_seat_map_tokens ALTER COLUMN id SET NOT NULL;
ALTER TABLE email_seat_map_tokens ADD CONSTRAINT email_seat_map_tokens_pkey PRIMARY KEY (id);

ALTER TABLE outbound_emails DROP CONSTRAINT outbound_emails_pkey CASCADE;
ALTER TABLE outbound_emails RENAME COLUMN id TO id_old;
ALTER TABLE outbound_emails ADD COLUMN id uuid;
UPDATE outbound_emails SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE outbound_emails ALTER COLUMN id SET NOT NULL;
ALTER TABLE outbound_emails ADD CONSTRAINT outbound_emails_pkey PRIMARY KEY (id);

ALTER TABLE outbound_email_attachments DROP CONSTRAINT outbound_email_attachments_pkey CASCADE;
ALTER TABLE outbound_email_attachments RENAME COLUMN id TO id_old;
ALTER TABLE outbound_email_attachments ADD COLUMN id uuid;
UPDATE outbound_email_attachments SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE outbound_email_attachments ALTER COLUMN id SET NOT NULL;
ALTER TABLE outbound_email_attachments ADD CONSTRAINT outbound_email_attachments_pkey PRIMARY KEY (id);

ALTER TABLE login_attempts DROP CONSTRAINT login_attempts_pkey CASCADE;
ALTER TABLE login_attempts RENAME COLUMN id TO id_old;
ALTER TABLE login_attempts ADD COLUMN id uuid;
UPDATE login_attempts SET id = gen_random_uuid();  -- NOSONAR: intentional full-table backfill, every row gets a fresh id by design
ALTER TABLE login_attempts ALTER COLUMN id SET NOT NULL;
ALTER TABLE login_attempts ADD CONSTRAINT login_attempts_pkey PRIMARY KEY (id);

-- ============================================================================
-- Phase 2: repoint every foreign key column at its parent's new UUID id, via the still-present
-- id_old columns from Phase 1. Nullable foreign keys stay NULL where they already were (the join
-- simply leaves those rows unmatched).
-- ============================================================================

ALTER TABLE eventlocations ADD COLUMN manager_id_new uuid;
UPDATE eventlocations c SET manager_id_new = p.id FROM users p WHERE c.manager_id = p.id_old;
ALTER TABLE eventlocations RENAME COLUMN manager_id TO manager_id_old;
ALTER TABLE eventlocations RENAME COLUMN manager_id_new TO manager_id;

ALTER TABLE events ADD COLUMN event_location_id_new uuid;
UPDATE events c SET event_location_id_new = p.id FROM eventlocations p WHERE c.event_location_id = p.id_old;
ALTER TABLE events RENAME COLUMN event_location_id TO event_location_id_old;
ALTER TABLE events RENAME COLUMN event_location_id_new TO event_location_id;

ALTER TABLE events ADD COLUMN manager_id_new uuid;
UPDATE events c SET manager_id_new = p.id FROM users p WHERE c.manager_id = p.id_old;
ALTER TABLE events RENAME COLUMN manager_id TO manager_id_old;
ALTER TABLE events RENAME COLUMN manager_id_new TO manager_id;

ALTER TABLE event_location_areas ADD COLUMN event_location_id_new uuid;
UPDATE event_location_areas c SET event_location_id_new = p.id FROM eventlocations p WHERE c.event_location_id = p.id_old;
ALTER TABLE event_location_areas RENAME COLUMN event_location_id TO event_location_id_old;
ALTER TABLE event_location_areas RENAME COLUMN event_location_id_new TO event_location_id;

ALTER TABLE event_location_entrances ADD COLUMN event_location_id_new uuid;
UPDATE event_location_entrances c SET event_location_id_new = p.id FROM eventlocations p WHERE c.event_location_id = p.id_old;
ALTER TABLE event_location_entrances RENAME COLUMN event_location_id TO event_location_id_old;
ALTER TABLE event_location_entrances RENAME COLUMN event_location_id_new TO event_location_id;

ALTER TABLE event_location_markers ADD COLUMN event_location_id_new uuid;
UPDATE event_location_markers c SET event_location_id_new = p.id FROM eventlocations p WHERE c.event_location_id = p.id_old;
ALTER TABLE event_location_markers RENAME COLUMN event_location_id TO event_location_id_old;
ALTER TABLE event_location_markers RENAME COLUMN event_location_id_new TO event_location_id;

ALTER TABLE event_location_area_boundary ADD COLUMN area_id_new uuid;
UPDATE event_location_area_boundary c SET area_id_new = p.id FROM event_location_areas p WHERE c.area_id = p.id_old;
ALTER TABLE event_location_area_boundary RENAME COLUMN area_id TO area_id_old;
ALTER TABLE event_location_area_boundary RENAME COLUMN area_id_new TO area_id;
ALTER TABLE event_location_area_boundary ALTER COLUMN area_id SET NOT NULL;

ALTER TABLE seats ADD COLUMN location_id_new uuid;
UPDATE seats c SET location_id_new = p.id FROM eventlocations p WHERE c.location_id = p.id_old;
ALTER TABLE seats RENAME COLUMN location_id TO location_id_old;
ALTER TABLE seats RENAME COLUMN location_id_new TO location_id;

ALTER TABLE seats ADD COLUMN entrance_id_new uuid;
UPDATE seats c SET entrance_id_new = p.id FROM event_location_entrances p WHERE c.entrance_id = p.id_old;
ALTER TABLE seats RENAME COLUMN entrance_id TO entrance_id_old;
ALTER TABLE seats RENAME COLUMN entrance_id_new TO entrance_id;

ALTER TABLE seats ADD COLUMN area_id_new uuid;
UPDATE seats c SET area_id_new = p.id FROM event_location_areas p WHERE c.area_id = p.id_old;
ALTER TABLE seats RENAME COLUMN area_id TO area_id_old;
ALTER TABLE seats RENAME COLUMN area_id_new TO area_id;

ALTER TABLE reservations ADD COLUMN event_id_new uuid;
UPDATE reservations c SET event_id_new = p.id FROM events p WHERE c.event_id = p.id_old;
ALTER TABLE reservations RENAME COLUMN event_id TO event_id_old;
ALTER TABLE reservations RENAME COLUMN event_id_new TO event_id;

ALTER TABLE reservations ADD COLUMN seat_id_new uuid;
UPDATE reservations c SET seat_id_new = p.id FROM seats p WHERE c.seat_id = p.id_old;
ALTER TABLE reservations RENAME COLUMN seat_id TO seat_id_old;
ALTER TABLE reservations RENAME COLUMN seat_id_new TO seat_id;

ALTER TABLE reservations ADD COLUMN user_id_new uuid;
UPDATE reservations c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE reservations RENAME COLUMN user_id TO user_id_old;
ALTER TABLE reservations RENAME COLUMN user_id_new TO user_id;

ALTER TABLE eventuserallowance ADD COLUMN event_id_new uuid;
UPDATE eventuserallowance c SET event_id_new = p.id FROM events p WHERE c.event_id = p.id_old;
ALTER TABLE eventuserallowance RENAME COLUMN event_id TO event_id_old;
ALTER TABLE eventuserallowance RENAME COLUMN event_id_new TO event_id;

ALTER TABLE eventuserallowance ADD COLUMN user_id_new uuid;
UPDATE eventuserallowance c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE eventuserallowance RENAME COLUMN user_id TO user_id_old;
ALTER TABLE eventuserallowance RENAME COLUMN user_id_new TO user_id;

ALTER TABLE refresh_tokens ADD COLUMN user_id_new uuid;
UPDATE refresh_tokens c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE refresh_tokens RENAME COLUMN user_id TO user_id_old;
ALTER TABLE refresh_tokens RENAME COLUMN user_id_new TO user_id;
ALTER TABLE refresh_tokens ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE webauthn_credentials ADD COLUMN user_id_new uuid;
UPDATE webauthn_credentials c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE webauthn_credentials RENAME COLUMN user_id TO user_id_old;
ALTER TABLE webauthn_credentials RENAME COLUMN user_id_new TO user_id;
ALTER TABLE webauthn_credentials ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE email_verification ADD COLUMN user_id_new uuid;
UPDATE email_verification c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE email_verification RENAME COLUMN user_id TO user_id_old;
ALTER TABLE email_verification RENAME COLUMN user_id_new TO user_id;

ALTER TABLE email_seat_map_tokens ADD COLUMN event_id_new uuid;
UPDATE email_seat_map_tokens c SET event_id_new = p.id FROM events p WHERE c.event_id = p.id_old;
ALTER TABLE email_seat_map_tokens RENAME COLUMN event_id TO event_id_old;
ALTER TABLE email_seat_map_tokens RENAME COLUMN event_id_new TO event_id;
ALTER TABLE email_seat_map_tokens ALTER COLUMN event_id SET NOT NULL;

ALTER TABLE email_seat_map_tokens ADD COLUMN user_id_new uuid;
UPDATE email_seat_map_tokens c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE email_seat_map_tokens RENAME COLUMN user_id TO user_id_old;
ALTER TABLE email_seat_map_tokens RENAME COLUMN user_id_new TO user_id;
ALTER TABLE email_seat_map_tokens ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE email_seat_map_token_seats ADD COLUMN token_id_new uuid;
UPDATE email_seat_map_token_seats c SET token_id_new = p.id FROM email_seat_map_tokens p WHERE c.token_id = p.id_old;
ALTER TABLE email_seat_map_token_seats RENAME COLUMN token_id TO token_id_old;
ALTER TABLE email_seat_map_token_seats RENAME COLUMN token_id_new TO token_id;
ALTER TABLE email_seat_map_token_seats ALTER COLUMN token_id SET NOT NULL;

ALTER TABLE outbound_email_attachments ADD COLUMN email_id_new uuid;
UPDATE outbound_email_attachments c SET email_id_new = p.id FROM outbound_emails p WHERE c.email_id = p.id_old;
ALTER TABLE outbound_email_attachments RENAME COLUMN email_id TO email_id_old;
ALTER TABLE outbound_email_attachments RENAME COLUMN email_id_new TO email_id;
ALTER TABLE outbound_email_attachments ALTER COLUMN email_id SET NOT NULL;

ALTER TABLE outbound_email_recipients ADD COLUMN email_id_new uuid;
UPDATE outbound_email_recipients c SET email_id_new = p.id FROM outbound_emails p WHERE c.email_id = p.id_old;
ALTER TABLE outbound_email_recipients RENAME COLUMN email_id TO email_id_old;
ALTER TABLE outbound_email_recipients RENAME COLUMN email_id_new TO email_id;
ALTER TABLE outbound_email_recipients ALTER COLUMN email_id SET NOT NULL;

ALTER TABLE outbound_email_cc ADD COLUMN email_id_new uuid;
UPDATE outbound_email_cc c SET email_id_new = p.id FROM outbound_emails p WHERE c.email_id = p.id_old;
ALTER TABLE outbound_email_cc RENAME COLUMN email_id TO email_id_old;
ALTER TABLE outbound_email_cc RENAME COLUMN email_id_new TO email_id;
ALTER TABLE outbound_email_cc ALTER COLUMN email_id SET NOT NULL;

ALTER TABLE outbound_email_bcc ADD COLUMN email_id_new uuid;
UPDATE outbound_email_bcc c SET email_id_new = p.id FROM outbound_emails p WHERE c.email_id = p.id_old;
ALTER TABLE outbound_email_bcc RENAME COLUMN email_id TO email_id_old;
ALTER TABLE outbound_email_bcc RENAME COLUMN email_id_new TO email_id;
ALTER TABLE outbound_email_bcc ALTER COLUMN email_id SET NOT NULL;

ALTER TABLE login_attempts ADD COLUMN user_id_new uuid;
UPDATE login_attempts c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE login_attempts RENAME COLUMN user_id TO user_id_old;
ALTER TABLE login_attempts RENAME COLUMN user_id_new TO user_id;

ALTER TABLE user_tags ADD COLUMN user_id_new uuid;
UPDATE user_tags c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE user_tags RENAME COLUMN user_id TO user_id_old;
ALTER TABLE user_tags RENAME COLUMN user_id_new TO user_id;
ALTER TABLE user_tags ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE user_roles ADD COLUMN user_id_new uuid;
UPDATE user_roles c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE user_roles RENAME COLUMN user_id TO user_id_old;
ALTER TABLE user_roles RENAME COLUMN user_id_new TO user_id;
ALTER TABLE user_roles ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE event_supervisors ADD COLUMN event_id_new uuid;
UPDATE event_supervisors c SET event_id_new = p.id FROM events p WHERE c.event_id = p.id_old;
ALTER TABLE event_supervisors RENAME COLUMN event_id TO event_id_old;
ALTER TABLE event_supervisors RENAME COLUMN event_id_new TO event_id;
ALTER TABLE event_supervisors ALTER COLUMN event_id SET NOT NULL;

ALTER TABLE event_supervisors ADD COLUMN user_id_new uuid;
UPDATE event_supervisors c SET user_id_new = p.id FROM users p WHERE c.user_id = p.id_old;
ALTER TABLE event_supervisors RENAME COLUMN user_id TO user_id_old;
ALTER TABLE event_supervisors RENAME COLUMN user_id_new TO user_id;
ALTER TABLE event_supervisors ALTER COLUMN user_id SET NOT NULL;

-- ============================================================================
-- Phase 3: drop every renamed bigint column (CASCADE takes any constraint/index still attached
-- to it with it -- namely the composite PKs/unique constraints on the join tables, and the
-- idx_user_event index) plus the now-unused per-table sequences.
-- ============================================================================

ALTER TABLE users DROP COLUMN id_old CASCADE;
ALTER TABLE eventlocations DROP COLUMN id_old CASCADE, DROP COLUMN manager_id_old CASCADE;
ALTER TABLE events DROP COLUMN id_old CASCADE, DROP COLUMN event_location_id_old CASCADE, DROP COLUMN manager_id_old CASCADE;
ALTER TABLE event_location_areas DROP COLUMN id_old CASCADE, DROP COLUMN event_location_id_old CASCADE;
ALTER TABLE event_location_entrances DROP COLUMN id_old CASCADE, DROP COLUMN event_location_id_old CASCADE;
ALTER TABLE event_location_markers DROP COLUMN id_old CASCADE, DROP COLUMN event_location_id_old CASCADE;
ALTER TABLE event_location_area_boundary DROP COLUMN area_id_old CASCADE;
ALTER TABLE seats DROP COLUMN id_old CASCADE, DROP COLUMN location_id_old CASCADE, DROP COLUMN entrance_id_old CASCADE, DROP COLUMN area_id_old CASCADE;
ALTER TABLE reservations DROP COLUMN id_old CASCADE, DROP COLUMN event_id_old CASCADE, DROP COLUMN seat_id_old CASCADE, DROP COLUMN user_id_old CASCADE;
ALTER TABLE eventuserallowance DROP COLUMN id_old CASCADE, DROP COLUMN event_id_old CASCADE, DROP COLUMN user_id_old CASCADE;
ALTER TABLE refresh_tokens DROP COLUMN id_old CASCADE, DROP COLUMN user_id_old CASCADE;
ALTER TABLE webauthn_credentials DROP COLUMN id_old CASCADE, DROP COLUMN user_id_old CASCADE;
ALTER TABLE email_verification DROP COLUMN id_old CASCADE, DROP COLUMN user_id_old CASCADE;
ALTER TABLE email_seat_map_tokens DROP COLUMN id_old CASCADE, DROP COLUMN event_id_old CASCADE, DROP COLUMN user_id_old CASCADE;
ALTER TABLE email_seat_map_token_seats DROP COLUMN token_id_old CASCADE;
ALTER TABLE outbound_emails DROP COLUMN id_old CASCADE;
ALTER TABLE outbound_email_attachments DROP COLUMN id_old CASCADE, DROP COLUMN email_id_old CASCADE;
ALTER TABLE outbound_email_recipients DROP COLUMN email_id_old CASCADE;
ALTER TABLE outbound_email_cc DROP COLUMN email_id_old CASCADE;
ALTER TABLE outbound_email_bcc DROP COLUMN email_id_old CASCADE;
ALTER TABLE login_attempts DROP COLUMN id_old CASCADE, DROP COLUMN user_id_old CASCADE;
ALTER TABLE user_tags DROP COLUMN user_id_old CASCADE;
ALTER TABLE user_roles DROP COLUMN user_id_old CASCADE;
ALTER TABLE event_supervisors DROP COLUMN event_id_old CASCADE, DROP COLUMN user_id_old CASCADE;

DROP SEQUENCE users_seq;
DROP SEQUENCE eventlocations_seq;
DROP SEQUENCE events_seq;
DROP SEQUENCE event_location_areas_seq;
DROP SEQUENCE event_location_entrances_seq;
DROP SEQUENCE event_location_markers_seq;
DROP SEQUENCE seats_seq;
DROP SEQUENCE reservations_seq;
DROP SEQUENCE eventuserallowance_seq;
DROP SEQUENCE refresh_tokens_seq;
DROP SEQUENCE webauthn_credentials_seq;
DROP SEQUENCE email_verification_seq;
DROP SEQUENCE email_seat_map_tokens_seq;
DROP SEQUENCE outbound_emails_seq;
DROP SEQUENCE outbound_email_attachments_seq;
DROP SEQUENCE login_attempts_seq;

-- ============================================================================
-- Phase 4: recreate every constraint/index that Phase 3's CASCADEs removed, on the new UUID
-- columns, using the exact same names Hibernate itself uses -- so this schema is byte-identical
-- to what `quarkus.hibernate-orm.schema-management.strategy=validate` expects.
-- ============================================================================

ALTER TABLE ONLY email_seat_map_token_seats ADD CONSTRAINT email_seat_map_token_seats_token_id_seat_number_key UNIQUE (token_id, seat_number);
ALTER TABLE ONLY email_verification ADD CONSTRAINT email_verification_user_id_key UNIQUE (user_id);
ALTER TABLE ONLY event_location_area_boundary ADD CONSTRAINT event_location_area_boundary_pkey PRIMARY KEY (sort_order, area_id);
ALTER TABLE ONLY event_supervisors ADD CONSTRAINT event_supervisors_pkey PRIMARY KEY (event_id, user_id);
ALTER TABLE ONLY reservations ADD CONSTRAINT reservations_event_id_seat_id_key UNIQUE (event_id, seat_id);
ALTER TABLE ONLY reservations ADD CONSTRAINT reservations_event_id_user_id_checkincode_key UNIQUE (event_id, user_id, checkincode);
ALTER TABLE ONLY seats ADD CONSTRAINT seats_seatnumber_location_id_key UNIQUE (seatnumber, location_id);
ALTER TABLE ONLY user_roles ADD CONSTRAINT user_roles_user_id_role_key UNIQUE (user_id, role);
ALTER TABLE ONLY user_tags ADD CONSTRAINT user_tags_user_id_tags_key UNIQUE (user_id, tags);

CREATE INDEX idx_user_event ON email_seat_map_tokens USING btree (user_id, event_id);

ALTER TABLE ONLY refresh_tokens ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY event_supervisors ADD CONSTRAINT fk3aoscdyd7pr6u826dxmj5eq34 FOREIGN KEY (event_id) REFERENCES events(id);
ALTER TABLE ONLY event_location_markers ADD CONSTRAINT fk3svetled00reig5xnq9t4uucj FOREIGN KEY (event_location_id) REFERENCES eventlocations(id);
ALTER TABLE ONLY events ADD CONSTRAINT fk4lsvuu8y3xvo76gd0q1u30nnj FOREIGN KEY (manager_id) REFERENCES users(id);
ALTER TABLE ONLY reservations ADD CONSTRAINT fk57amums6j9fkqwbpw4oceyw9i FOREIGN KEY (seat_id) REFERENCES seats(id);
ALTER TABLE ONLY event_location_entrances ADD CONSTRAINT fk5d8980bb9w8gflo6kuaulslmc FOREIGN KEY (event_location_id) REFERENCES eventlocations(id);
ALTER TABLE ONLY eventuserallowance ADD CONSTRAINT fk5xruwwuqkxk5ufwpe5456qu84 FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY webauthn_credentials ADD CONSTRAINT fk61k8kijke2qqqpsrg65qjwcie FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY eventuserallowance ADD CONSTRAINT fk7jrv62d8lhjl3a20qv7dupmcc FOREIGN KEY (event_id) REFERENCES events(id);
ALTER TABLE ONLY outbound_email_cc ADD CONSTRAINT fk9iqj75se6yd752rpnvh444t2y FOREIGN KEY (email_id) REFERENCES outbound_emails(id);
ALTER TABLE ONLY reservations ADD CONSTRAINT fkb5g9io5h54iwl2inkno50ppln FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY email_verification ADD CONSTRAINT fkbh3863tiicveqq2k27uooni0g FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY reservations ADD CONSTRAINT fkcnr8finplwp8whntrr02jpvre FOREIGN KEY (event_id) REFERENCES events(id);
ALTER TABLE ONLY user_tags ADD CONSTRAINT fkdylhtw3qjb2nj40xp50b0p495 FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY seats ADD CONSTRAINT fkewopqcg6ejtw7w1cr72h81uic FOREIGN KEY (entrance_id) REFERENCES event_location_entrances(id);
ALTER TABLE ONLY event_location_area_boundary ADD CONSTRAINT fkfho28q7mc9mn6ku9vaah1ecad FOREIGN KEY (area_id) REFERENCES event_location_areas(id);
ALTER TABLE ONLY event_supervisors ADD CONSTRAINT fkh5dc0cko6fxnrh14ab4u9hgsq FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY events ADD CONSTRAINT fkhbmiaeypek6v8lya3ekrbim3l FOREIGN KEY (event_location_id) REFERENCES eventlocations(id);
ALTER TABLE ONLY user_roles ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY email_seat_map_tokens ADD CONSTRAINT fki7ok9vlqtnek7sfpejikoppbm FOREIGN KEY (event_id) REFERENCES events(id);
ALTER TABLE ONLY eventlocations ADD CONSTRAINT fkjyf4c2l9fjuhk4lfjtd1nc813 FOREIGN KEY (manager_id) REFERENCES users(id);
ALTER TABLE ONLY outbound_email_bcc ADD CONSTRAINT fkkbqiu53cwbwt37ius2rmdgrn3 FOREIGN KEY (email_id) REFERENCES outbound_emails(id);
ALTER TABLE ONLY outbound_email_attachments ADD CONSTRAINT fkodwuh62l6gv1jsvllvodtwwwn FOREIGN KEY (email_id) REFERENCES outbound_emails(id);
ALTER TABLE ONLY outbound_email_recipients ADD CONSTRAINT fkoia5cdiai43feapmxw3fh7aqn FOREIGN KEY (email_id) REFERENCES outbound_emails(id);
ALTER TABLE ONLY email_seat_map_tokens ADD CONSTRAINT fkomqn7ooasx3jq8oo2hs3lw2vf FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY email_seat_map_token_seats ADD CONSTRAINT fkpywj3cn0csx3jpr0sneprc7wo FOREIGN KEY (token_id) REFERENCES email_seat_map_tokens(id);
ALTER TABLE ONLY seats ADD CONSTRAINT fkqghl2hgr3uhl9krv29b4xeqcq FOREIGN KEY (area_id) REFERENCES event_location_areas(id);
ALTER TABLE ONLY event_location_areas ADD CONSTRAINT fkscw524e3yojfvg7ed2chgccxr FOREIGN KEY (event_location_id) REFERENCES eventlocations(id);
ALTER TABLE ONLY seats ADD CONSTRAINT fktbc2ac345o9925k9xgxxchgkm FOREIGN KEY (location_id) REFERENCES eventlocations(id);
ALTER TABLE ONLY login_attempts ADD CONSTRAINT fktg9vhke4mlf5vij2rcvfk2dg2 FOREIGN KEY (user_id) REFERENCES users(id);
