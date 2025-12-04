CREATE SEQUENCE partner_id_seq
    START 201
    INCREMENT 1;

CREATE TABLE partners
(
    partner_id     TEXT PRIMARY KEY DEFAULT ('DSE' || nextval('partner_id_seq')),
    partner_name   varchar(255) not null,
    country        varchar(255) not null,
    region         varchar(255) not null,
    contact_person varchar(255) not null,
    contact_email  varchar(255) not null,
    contact_phone  varchar(20)  not null,
    is_active      boolean default true,
    created_at     timestamp    not null,
    updated_at     timestamp
);

CREATE TABLE centers
(
    center_id   uuid         NOT NULL PRIMARY KEY,
    partner_id  text         NOT NULL,
    center_name varchar(255) NOT NULL,
    location    varchar(255) NOT NULL,
    country     varchar(255) NOT NULL,
    region      varchar(255),
    is_active   boolean DEFAULT true,
    created_at  timestamp NOT NULL,
    updated_at  timestamp,

    CONSTRAINT centers_partners_fk
        FOREIGN KEY (partner_id)
            REFERENCES partners(partner_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE
);

CREATE TYPE user_role AS ENUM ('PARTNER', 'ME_OFFICER', 'FACILITATOR');

CREATE TABLE users
(
    user_id       uuid         NOT NULL PRIMARY KEY,
    email         varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role          user_role    NOT NULL,
    partner_id    text,
    center_id     uuid,
    first_name    varchar(100),
    last_name     varchar(100),
    is_active     boolean DEFAULT true,
    created_at    timestamp NOT NULL,
    updated_at    timestamp,

    CONSTRAINT users_partner_fk
        FOREIGN KEY (partner_id)
            REFERENCES partners(partner_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE,

    CONSTRAINT users_center_fk
        FOREIGN KEY (center_id)
            REFERENCES centers(center_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE
);
