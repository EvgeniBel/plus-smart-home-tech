-- Создание базы данных
CREATE DATABASE IF NOT EXISTS delivery;

-- Подключение к базе
\c delivery;

-- Создание таблицы доставок
CREATE TABLE IF NOT EXISTS deliveries (
    delivery_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    from_country VARCHAR(100) NOT NULL,
    from_city VARCHAR(100) NOT NULL,
    from_street VARCHAR(200) NOT NULL,
    from_house VARCHAR(20) NOT NULL,
    from_flat VARCHAR(20),
    to_country VARCHAR(100) NOT NULL,
    to_city VARCHAR(100) NOT NULL,
    to_street VARCHAR(200) NOT NULL,
    to_house VARCHAR(20) NOT NULL,
    to_flat VARCHAR(20),
    delivery_state VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Индексы для оптимизации
CREATE INDEX idx_deliveries_order_id ON deliveries(order_id);
CREATE INDEX idx_deliveries_delivery_state ON deliveries(delivery_state);