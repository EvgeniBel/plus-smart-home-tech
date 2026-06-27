-- =============================================
-- V1__init_schema.sql
-- Схема базы данных для сервиса Analyzer
-- =============================================

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

-- 3. Таблица условий (отдельная сущность)
CREATE TABLE IF NOT EXISTS conditions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    value INTEGER
);

-- 4. Таблица действий (отдельная сущность)
CREATE TABLE IF NOT EXISTS actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    value INTEGER
);

-- 5. Связующая таблица сценарий-датчик-условие
CREATE TABLE IF NOT EXISTS scenario_conditions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    sensor_id VARCHAR(255) NOT NULL,
    condition_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE,
    FOREIGN KEY (sensor_id) REFERENCES sensors(id) ON DELETE CASCADE,
    FOREIGN KEY (condition_id) REFERENCES conditions(id) ON DELETE CASCADE,
    UNIQUE(scenario_id, sensor_id, condition_id)
);

-- 6. Связующая таблица сценарий-датчик-действие
CREATE TABLE IF NOT EXISTS scenario_actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    sensor_id VARCHAR(255) NOT NULL,
    action_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE,
    FOREIGN KEY (sensor_id) REFERENCES sensors(id) ON DELETE CASCADE,
    FOREIGN KEY (action_id) REFERENCES actions(id) ON DELETE CASCADE,
    UNIQUE(scenario_id, sensor_id, action_id)
);

-- =============================================
-- Индексы для оптимизации
-- =============================================

CREATE INDEX idx_scenarios_hub_id ON scenarios(hub_id);
CREATE INDEX idx_scenarios_name ON scenarios(name);
CREATE INDEX idx_sensors_hub_id ON sensors(hub_id);
CREATE INDEX idx_scenario_conditions_scenario_id ON scenario_conditions(scenario_id);
CREATE INDEX idx_scenario_conditions_sensor_id ON scenario_conditions(sensor_id);
CREATE INDEX idx_scenario_conditions_condition_id ON scenario_conditions(condition_id);
CREATE INDEX idx_scenario_actions_scenario_id ON scenario_actions(scenario_id);
CREATE INDEX idx_scenario_actions_sensor_id ON scenario_actions(sensor_id);
CREATE INDEX idx_scenario_actions_action_id ON scenario_actions(action_id);

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
COMMENT ON TABLE conditions IS 'Условия для сценариев';
COMMENT ON TABLE actions IS 'Действия для сценариев';
COMMENT ON TABLE scenario_conditions IS 'Связь сценариев с условиями и датчиками';
COMMENT ON TABLE scenario_actions IS 'Связь сценариев с действиями и датчиками';