-- Создание базы данных (если не существует)
CREATE DATABASE IF NOT EXISTS warehouse;

-- Подключение к базе
\c warehouse;

-- ============================================
-- Создание таблицы товаров на складе
-- ============================================

CREATE TABLE IF NOT EXISTS warehouse_products (
    product_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    weight DECIMAL(19,2) NOT NULL CHECK (weight > 0),
    width DECIMAL(19,2) NOT NULL CHECK (width > 0),
    height DECIMAL(19,2) NOT NULL CHECK (height > 0),
    depth DECIMAL(19,2) NOT NULL CHECK (depth > 0),
    fragile BOOLEAN NOT NULL DEFAULT FALSE,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0)
);

-- ============================================
-- Индексы для оптимизации
-- ============================================

CREATE INDEX IF NOT EXISTS idx_warehouse_products_quantity ON warehouse_products(quantity);
CREATE INDEX IF NOT EXISTS idx_warehouse_products_name ON warehouse_products(name);
CREATE INDEX IF NOT EXISTS idx_warehouse_products_fragile ON warehouse_products(fragile);
