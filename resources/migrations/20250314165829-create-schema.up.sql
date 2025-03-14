--------------------------------------------------------------------------------
-- UUIDs

create extension if not exists "uuid-ossp";

--------------------------------------------------------------------------------
-- Updated timestamp

create or replace function set_updated_at()
  returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

--------------------------------------------------------------------------------
-- Users

create table users (
  id bigserial primary key,
  public_id uuid unique not null default uuid_generate_v4(),
  email text unique,
  name text,
  created_at timestamp with time zone default now(),
  updated_at timestamp with time zone
);

create index users_email
on users (email);

create trigger users_updated_trigger
  before update on users
  for each row
    execute procedure set_updated_at();

--------------------------------------------------------------------------------
-- Organizations

create table organizations (
  id bigserial primary key,
  name text not null,
  public_id uuid unique not null default uuid_generate_v4(),
  created_at timestamp with time zone default now(),
  updated_at timestamp with time zone
);

create trigger organizations_updated_trigger
  before update on organizations
  for each row
    execute procedure set_updated_at();

--------------------------------------------------------------------------------
-- Verified domains

create table verified_domains (
  id bigserial primary key,
  organization_id bigint not null references organizations(id) on delete cascade,
  domain text not null unique,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone
);

create index verified_domains_organization_id
on verified_domains (organization_id);

create trigger verified_domains_updated_trigger
  before update on verified_domains
  for each row
    execute procedure set_updated_at();

--------------------------------------------------------------------------------
-- Employments

create table employments (
  id bigserial primary key,
  organization_id bigint not null references organizations(id) on delete cascade,
  user_id bigint not null references users(id) on delete cascade,
  public_id uuid unique not null default uuid_generate_v4(),

  title text,

  created_at timestamp with time zone default now(),
  updated_at timestamp with time zone
);

create trigger employments_updated_trigger
  before update on employments
  for each row
    execute procedure set_updated_at();

--------------------------------------------------------------------------------
-- Groups

create table groups (
  id bigserial primary key,
  public_id uuid unique not null default uuid_generate_v4(),

  organization_id bigint not null references organizations (id) on delete cascade,

  name text,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone
);

create trigger groups_updated_trigger
  before update on groups
  for each row
    execute procedure set_updated_at();

create table group_memberships (
  id bigserial primary key,
  public_id uuid unique not null default uuid_generate_v4(),

  group_id bigint not null references groups (id) on delete cascade,
  employment_id bigint not null references employments (id) on delete cascade,
  created_at timestamp with time zone not null default now(),

  unique (group_id, employment_id)
);
