CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGSERIAL NOT NULL,
    user_id VARCHAR(30) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_time CHECK(end_time > start_time)
)