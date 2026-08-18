-- Migration V12: Add user_notifications and user_push_subscriptions tables.

CREATE TABLE user_notifications (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    message text NOT NULL,
    priority character varying(20) NOT NULL DEFAULT 'NORMAL',
    is_read boolean NOT NULL DEFAULT false,
    action_type character varying(20) NOT NULL DEFAULT 'NAVIGATE',
    action_url character varying(2048) NULL,
    action_label character varying(255) NULL,
    metadata text NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_notifications_pkey PRIMARY KEY (id),
    CONSTRAINT user_notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_notifications_user_is_read ON user_notifications(user_id, is_read);
CREATE INDEX idx_user_notifications_user_created_at ON user_notifications(user_id, created_at DESC);

CREATE TABLE user_push_subscriptions (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    endpoint character varying(2048) NOT NULL,
    p256dh character varying(255) NOT NULL,
    auth character varying(255) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_push_subscriptions_pkey PRIMARY KEY (id),
    CONSTRAINT user_push_subscriptions_endpoint_key UNIQUE (endpoint),
    CONSTRAINT user_push_subscriptions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_push_subscriptions_user_id ON user_push_subscriptions(user_id);
