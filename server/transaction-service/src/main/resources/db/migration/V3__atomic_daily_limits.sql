-- Atomic counter for each customer's outgoing payments per bank day (America/Bogota).
-- The transaction-service reserves the amount inside the same transaction that records the
-- instruction, so concurrent payments cannot both spend the same remaining limit.
CREATE TABLE daily_limit_usage (
    customer_id BIGINT NOT NULL,
    usage_date DATE NOT NULL,
    used_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (customer_id, usage_date),
    CONSTRAINT daily_limit_usage_non_negative CHECK (used_amount >= 0)
);

-- Track which reservation a transaction owns so a definitive failure can compensate it.
ALTER TABLE transactions
    ADD COLUMN limit_usage_date DATE,
    ADD COLUMN limit_released BOOLEAN NOT NULL DEFAULT FALSE;

-- Seed the counter from existing outgoing instructions so deploying this migration does not
-- reset today's usage.
INSERT INTO daily_limit_usage (customer_id, usage_date, used_amount)
SELECT source_customer_id,
       (created_at AT TIME ZONE 'America/Bogota')::date,
       SUM(amount)
FROM transactions
WHERE source_customer_id IS NOT NULL
  AND type IN ('TRANSFER', 'WITHDRAWAL')
  AND status IN ('PENDING', 'COMPLETED')
GROUP BY 1, 2
ON CONFLICT (customer_id, usage_date) DO NOTHING;

-- Pending instructions still own their reservation and can be compensated if recovery fails.
-- Completed instructions no longer need one; failed rows never had a usable reservation.
UPDATE transactions
SET limit_usage_date = (created_at AT TIME ZONE 'America/Bogota')::date
WHERE source_customer_id IS NOT NULL
  AND type IN ('TRANSFER', 'WITHDRAWAL')
  AND status = 'PENDING';

UPDATE transactions
SET limit_usage_date = (created_at AT TIME ZONE 'America/Bogota')::date,
    limit_released = TRUE
WHERE source_customer_id IS NOT NULL
  AND type IN ('TRANSFER', 'WITHDRAWAL')
  AND status = 'COMPLETED';
