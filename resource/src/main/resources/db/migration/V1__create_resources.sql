CREATE TABLE resources
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100)   NOT NULL,
    type         VARCHAR(30)    NOT NULL,
    hourly_price NUMERIC(10, 2) NOT NULL CHECK (hourly_price >= 0),
    open_time    TIME           NOT NULL,
    close_time   TIME           NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hours CHECK (close_time > open_time)
);