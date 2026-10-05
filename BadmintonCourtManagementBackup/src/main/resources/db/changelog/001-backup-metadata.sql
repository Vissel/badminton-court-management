CREATE TABLE backup_trigger (
    trigger_id BIGINT NOT NULL AUTO_INCREMENT,
    trigger_source VARCHAR(20) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    schedule_type VARCHAR(20),
    external_request_id VARCHAR(100),
    from_cutoff TIMESTAMP(6) NULL,
    to_cutoff TIMESTAMP(6) NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    error_message VARCHAR(2000),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (trigger_id),
    INDEX idx_backup_trigger_created (created_at),
    INDEX idx_backup_trigger_status (status)
);

CREATE TABLE backup_file (
    file_id BIGINT NOT NULL AUTO_INCREMENT,
    trigger_id BIGINT NOT NULL,
    schema_name VARCHAR(100) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    row_count BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (file_id),
    CONSTRAINT backup_file_trigger_fk FOREIGN KEY (trigger_id) REFERENCES backup_trigger(trigger_id)
);

CREATE TABLE backup_watermark (
    watermark_id BIGINT NOT NULL AUTO_INCREMENT,
    schema_name VARCHAR(100) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    last_successful_cutoff TIMESTAMP(6) NULL,
    updated_at TIMESTAMP(6) NULL,
    PRIMARY KEY (watermark_id),
    CONSTRAINT uk_watermark_schema_table UNIQUE (schema_name, table_name)
);

CREATE TABLE backup_execution_lock (
    lock_id INT NOT NULL,
    locked BOOLEAN NOT NULL,
    trigger_id BIGINT NULL,
    PRIMARY KEY (lock_id)
);

INSERT INTO backup_execution_lock (lock_id, locked, trigger_id) VALUES (1, FALSE, NULL);

CREATE TABLE processed_backup_request (
    request_id VARCHAR(100) NOT NULL,
    trigger_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (request_id),
    CONSTRAINT uk_processed_trigger UNIQUE (trigger_id),
    CONSTRAINT processed_request_trigger_fk FOREIGN KEY (trigger_id) REFERENCES backup_trigger(trigger_id)
);
