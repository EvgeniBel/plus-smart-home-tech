-- 1. Таблица сценариев
CREATE TABLE IF NOT EXISTS scenarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hub_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(hub_id, name)
);

-- 2. Таблица датчиков
CREATE TABLE IF NOT EXISTS sensors (
    id VARCHAR(255) PRIMARY KEY,
    hub_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Таблица условий (связь сценарий-датчик-условие)
CREATE TABLE IF NOT EXISTS scenario_conditions (
    scenario_id BIGINT NOT NULL,
    sensor_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    value INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scenario_id, sensor_id),
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE,
    FOREIGN KEY (sensor_id) REFERENCES sensors(id) ON DELETE CASCADE
);

-- 4. Таблица действий (связь сценарий-датчик-действие)
CREATE TABLE IF NOT EXISTS scenario_actions (
    scenario_id BIGINT NOT NULL,
    sensor_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    value INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scenario_id, sensor_id),
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE,
    FOREIGN KEY (sensor_id) REFERENCES sensors(id) ON DELETE CASCADE
);

-- =============================================
-- Индексы для оптимизации
-- =============================================

CREATE INDEX idx_scenarios_hub_id ON scenarios(hub_id);
CREATE INDEX idx_sensors_hub_id ON sensors(hub_id);
CREATE INDEX idx_scenario_conditions_sensor_id ON scenario_conditions(sensor_id);
CREATE INDEX idx_scenario_actions_sensor_id ON scenario_actions(sensor_id);

-- =============================================
-- Триггер для обновления updated_at
-- =============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггеры для scenarios и sensors
CREATE TRIGGER tr_update_scenarios_updated_at
BEFORE UPDATE ON scenarios
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER tr_update_sensors_updated_at
BEFORE UPDATE ON sensors
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- Комментарии
-- =============================================

COMMENT ON TABLE scenarios IS 'Сценарии умного дома';
COMMENT ON TABLE sensors IS 'Датчики';
COMMENT ON TABLE scenario_conditions IS 'Условия сценариев';
COMMENT ON TABLE scenario_actions IS 'Действия сценариев';