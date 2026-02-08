CREATE TABLE click_events
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    short_code VARCHAR(20) NOT NULL,
    user_id    VARCHAR(36),
    clicked_at TIMESTAMP   NOT NULL,
    ip         VARCHAR(45) NOT NULL,
    user_agent VARCHAR(512),
    referrer   VARCHAR(2048),
    event_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_click_events PRIMARY KEY (id),
    CONSTRAINT uk_click_events_hash UNIQUE (event_hash)
);

CREATE INDEX idx_click_short_code ON click_events (short_code);
CREATE INDEX idx_click_user_id ON click_events (user_id);
CREATE INDEX idx_click_timestamp ON click_events (clicked_at);
CREATE INDEX idx_click_short_code_date ON click_events (short_code, clicked_at);
