-- Создание базы данных
CREATE DATABASE IF NOT EXISTS warehouse;

-- Подключение к базе
\c warehouse;

-- Создание таблицы товаров на складе
CREATE TABLE IF NOT EXISTS warehouse_products (
    product_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    weight DECIMAL(19,2) NOT NULL,
    width DECIMAL(19,2) NOT NULL,
    height DECIMAL(19,2) NOT NULL,
    depth DECIMAL(19,2) NOT NULL,
    fragile BOOLEAN NOT NULL DEFAULT FALSE,
    quantity INTEGER NOT NULL DEFAULT 0
);

-- Индексы для оптимизации
CREATE INDEX idx_warehouse_products_quantity ON warehouse_products(quantity);

-- Тестовые данные
INSERT INTO warehouse_products (product_id, name, description, weight, width, height, depth, fragile, quantity) VALUES
    ('123e4567-e89b-12d3-a456-426614174000', 'Умная лампа', 'Светодиодная лампа с управлением через Wi-Fi', 0.5, 10.0, 10.0, 10.0, true, 50),
    ('223e4567-e89b-12d3-a456-426614174001', 'Датчик движения', 'Датчик движения для системы умного дома', 0.2, 5.0, 5.0, 3.0, false, 30),
    ('323e4567-e89b-12d3-a456-426614174002', 'Центральный контроллер', 'Главный контроллер умного дома', 0.8, 15.0, 10.0, 5.0, false, 10);