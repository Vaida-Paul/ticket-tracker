CREATE TABLE notification_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    digest_date DATE NOT NULL,
    batch_id VARCHAR(64) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_log_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_log_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT uk_notification_log_user_ticket_date
        UNIQUE (user_id, ticket_id, digest_date)
);

CREATE INDEX idx_notification_log_digest_date ON notification_log(digest_date);
CREATE INDEX idx_notification_log_batch_id ON notification_log(batch_id);