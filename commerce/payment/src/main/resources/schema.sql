-- Создание базы данных
CREATE DATABASE IF NOT EXISTS payment;

-- Подключение к базе
\c payment;

-- Создание таблицы платежей
CREATE TABLE IF NOT EXISTS payments (
    payment_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_total DECIMAL(19,2) NOT NULL,
    delivery_total DECIMAL(19,2) NOT NULL,
    fee_total DECIMAL(19,2) NOT NULL,
    total_payment DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Индексы для оптимизации
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);