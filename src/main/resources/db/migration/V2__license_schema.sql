create table if not exists product (
    id          bigserial primary key,
    name        varchar(255) not null,
    is_blocked  boolean      not null default false
);

create table if not exists license_type (
    id                       bigserial primary key,
    name                     varchar(255) not null,
    default_duration_in_days integer      not null,
    description              text
);

create table if not exists license (
    id                    bigserial primary key,
    code                  varchar(64)  not null unique,
    user_id               bigint       references users (id),
    product_id            bigint       not null references product (id),
    type_id               bigint       not null references license_type (id),
    first_activation_date date,
    ending_date           date,
    blocked               boolean      not null default false,
    device_count          integer      not null default 0,
    owner_id              bigint       references users (id),
    description           text
);

create index if not exists idx_license_code on license (code);
create index if not exists idx_license_user_id on license (user_id);
create index if not exists idx_license_product_id on license (product_id);
create index if not exists idx_license_type_id on license (type_id);
create index if not exists idx_license_owner_id on license (owner_id);

create table if not exists device (
    id           bigserial primary key,
    name         varchar(255),
    mac_address  varchar(64) not null unique,
    user_id      bigint      references users (id)
);

create table if not exists device_license (
    id              bigserial primary key,
    license_id      bigint      not null references license (id) on delete cascade,
    device_id       bigint      not null references device (id) on delete cascade,
    activation_date timestamp   not null,
    unique (license_id, device_id)
);

create index if not exists idx_device_license_license_id on device_license (license_id);
create index if not exists idx_device_license_device_id on device_license (device_id);

create table if not exists license_history (
    id          bigserial primary key,
    license_id  bigint      not null references license (id) on delete cascade,
    user_id     bigint      references users (id),
    status      varchar(50) not null,
    change_date timestamp   not null default now(),
    description text
);

create index if not exists idx_license_history_license_id on license_history (license_id);

