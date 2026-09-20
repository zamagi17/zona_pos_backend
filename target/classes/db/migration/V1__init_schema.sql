-- Flyway Migration V1: Initial Schema for Zona POS (aligned with ERD)

-- 1. Roles
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Tenants
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(255) NOT NULL,
    owner_name VARCHAR(150) NOT NULL,
    owner_phone VARCHAR(20) NOT NULL,
    owner_email VARCHAR(150) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- 3. Outlets
CREATE TABLE outlets (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(255) NOT NULL,
    manager_name VARCHAR(150),
    manager_phone VARCHAR(20),
    manager_email VARCHAR(150),
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- 4. Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    outlet_id BIGINT REFERENCES outlets(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    role_id INT NOT NULL REFERENCES roles(id),
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- 5. Category (supports hierarchical categories)
CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    parent_id BIGINT REFERENCES category(id) ON DELETE SET NULL,
    is_parent BOOLEAN NOT NULL DEFAULT FALSE,
    "desc" VARCHAR(300),
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    CONSTRAINT uq_category_tenant_code UNIQUE (tenant_id, code)
);

-- 6. Units
CREATE TABLE units (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    size INT NOT NULL DEFAULT 1,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    CONSTRAINT uq_unit_tenant_code UNIQUE (tenant_id, code)
);

-- 7. Storages (Gudang)
CREATE TABLE storages (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    CONSTRAINT uq_storage_tenant_code UNIQUE (tenant_id, code)
);

-- 8. Products
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL,
    barcode VARCHAR(100),
    name VARCHAR(150) NOT NULL,
    "desc" VARCHAR(300),
    category_id BIGINT REFERENCES category(id) ON DELETE SET NULL,
    unit_id BIGINT REFERENCES units(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    CONSTRAINT uq_product_tenant_sku UNIQUE (tenant_id, sku)
);

-- 9. Product Variants
CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- 10. Stocks (Inventory in Outlets or Storages)
CREATE TABLE stocks (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE CASCADE,
    outlet_id BIGINT REFERENCES outlets(id) ON DELETE CASCADE,
    storage_id BIGINT REFERENCES storages(id) ON DELETE CASCADE,
    quantity BIGINT NOT NULL DEFAULT 0,
    minimum BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- 11. Stock History (Audit Trail for Inventory)
CREATE TABLE stock_history (
    id BIGSERIAL PRIMARY KEY,
    stock_id BIGINT NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    stock_in_out BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL, -- IN, OUT
    type VARCHAR(50) NOT NULL,   -- SALE, PURCHASE, ADJUSTMENT, TRANSFER
    remarks VARCHAR(255),
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. Prices (Multi-tier Pricing per Outlet)
CREATE TABLE prices (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    outlet_id BIGINT NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    selling_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    purchase_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount_percentage SMALLINT NOT NULL DEFAULT 0,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_percentage SMALLINT NOT NULL DEFAULT 0,
    tax_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    CONSTRAINT uq_price_product_outlet UNIQUE (product_id, outlet_id)
);

-- 13. Price History
CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    price_id BIGINT NOT NULL REFERENCES prices(id) ON DELETE CASCADE,
    selling_price DOUBLE PRECISION NOT NULL,
    purchase_price DOUBLE PRECISION NOT NULL,
    discount_percentage SMALLINT NOT NULL DEFAULT 0,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_percentage SMALLINT NOT NULL DEFAULT 0,
    tax_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. Customers
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_tenant_phone UNIQUE (tenant_id, phone)
);

-- 15. Cashier Shifts
CREATE TABLE cashier_shifts (
    id BIGSERIAL PRIMARY KEY,
    outlet_id BIGINT NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_cash DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    expected_cash DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    actual_cash DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP
);

-- 16. Transactions
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    trx_no VARCHAR(100) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    outlet_id BIGINT NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    shift_id BIGINT REFERENCES cashier_shifts(id) ON DELETE SET NULL,
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    grand_total DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, BOOKED, CONFIRMED, COMPLETED, CANCELED, REFUNDED
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- 17. Transaction Items
CREATE TABLE transaction_items (
    id BIGSERIAL PRIMARY KEY,
    trx_id BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    outlet_id BIGINT NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE SET NULL,
    quantity INT NOT NULL DEFAULT 1,
    base_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    unit_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- 18. Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    trx_id BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    payment_method VARCHAR(50) NOT NULL, -- CASH, QRIS, Transfer, Debit Card
    amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'PAID', -- UNPAID, PARTIAL, PAID, REFUNDED
    reference VARCHAR(150),
    paid_at TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- 19. Transaction History
CREATE TABLE transaction_history (
    id BIGSERIAL PRIMARY KEY,
    trx_id BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    trx_no VARCHAR(100) NOT NULL,
    outlet_id BIGINT NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    grand_total DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_stocks_product ON stocks(product_id);
CREATE INDEX idx_stocks_outlet ON stocks(outlet_id);
CREATE INDEX idx_stocks_storage ON stocks(storage_id);
CREATE INDEX idx_prices_outlet ON prices(outlet_id);
CREATE INDEX idx_transactions_tenant ON transactions(tenant_id);
CREATE INDEX idx_transactions_outlet ON transactions(outlet_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_shift ON transactions(shift_id);
