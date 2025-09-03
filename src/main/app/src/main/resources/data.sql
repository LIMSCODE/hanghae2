-- 사용자 테스트 데이터
INSERT INTO users (username, email, balance, created_at, updated_at, version) VALUES
('user1', 'user1@test.com', 100000.00, NOW(), NOW(), 0),
('user2', 'user2@test.com', 50000.00, NOW(), NOW(), 0),
('user3', 'user3@test.com', 200000.00, NOW(), NOW(), 0);

-- 상품 테스트 데이터
INSERT INTO products (name, description, price, stock_quantity, is_active, created_at, updated_at, version) VALUES
('iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB', 1200000.00, 50, true, NOW(), NOW(), 0),
('Galaxy S24 Ultra', 'Samsung Galaxy S24 Ultra 512GB', 1100000.00, 30, true, NOW(), NOW(), 0),
('iPad Pro', 'Apple iPad Pro 12.9 inch', 1500000.00, 25, true, NOW(), NOW(), 0),
('MacBook Air', 'Apple MacBook Air M3', 1800000.00, 15, true, NOW(), NOW(), 0),
('AirPods Pro', 'Apple AirPods Pro 3rd Gen', 300000.00, 100, true, NOW(), NOW(), 0);

-- 판매 통계 테스트 데이터 (최근 3일)
INSERT INTO product_sales_statistics (product_id, sales_date, total_quantity, total_amount, created_at, updated_at) VALUES
-- 3일 전
(1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 15, 18000000.00, NOW(), NOW()),
(2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 12, 13200000.00, NOW(), NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 20, 6000000.00, NOW(), NOW()),

-- 2일 전
(1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 10, 12000000.00, NOW(), NOW()),
(2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 8, 8800000.00, NOW(), NOW()),
(3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 5, 7500000.00, NOW(), NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 15, 4500000.00, NOW(), NOW()),

-- 1일 전 (오늘)
(1, CURDATE(), 20, 24000000.00, NOW(), NOW()),
(2, CURDATE(), 18, 19800000.00, NOW(), NOW()),
(3, CURDATE(), 3, 4500000.00, NOW(), NOW()),
(4, CURDATE(), 2, 3600000.00, NOW(), NOW()),
(5, CURDATE(), 25, 7500000.00, NOW(), NOW());