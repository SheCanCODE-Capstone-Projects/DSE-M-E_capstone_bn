create table notifications
(
    notification_id   uuid         not null,
    recipient_id      uuid         not null,
    notification_type varchar(255) default 'INFO',
    title             varchar(255) not null,
    message           text         not null,
    is_read           boolean      default false,
    priority          varchar(255) default 'HIGH',
    created_at        timestamp    not null default now()
);

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
        CHECK (notification_type IN ('ALERT', 'REMINDER', 'APPROVAL_REQUEST', 'INFO'));

ALTER TABLE notifications
    ADD CONSTRAINT notifications_priority_check
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'));

ALTER TABLE notifications
    ADD CONSTRAINT notifications_recipient_fk
        FOREIGN KEY (recipient_id)
            REFERENCES users(user_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE;

alter table notifications
    add constraint notifications_pk
        primary key (notification_id);

ALTER TABLE notifications
    ADD COLUMN role_request_id UUID;
