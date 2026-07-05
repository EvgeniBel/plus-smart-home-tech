-- Создание базы данных
CREATE DATABASE shopping_cart;

-- Подключение к базе
\c shopping_cart;

-- Создание таблицы корзин
CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы элементов корзины
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    price_at_addition DECIMAL(19,2) NOT NULL
);

-- Индексы для оптимизации
CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);
CREATE INDEX idx_carts_username ON carts(username);
CREATE INDEX idx_carts_active ON carts(active);

-- Тестовые данные
INSERT INTO carts (id, username, active) VALUES
    (gen_random_uuid(), 'test_user1', true),
    (gen_random_uuid(), 'test_user2', true),
    (gen_random_uuid(), 'test_user3', false);

INSERT INTO cart_items (cart_id, product_id, quantity, price_at_addition)
SELECT
    c.id,
    '123e4567-e89b-12d3-a456-426614174000'::UUID,
    2,
    1499.99
FROM carts c WHERE c.username = 'test_user1';

INSERT INTO cart_items (cart_id, product_id, quantity, price_at_addition)
SELECT
    c.id,
    '223e4567-e89b-12d3-a456-426614174001'::UUID,
    1,
    899.50
FROM carts c WHERE c.username = 'test_user1';