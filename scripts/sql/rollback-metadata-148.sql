-- Yume S6: recovery in a disposable copy only. Export decisions/back up the database first.
-- SQLDelight does not support application downgrade; do not change user_version on a live database.
BEGIN TRANSACTION;
DROP TABLE IF EXISTS metadata_work;
DROP TABLE IF EXISTS anime_metadata;
DROP TABLE IF EXISTS metadata_image_index;
DROP TABLE IF EXISTS metadata_restore_pending;
DROP TABLE IF EXISTS metadata_lookup_cache;
DROP TABLE IF EXISTS metadata_media_cache;
DROP TABLE IF EXISTS metadata_control;
COMMIT;
