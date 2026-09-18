-- Section 1: User Management.
-- This schema is a contract other services read. `papers.owner_id` and
-- `papers.folder_id` in Storage Management are bare references into these
-- tables - deliberately NOT enforced FKs, since they live in another schema.

-- "users", not "user": `user` is a reserved word in Postgres.
create table users (
    id            uuid primary key default gen_random_uuid(),
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at    timestamptz  not null default now()
);

create table folders (
    id         uuid primary key default gen_random_uuid(),
    owner_id   uuid         not null references users (id) on delete cascade,
    name       varchar(255) not null,
    created_at timestamptz  not null default now()
);

create index idx_folders_owner on folders (owner_id);

-- One folder name per user, case-insensitively.
create unique index uq_folders_owner_name on folders (owner_id, lower(name));
