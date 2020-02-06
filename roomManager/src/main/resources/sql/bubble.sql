-- bubble 数据表 创建时间： 2019/11/08

create sequence match_info_id_seq start with 1000001;

create table match_info (
  id                    bigint          primary key default nextval('match_info_id_seq'),
  src_video_name        varchar(256)    not null,
  tst_video_name        varchar(256)    not null,
  similarity            float           not null,
  success               boolean         not null default false,
  src_video_duration    int             not null default 0,
  src_video_format      varchar(256)    not null default '',
  src_video_size        varchar(256)    not null default '',
  src_video_fps         float           not null default 0,
  tst_video_duration    int             not null default 0,
  tst_video_format      varchar(256)    not null default '',
  tst_video_size        varchar(256)    not null default '',
  tst_video_fps         float           not null default 0,
  create_time           bigint          not null,
  wrong_code            bigint          not null,
  wrong_message         varchar(256)    not null default ''
);

alter sequence match_info_id_seq owned by match_info.id;

alter table MATCH_INFO
	add SUBMIT_TIME BIGINT default -1 not null;

alter table MATCH_INFO
	add FINISH_TIME BIGINT default -1 not null;

alter table MATCH_INFO
	add WAIT_TIME BIGINT default -1 not null;

alter table MATCH_INFO
	add RUNNING_TIME BIGINT default -1 not null;

	alter table MATCH_INFO drop column SRC_VIDEO_DURATION;

alter table MATCH_INFO drop column SRC_VIDEO_FORMAT;

alter table MATCH_INFO drop column SRC_VIDEO_SIZE;

alter table MATCH_INFO drop column SRC_VIDEO_FPS;

alter table MATCH_INFO drop column TST_VIDEO_DURATION;

alter table MATCH_INFO drop column TST_VIDEO_FORMAT;

alter table MATCH_INFO drop column TST_VIDEO_SIZE;

alter table MATCH_INFO drop column TST_VIDEO_FPS;