-- Flyway Migration V4: Support Split Payment and Kasbon/Tempo (Credit & Receivables)

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS due_date TIMESTAMP;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS due_date TIMESTAMP;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS notes VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_transactions_customer_status ON transactions(customer_id, status);
