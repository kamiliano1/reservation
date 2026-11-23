CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_booking_times CHECK(end_time > start_time)
);
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE bookings
    ADD CONSTRAINT no_overlapping_bookings
    EXCLUDE USING gist (
        resource_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    )    
	WHERE (status IN ('PENDING', 'CONFIRMED'));