-- Fix notifications table if it exists without proper constraints
DO $$
BEGIN
    -- Only create table if it doesn't exist
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'notifications') THEN
        CREATE TABLE notifications
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
    END IF;

    -- Add constraints if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'notifications_type_check') THEN
        ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (notification_type IN ('ALERT', 'REMINDER', 'APPROVAL_REQUEST', 'INFO'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'notifications_priority_check') THEN
        ALTER TABLE notifications ADD CONSTRAINT notifications_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'notifications_recipient_fk') THEN
        ALTER TABLE notifications ADD CONSTRAINT notifications_recipient_fk FOREIGN KEY (recipient_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'notifications_pk') THEN
        ALTER TABLE notifications ADD CONSTRAINT notifications_pk PRIMARY KEY (notification_id);
    END IF;

    -- Add role_request_id column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'notifications' AND column_name = 'role_request_id') THEN
        ALTER TABLE notifications ADD COLUMN role_request_id UUID;
    END IF;
END $$;