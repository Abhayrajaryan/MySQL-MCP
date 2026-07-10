CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE database_connections (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    name                VARCHAR(255) NOT NULL,
    host                VARCHAR(255) NOT NULL,
    port                INT NOT NULL,
    database_name       VARCHAR(255) NOT NULL,
    db_username         VARCHAR(255) NOT NULL,
    encrypted_password  TEXT NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_db_connections_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE api_keys (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    database_connection_id   BIGINT NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    key_prefix               VARCHAR(255) NOT NULL,
    key_hash                 VARCHAR(255) NOT NULL,
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at               TIMESTAMP NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at             TIMESTAMP NULL,
    CONSTRAINT fk_api_keys_db_connection FOREIGN KEY (database_connection_id) REFERENCES database_connections(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE api_key_permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_key_id  BIGINT NOT NULL,
    permission  VARCHAR(50) NOT NULL,
    CONSTRAINT fk_permissions_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_key_id               BIGINT NULL,
    database_connection_id   BIGINT NULL,
    source_type              VARCHAR(20) NOT NULL,
    operation                VARCHAR(255) NOT NULL,
    request_payload          TEXT NULL,
    response_summary         TEXT NULL,
    success                  BOOLEAN NOT NULL,
    rows_affected            BIGINT NULL,
    execution_time_ms        BIGINT NOT NULL,
    error_code               VARCHAR(50) NULL,
    error_message            TEXT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys(id),
    CONSTRAINT fk_audit_db_connection FOREIGN KEY (database_connection_id) REFERENCES database_connections(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;