CREATE TABLE app_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    enabled boolean NOT NULL DEFAULT true
);

CREATE TABLE app_role (
    id smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code text NOT NULL UNIQUE,
    display_name text NOT NULL
);

CREATE TABLE app_user_role (
    user_id uuid PRIMARY KEY REFERENCES app_user (id),
    role_id smallint NOT NULL REFERENCES app_role (id)
);

CREATE INDEX app_user_role_role_id_idx ON app_user_role (role_id);

INSERT INTO app_role (code, display_name) VALUES
    ('ADMIN', '管理员'),
    ('OPERATOR', '操作员'),
    ('VIEWER', '查看者');
