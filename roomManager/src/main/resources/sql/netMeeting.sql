create sequence user_info_uid_seq start with 10001;

create table user_info (
  uid               bigint primary key default nextval('user_info_uid_seq'),
  user_name         varchar(100)  not null,
  account           varchar(100)  not null,
  password          varchar(100)  not null,
  head_img          varchar(256) not null default '',
  cover_img         varchar(256) not null default '',
  create_time       bigint        not null,
  rtmp_url          varchar(100)  not null
);

alter sequence user_info_uid_seq owned by user_info.uid;
alter table user_info drop constraint user_info_pkey;

alter table user_info
	add email varchar(128) default '' not null;