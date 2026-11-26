CREATE TABLE app_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(191) COLLATE utf8mb4_0900_as_cs NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY app_user_username_uk (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE app_role (
    id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY app_role_code_uk (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE app_user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id),
    KEY app_user_role_role_id_idx (role_id),
    CONSTRAINT app_user_role_user_fk FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT app_user_role_role_fk FOREIGN KEY (role_id) REFERENCES app_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO app_role (code, display_name) VALUES
    ('ADMIN', '管理员'),
    ('OPERATOR', '操作员'),
    ('VIEWER', '查看者');
