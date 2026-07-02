-- Создание базы данных
CREATE DATABASE shopping_store;

-- Подключение к базе
\c shopping_store;

-- Создание таблицы продуктов
CREATE TABLE IF NOT EXISTS products (
    product_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    image_src VARCHAR(500),
    price DECIMAL(19,2) NOT NULL,
    product_category VARCHAR(50) NOT NULL,
    quantity_state VARCHAR(50) NOT NULL,
    product_state VARCHAR(50) NOT NULL
);

-- Индексы для оптимизации
CREATE INDEX idx_products_category_state ON products(product_category, product_state);
CREATE INDEX idx_products_state ON products(product_state);

-- Тестовые данные
INSERT INTO products (product_id, product_name, description, image_src, price, product_category, quantity_state, product_state)
VALUES
    (gen_random_uuid(), 'Умная лампа', 'Светодиодная лампа с управлением через Wi-Fi', 'images/lamp.jpg', 1499.99, 'LIGHTING', 'MANY', 'ACTIVE'),
    (gen_random_uuid(), 'Датчик движения', 'Датчик движения для системы умного дома', 'images/motion.jpg', 899.50, 'SENSORS', 'ENOUGH', 'ACTIVE'),
    (gen_random_uuid(), 'Центральный контроллер', 'Главный контроллер умного дома', 'images/controller.jpg', 4999.00, 'CONTROL', 'FEW', 'ACTIVE'),
    (gen_random_uuid(), 'LED лента', 'RGB LED лента 5 метров', 'images/led.jpg', 2499.00, 'LIGHTING', 'ENOUGH', 'ACTIVE'),
    (gen_random_uuid(), 'Датчик температуры', 'Беспроводной датчик температуры', 'images/temp.jpg', 1299.00, 'SENSORS', 'FEW', 'DEACTIVATE');