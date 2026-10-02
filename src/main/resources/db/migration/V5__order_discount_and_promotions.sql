-- Flyway Migration V5: Klaster 3 - Diskon Global Nota & Kode Voucher Promosi
-- Mendukung diskon nota per-transaksi (persen/nominal) dan voucher promo kupon (HEMAT10, GRANDOPENING, dll)

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS order_discount DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS order_discount_type VARCHAR(20); -- PERCENT, FIXED
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS order_discount_rate DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(50);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS voucher_discount DOUBLE PRECISION NOT NULL DEFAULT 0.0;

ALTER TABLE transaction_history ADD COLUMN IF NOT EXISTS order_discount DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE transaction_history ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(50);
ALTER TABLE transaction_history ADD COLUMN IF NOT EXISTS voucher_discount DOUBLE PRECISION NOT NULL DEFAULT 0.0;

CREATE TABLE IF NOT EXISTS promotions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    outlet_id BIGINT REFERENCES outlets(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENT', -- PERCENT, FIXED
    discount_value DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    min_order_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    max_discount_amount DOUBLE PRECISION,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    usage_limit INT,
    times_used INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    CONSTRAINT uq_promotions_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_promotions_tenant_active ON promotions(tenant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(code);

-- Seed Initial Voucher Promosi untuk Tenant Utama (Tenant ID 1 jika ada)
INSERT INTO promotions (tenant_id, code, name, description, discount_type, discount_value, min_order_amount, max_discount_amount, is_active, created_by)
SELECT 1, 'HEMAT10', 'Promo Hemat 10%', 'Diskon 10% maksimal Rp 50.000 untuk minimal belanja Rp 50.000', 'PERCENT', 10.0, 50000.0, 50000.0, true, 'System'
WHERE EXISTS (SELECT 1 FROM tenants WHERE id = 1)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO promotions (tenant_id, code, name, description, discount_type, discount_value, min_order_amount, is_active, created_by)
SELECT 1, 'GRANDOPENING', 'Promo Grand Opening Rp 15.000', 'Potongan langsung Rp 15.000 untuk minimal transaksi Rp 100.000', 'FIXED', 15000.0, 100000.0, true, 'System'
WHERE EXISTS (SELECT 1 FROM tenants WHERE id = 1)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO promotions (tenant_id, code, name, description, discount_type, discount_value, min_order_amount, is_active, created_by)
SELECT 1, 'MEMBER5', 'Diskon Spesial Member 5%', 'Diskon langsung 5% tanpa syarat minimal belanja', 'PERCENT', 5.0, 0.0, true, 'System'
WHERE EXISTS (SELECT 1 FROM tenants WHERE id = 1)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO promotions (tenant_id, code, name, description, discount_type, discount_value, min_order_amount, is_active, created_by)
SELECT 1, 'DISKON25K', 'Promo Spesial Rp 25.000', 'Potongan Rp 25.000 untuk pembelanjaan di atas Rp 150.000', 'FIXED', 25000.0, 150000.0, true, 'System'
WHERE EXISTS (SELECT 1 FROM tenants WHERE id = 1)
ON CONFLICT (tenant_id, code) DO NOTHING;
