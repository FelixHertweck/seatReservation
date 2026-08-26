-- Migration V13: Add ON DELETE CASCADE / ON DELETE SET NULL to foreign keys referencing users(id)

-- 1. refresh_tokens
ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS fk1lih5y2npsf8u5o3vhdb9y0os;
ALTER TABLE refresh_tokens ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 2. eventuserallowance
ALTER TABLE eventuserallowance DROP CONSTRAINT IF EXISTS fk5xruwwuqkxk5ufwpe5456qu84;
ALTER TABLE eventuserallowance ADD CONSTRAINT fk5xruwwuqkxk5ufwpe5456qu84 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 3. webauthn_credentials
ALTER TABLE webauthn_credentials DROP CONSTRAINT IF EXISTS fk61k8kijke2qqqpsrg65qjwcie;
ALTER TABLE webauthn_credentials ADD CONSTRAINT fk61k8kijke2qqqpsrg65qjwcie FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 4. reservations
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS fkb5g9io5h54iwl2inkno50ppln;
ALTER TABLE reservations ADD CONSTRAINT fkb5g9io5h54iwl2inkno50ppln FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 5. email_verification
ALTER TABLE email_verification DROP CONSTRAINT IF EXISTS fkbh3863tiicveqq2k27uooni0g;
ALTER TABLE email_verification ADD CONSTRAINT fkbh3863tiicveqq2k27uooni0g FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 6. user_tags
ALTER TABLE user_tags DROP CONSTRAINT IF EXISTS fkdylhtw3qjb2nj40xp50b0p495;
ALTER TABLE user_tags ADD CONSTRAINT fkdylhtw3qjb2nj40xp50b0p495 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 7. event_supervisors
ALTER TABLE event_supervisors DROP CONSTRAINT IF EXISTS fkh5dc0cko6fxnrh14ab4u9hgsq;
ALTER TABLE event_supervisors ADD CONSTRAINT fkh5dc0cko6fxnrh14ab4u9hgsq FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 8. user_roles
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS fkhfh9dx7w3ubf1co1vdev94g3f;
ALTER TABLE user_roles ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 9. email_seat_map_tokens
ALTER TABLE email_seat_map_tokens DROP CONSTRAINT IF EXISTS fkomqn7ooasx3jq8oo2hs3lw2vf;
ALTER TABLE email_seat_map_tokens ADD CONSTRAINT fkomqn7ooasx3jq8oo2hs3lw2vf FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 10. email_seat_map_token_seats
ALTER TABLE email_seat_map_token_seats DROP CONSTRAINT IF EXISTS fkpywj3cn0csx3jpr0sneprc7wo;
ALTER TABLE email_seat_map_token_seats ADD CONSTRAINT fkpywj3cn0csx3jpr0sneprc7wo FOREIGN KEY (token_id) REFERENCES email_seat_map_tokens(id) ON DELETE CASCADE;

-- 11. login_attempts
ALTER TABLE login_attempts DROP CONSTRAINT IF EXISTS fktg9vhke4mlf5vij2rcvfk2dg2;
ALTER TABLE login_attempts ADD CONSTRAINT fktg9vhke4mlf5vij2rcvfk2dg2 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 12. password_reset_tokens
ALTER TABLE password_reset_tokens DROP CONSTRAINT IF EXISTS password_reset_tokens_user_id_fkey;
ALTER TABLE password_reset_tokens ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 13. events (created_by_user_id)
ALTER TABLE events DROP CONSTRAINT IF EXISTS fk4lsvuu8y3xvo76gd0q1u30nnj;
ALTER TABLE events ADD CONSTRAINT fk4lsvuu8y3xvo76gd0q1u30nnj FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- 14. eventlocations (created_by_user_id)
ALTER TABLE eventlocations DROP CONSTRAINT IF EXISTS fkjyf4c2l9fjuhk4lfjtd1nc813;
ALTER TABLE eventlocations ADD CONSTRAINT fkjyf4c2l9fjuhk4lfjtd1nc813 FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;
