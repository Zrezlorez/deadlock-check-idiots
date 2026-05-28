create table if not exists action_log (
    id bigserial primary key,
    actor_id bigint,
    target_id bigint,
    action varchar(64) not null,
    old_actor_size double precision,
    new_actor_size double precision,
    old_target_size double precision,
    new_target_size double precision,
    metadata JSONB,
    created_at TIMESTAMPTZ not null default now()
);

create table if not exists action_log_tags (
     action_log_id bigint not null,
     tag varchar(64) not null,

     constraint fk_action_log_tags_action_log
         foreign key (action_log_id)
             references action_log (id)
             on delete cascade,

     constraint uk_action_log_tags_log_tag
         unique (action_log_id, tag)
);

create table if not exists player_behavior_stats (
    id bigserial primary key,
    player_id bigint not null,
    aggressive_actions integer not null default 0,
    support_actions integer not null default 0,
    risk_actions integer not null default 0,
    defensive_actions integer not null default 0,
    archetype varchar(64),

    last_ability_action varchar(64),
    same_ability_streak integer,

    constraint uk_player_id
        unique (player_id)
);

create table if not exists player_growth_stats (
     id bigserial primary key,

     player_id bigint not null,

     average_growth double precision not null default 0,

     current_lucky_streak integer not null default 0,
     current_fail_streak integer not null default 0,
     current_normal_streak integer not null default 0,

     max_lucky_streak integer not null default 0,
     max_fail_streak integer not null default 0,
     max_normal_streak integer not null default 0,

     total_crits integer not null default 0,
     total_fails integer not null default 0,
     total_normal_growths integer not null default 0,

     updated_at timestamptz not null default now(),

     constraint uk_player_growth_stats_player
         unique (player_id)
);