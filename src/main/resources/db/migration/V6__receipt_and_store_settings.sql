-- Flyway Migration V6: Klaster 4 - Pengaturan Header & Footer Struk Thermal (58mm & 80mm)
-- Kustomisasi Logo, Nama Bisnis, Alamat, Kontak, Medsos, Catatan Kaki, dan Ukuran Kertas Thermal

CREATE TABLE IF NOT EXISTS receipt_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    outlet_id BIGINT REFERENCES outlets(id) ON DELETE CASCADE,
    business_name VARCHAR(150),
    logo_url TEXT,
    address TEXT,
    phone VARCHAR(50),
    instagram VARCHAR(100),
    website VARCHAR(150),
    footer_note TEXT,
    paper_size VARCHAR(10) NOT NULL DEFAULT '58mm', -- 58mm, 80mm
    show_logo BOOLEAN NOT NULL DEFAULT TRUE,
    show_social_media BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    CONSTRAINT uq_receipt_settings_tenant_outlet UNIQUE (tenant_id, outlet_id)
);

CREATE INDEX IF NOT EXISTS idx_receipt_settings_tenant ON receipt_settings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_receipt_settings_outlet ON receipt_settings(outlet_id);

-- Seed initial setting for tenant 1 & outlet 1 if exists
INSERT INTO receipt_settings (tenant_id, outlet_id, business_name, address, phone, instagram, footer_note, paper_size, show_logo, show_social_media, created_by)
SELECT 1, 1, 'ZONA POS RETAIL & CAFE', 'Jl. Malioboro No. 45, Yogyakarta', '0812-3456-7890', '@zonapos.id', 'Barang yang sudah dibeli tidak dapat ditukar atau dikembalikan.\nTerima kasih atas kunjungan Anda!', '58mm', true, true, 'System'
WHERE EXISTS (SELECT 1 FROM tenants WHERE id = 1)
ON CONFLICT (tenant_id, outlet_id) DO NOTHING;
