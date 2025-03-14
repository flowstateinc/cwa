drop table if exists group_memberships cascade;
drop table if exists groups cascade;
drop table if exists employments cascade;
drop table if exists verified_domains cascade;
drop table if exists organizations cascade;
drop table if exists users cascade;
drop function if exists set_updated_at() cascade;
drop extension if exists "uuid-ossp";
