-- "Shard by user_id" on Postgres = declarative hash partitioning, not a physical shard per
-- user. Hibernate's ddl-auto=validate (see application.properties) expects the schema to
-- already exist - it will NOT create this partitioned structure for you, so run this
-- migration (or the Flyway/Liquibase equivalent) before first boot.

CREATE TABLE trade_order (
    user_id                     UUID        NOT NULL,
    order_id                    UUID        NOT NULL DEFAULT gen_random_uuid(),
    stock_ticker                VARCHAR(16) NOT NULL,
    quantity                    BIGINT      NOT NULL,
    side                        VARCHAR(8)  NOT NULL,
    status                      VARCHAR(16) NOT NULL,
    executed_price_minor_units  BIGINT,
    limit_price_minor_units     BIGINT,
    idempotency_key             TEXT        NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                     BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, order_id)
) PARTITION BY HASH (user_id);

-- Create as many partitions as your expected write volume calls for - 16 is a reasonable
-- starting point for a few million users; re-partitioning later means a migration, so size
-- generously rather than growing this incrementally.
DO $$
BEGIN
    FOR i IN 0..15 LOOP
        EXECUTE format(
            'CREATE TABLE trade_order_p%1$s PARTITION OF trade_order FOR VALUES WITH (MODULUS 16, REMAINDER %1$s)',
            i
        );
    END LOOP;
END $$;

CREATE UNIQUE INDEX idx_order_idempotency_key ON trade_order (idempotency_key);
CREATE INDEX idx_order_user_status ON trade_order (user_id, status);

-- outbox_event is deliberately NOT partitioned - it's a short-lived append-only table that
-- Debezium tails and that you'd periodically prune (rows older than N days with status =
-- PUBLISHED), so partitioning adds operational overhead with little benefit here.
CREATE TABLE outbox_event (
    id             UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   TEXT        NOT NULL,
    type           VARCHAR(64) NOT NULL,
    payload        TEXT        NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'NEW',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Required for Debezium's Postgres connector (logical decoding) to see changes at all.
-- Set on the server (postgresql.conf), included here as a reminder, not something this
-- script can apply itself:
-- ALTER SYSTEM SET wal_level = 'logical';
