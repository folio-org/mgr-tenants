create database tm_it;

create user tm_admin with password 'folio123';
grant connect on database tm_it to tm_admin;
grant all privileges on database tm_it to tm_admin;

create database kong_it;

create user kong_admin with password 'kong123';
grant connect on database kong_it to kong_admin;
grant all privileges on database kong_it to kong_admin;
