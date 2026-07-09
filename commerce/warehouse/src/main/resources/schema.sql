-- Создание базы данных
CREATE DATABASE IF NOT EXISTS warehouse;

-- Подключение к базе
\c warehouse;

DROP TABLE IF EXISTS warehouse_products;

CREATE TABLE IF NOT EXISTS warehouse_products (
    product_id UUID PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(1000),
    weight DECIMAL(19,2) NOT NULL,
    width DECIMAL(19,2) NOT NULL,
    height DECIMAL(19,2) NOT NULL,
    depth DECIMAL(19,2) NOT NULL,
    fragile BOOLEAN DEFAULT FALSE,
    quantity INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_warehouse_products_quantity ON warehouse_products(quantity);
