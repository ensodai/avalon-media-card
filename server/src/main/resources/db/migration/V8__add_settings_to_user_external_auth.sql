-- V8__add_settings_to_user_external_auth.sql
ALTER TABLE user_external_auth
    ADD COLUMN settings TEXT;
