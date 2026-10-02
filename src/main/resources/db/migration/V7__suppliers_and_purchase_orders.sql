-- V7__suppliers_and_purchase_orders.sql
-- Klaster 6: Supply Chain, Master Data Supplier, dan Purchase Order Tracking

CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(100),
    address TEXT,
    payment_terms VARCHAR(50) NOT NULL DEFAULT 'COD', -- COD, NET_7, NET_14, NET_30, TEMPO
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_suppliers_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX IF NOT EXISTS idx_suppliers_tenant_id ON suppliers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_is_active ON suppliers(tenant_id, is_active);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    po_no VARCHAR(50) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    supplier_id BIGINT REFERENCES suppliers(id) ON DELETE SET NULL,
    storage_id BIGINT REFERENCES storages(id) ON DELETE SET NULL,
    outlet_id BIGINT REFERENCES outlets(id) ON DELETE SET NULL,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    quantity BIGINT NOT NULL,
    purchase_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    invoice_no VARCHAR(100),
    remarks TEXT,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_po_tenant_id ON purchase_orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_product_id ON purchase_orders(product_id);
CREATE INDEX IF NOT EXISTS idx_po_supplier_id ON purchase_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_po_created_at ON purchase_orders(tenant_id, created_at DESC);

-- Seed initial default supplier for tenant 1
INSERT INTO suppliers (id, tenant_id, name, contact_person, phone, email, address, payment_terms, notes)
VALUES 
(1, 1, 'PT Sumber Pangan Nusantara', 'Hendra Setiawan', '0812-9876-5432', 'sales@sumberpangan.co.id', 'Kawasan Industri Pulogadung Blok B No. 12, Jakarta Timur', 'NET_30', 'Distributor resmi bahan baku F&B dan retail groceries'),
(2, 1, 'CV Kopi Nusantara Mandiri', 'Rian Pratama', '0813-1122-3344', 'order@kopinusantara.id', 'Jl. Raya Lembang No. 88, Bandung', 'COD', 'Supplier kopi arabika & robusta specialty'),
(3, 1, 'PT Mitra Retail Indonesia', 'Siti Rahma', '0818-5544-3322', 'mitra@retailindo.com', 'Jl. Rungkut Industri III No. 45, Surabaya', 'NET_14', 'Supplier produk kemasan, botol, dan perlengkapan kasir')
ON CONFLICT (tenant_id, name) DO NOTHING;
