CREATE TABLE blade_inspection (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES app_user (id),
    farm_name text NOT NULL,
    turbine_number text NOT NULL,
    original_name text NOT NULL,
    status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    attempt_id uuid,
    failure_message text,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX blade_inspection_owner_created ON blade_inspection (owner_id, created_at DESC, id DESC);
CREATE INDEX blade_inspection_created ON blade_inspection (created_at DESC, id DESC);
CREATE INDEX blade_inspection_pending ON blade_inspection (created_at, id) WHERE status = 'PENDING';
