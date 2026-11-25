ALTER TABLE import_job ADD COLUMN idempotency_key VARCHAR(64);
ALTER TABLE import_job ADD COLUMN rubric_snapshot_json TEXT;
CREATE UNIQUE INDEX uk_import_batch_key ON import_job(batch_id, idempotency_key);
ALTER TABLE answer ADD COLUMN content_hash VARCHAR(64);
