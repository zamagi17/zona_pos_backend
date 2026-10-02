-- Flyway Migration V3: Cash Movements (Petty Cash / Cash In - Cash Out)
-- Mendukung operasional kas kecil kasir: beli es batu, bayar galon, tambah modal kembalian

ALTER TABLE cashier_shifts ADD COLUMN IF NOT EXISTS total_cash_in DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE cashier_shifts ADD COLUMN IF NOT EXISTS total_cash_out DOUBLE PRECISION NOT NULL DEFAULT 0.0;

CREATE TABLE IF NOT EXISTS cash_movements (
    id BIGSERIAL PRIMARY KEY,
    shift_id BIGINT NOT NULL REFERENCES cashier_shifts(id) ON DELETE CASCADE,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    outlet_id BIGINT NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL, -- CASH_IN, CASH_OUT
    amount DOUBLE PRECISION NOT NULL,
    category VARCHAR(100) NOT NULL, -- Operasional, Bahan, Tambah Modal, Kurir, Lainnya
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cash_movements_shift ON cash_movements(shift_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_tenant_outlet ON cash_movements(tenant_id, outlet_id);
