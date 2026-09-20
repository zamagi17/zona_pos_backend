-- Flyway Migration V2: Seed Demo Data for Zona POS

-- 1. Seed Roles
INSERT INTO roles (id, name) VALUES 
(1, 'ROLE_TENANT_OWNER'),
(2, 'ROLE_OUTLET_MANAGER'),
(3, 'ROLE_CASHIER')
ON CONFLICT (id) DO NOTHING;

-- 2. Seed Tenant
INSERT INTO tenants (id, name, address, owner_name, owner_phone, owner_email, status, created_by) VALUES
(1, 'Kopi & Retail Nusantara UMKM', 'Jl. Sudirman Kav 21, Jakarta Pusat', 'Bpk. Hendra Wijaya', '081122334455', 'owner@zonapos.com', 'ACTIVE', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 3. Seed Outlets
INSERT INTO outlets (id, tenant_id, name, address, manager_name, manager_phone, manager_email, created_by) VALUES
(1, 1, 'Cabang Utama Thamrin', 'Jl. M.H. Thamrin No. 10, Jakarta Pusat', 'Rian Pratama', '081298765432', 'manager@zonapos.com', 'SYSTEM'),
(2, 1, 'Cabang Bandung Dago', 'Jl. Ir. H. Juanda No. 88, Bandung', 'Dian Lestari', '081311223344', 'dian.dago@zonapos.com', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 4. Seed Storages (Gudang Pusat)
INSERT INTO storages (id, tenant_id, code, name, created_by) VALUES
(1, 1, 'STR-JKT-01', 'Gudang Pusat Jakarta Hub', 'SYSTEM'),
(2, 1, 'STR-BDO-01', 'Gudang Logistik Bandung', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 5. Seed Users (password123 -> $2a$10$wT02j6/o3q7QO5nI47k6.O9xGkM6p87kYkF8c6Kj4j.5j9Y0e6Gmi or standard BCrypt)
-- Hash below is BCrypt for 'password123'
INSERT INTO users (id, tenant_id, outlet_id, name, email, password, phone, is_active, role_id, created_by) VALUES
(1, 1, NULL, 'Hendra Wijaya (Owner)', 'owner@zonapos.com', '$2a$10$sCZXDOSQFuw/UvLcjTkqb.FK1/Qn8wuqgQR50cIQU0qTdv2aQS.ZS', '081122334455', true, 1, 'SYSTEM'),
(2, 1, 1, 'Rian Pratama (Manager Thamrin)', 'manager@zonapos.com', '$2a$10$sCZXDOSQFuw/UvLcjTkqb.FK1/Qn8wuqgQR50cIQU0qTdv2aQS.ZS', '081298765432', true, 2, 'SYSTEM'),
(3, 1, 1, 'Siti Kasir (Kasir Thamrin)', 'kasir@zonapos.com', '$2a$10$sCZXDOSQFuw/UvLcjTkqb.FK1/Qn8wuqgQR50cIQU0qTdv2aQS.ZS', '081344556677', true, 3, 'SYSTEM')
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

-- 6. Seed Categories
INSERT INTO category (id, tenant_id, code, name, parent_id, is_parent, "desc", created_by) VALUES
(1, 1, 'CAT-COFFEE', 'Minuman & Kopi', NULL, false, 'Aneka minuman kopi dan non-kopi', 'SYSTEM'),
(2, 1, 'CAT-FOOD', 'Makanan & Pastry', NULL, false, 'Snack, pastry, dan makanan utama', 'SYSTEM'),
(3, 1, 'CAT-RETAIL', 'Retail & Merchandise', NULL, false, 'Barang retail, tumbler, dan merchandise', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 7. Seed Units
INSERT INTO units (id, tenant_id, code, name, size, created_by) VALUES
(1, 1, 'CUP', 'Cup Regular', 1, 'SYSTEM'),
(2, 1, 'PCS', 'Pieces', 1, 'SYSTEM'),
(3, 1, 'PACK', 'Pack / Box', 1, 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 8. Seed Customers
INSERT INTO customers (id, tenant_id, name, phone, email) VALUES
(1, 1, 'Budi Santoso', '081234567890', 'budi.santoso@gmail.com'),
(2, 1, 'Anisa Rahmawati', '081987654321', 'anisa.rahma@gmail.com')
ON CONFLICT (id) DO NOTHING;

-- 9. Seed Products
INSERT INTO products (id, tenant_id, sku, barcode, name, "desc", category_id, unit_id, is_active, created_by) VALUES
(1, 1, 'KOP-001', '899100101', 'Kopi Susu Gula Aren', 'Espresso dengan susu creamy dan gula aren organik', 1, 1, true, 'SYSTEM'),
(2, 1, 'KOP-002', '899100102', 'Americano Robusta', 'Espresso shot ganda dengan air mineral dingin/hangat', 1, 1, true, 'SYSTEM'),
(3, 1, 'MCK-001', '899200101', 'Croissant Butter Perancis', 'Pastry renyah dengan mentega impor berkualitas', 2, 2, true, 'SYSTEM'),
(4, 1, 'MCK-002', '899200102', 'Kentang Goreng Keju', 'French fries renyah dengan taburan saus keju cheddar', 2, 2, true, 'SYSTEM'),
(5, 1, 'RTL-001', '899300101', 'Tumbler Stainless Zona POS 500ml', 'Tumbler tahan dingin dan panas hingga 12 jam', 3, 2, true, 'SYSTEM'),
(6, 1, 'RTL-002', '899300102', 'Kaos Kafe Nusantara Size L', 'Bahan katun combed 30s premium nyaman dan adem', 3, 2, true, 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 10. Seed Prices (for Outlet 1)
INSERT INTO prices (id, product_id, outlet_id, selling_price, purchase_price, discount_percentage, discount_amount, tax_percentage, tax_amount, created_by) VALUES
(1, 1, 1, 18000, 8000, 0, 0, 0, 0, 'SYSTEM'),
(2, 2, 1, 15000, 6000, 0, 0, 0, 0, 'SYSTEM'),
(3, 3, 1, 22000, 10000, 0, 0, 0, 0, 'SYSTEM'),
(4, 4, 1, 20000, 9000, 0, 0, 0, 0, 'SYSTEM'),
(5, 5, 1, 75000, 40000, 0, 0, 0, 0, 'SYSTEM'),
(6, 6, 1, 85000, 45000, 0, 0, 0, 0, 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 11. Seed Stocks (Outlet 1 & Storage 1)
-- Stocks in Outlet 1
INSERT INTO stocks (id, product_id, variant_id, outlet_id, storage_id, quantity, minimum, created_by) VALUES
(1, 1, NULL, 1, NULL, 50, 10, 'SYSTEM'),
(2, 2, NULL, 1, NULL, 40, 10, 'SYSTEM'),
(3, 3, NULL, 1, NULL, 25, 5, 'SYSTEM'),
(4, 4, NULL, 1, NULL, 30, 5, 'SYSTEM'),
(5, 5, NULL, 1, NULL, 15, 3, 'SYSTEM'),
(6, 6, NULL, 1, NULL, 8, 2, 'SYSTEM'),
-- Stocks in Storage 1 (Gudang Pusat Jakarta)
(7, 1, NULL, NULL, 1, 200, 20, 'SYSTEM'),
(8, 2, NULL, NULL, 1, 150, 20, 'SYSTEM'),
(9, 3, NULL, NULL, 1, 80, 10, 'SYSTEM'),
(10, 4, NULL, NULL, 1, 100, 10, 'SYSTEM'),
(11, 5, NULL, NULL, 1, 60, 5, 'SYSTEM'),
(12, 6, NULL, NULL, 1, 30, 5, 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 12. Seed Stock History (Initial Stock Setup)
INSERT INTO stock_history (stock_id, stock_in_out, quantity, status, type, remarks, user_id, created_at) VALUES
(1, 50, 50, 'IN', 'ADJUSTMENT', 'Stok awal outlet Thamrin', 1, CURRENT_TIMESTAMP),
(2, 40, 40, 'IN', 'ADJUSTMENT', 'Stok awal outlet Thamrin', 1, CURRENT_TIMESTAMP),
(3, 25, 25, 'IN', 'ADJUSTMENT', 'Stok awal outlet Thamrin', 1, CURRENT_TIMESTAMP),
(4, 30, 30, 'IN', 'ADJUSTMENT', 'Stok awal outlet Thamrin', 1, CURRENT_TIMESTAMP),
(5, 15, 15, 'IN', 'ADJUSTMENT', 'Stok awal outlet Thamrin', 1, CURRENT_TIMESTAMP),
(6, 8, 8, 'IN', 'ADJUSTMENT', 'Stok awal outlet Thamrin', 1, CURRENT_TIMESTAMP),
(7, 200, 200, 'IN', 'PURCHASE', 'Barang masuk supplier gudang pusat', 1, CURRENT_TIMESTAMP),
(8, 150, 150, 'IN', 'PURCHASE', 'Barang masuk supplier gudang pusat', 1, CURRENT_TIMESTAMP);

-- Reset Sequences to ensure future inserts don't collide with seeded IDs
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('tenants_id_seq', (SELECT MAX(id) FROM tenants));
SELECT setval('outlets_id_seq', (SELECT MAX(id) FROM outlets));
SELECT setval('storages_id_seq', (SELECT MAX(id) FROM storages));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('category_id_seq', (SELECT MAX(id) FROM category));
SELECT setval('units_id_seq', (SELECT MAX(id) FROM units));
SELECT setval('customers_id_seq', (SELECT MAX(id) FROM customers));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('prices_id_seq', (SELECT MAX(id) FROM prices));
SELECT setval('stocks_id_seq', (SELECT MAX(id) FROM stocks));
