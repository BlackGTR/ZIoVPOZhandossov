create table if not exists users (
  id bigserial primary key,
  username varchar(100) not null unique,
  password_hash varchar(255) not null,
  role varchar(30) not null,
  enabled boolean not null default true
);

create table if not exists refresh_tokens (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  token_hash varchar(255) not null unique,
  expires_at timestamp not null,
  revoked boolean not null default false
);

create index if not exists idx_refresh_tokens_user_id on refresh_tokens(user_id);