alter table players add column if not exists status varchar(64);
alter table players add column if not exists status_until timestamptz;

alter table player_growth_stats add column if not exists average_growth double precision;

alter table player_behavior_stats add column if not exists last_ability_action varchar(64);
alter table player_behavior_stats add column if not exists same_ability_streak integer;

alter table player_behavior_stats drop column if exists crits;
alter table player_behavior_stats drop column if exists fails;
alter table player_behavior_stats drop column if exists chat_id;
alter table player_behavior_stats drop column if exists average_growth;

alter table player_behavior_stats add constraint uk_player_id unique (player_id);
