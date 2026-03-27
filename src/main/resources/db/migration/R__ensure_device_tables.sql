-- Повторяемая миграция: подстраховка, если в versioned-скриптах на репозитории не оказалось device/device_license.
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
