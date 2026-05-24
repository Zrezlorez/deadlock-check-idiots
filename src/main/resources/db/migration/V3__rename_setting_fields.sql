update app_settings
set setting_key = 'fuck_cost_percent'
where setting_key = 'enemy_fail_cost_percent';

update app_settings
set setting_key = 'fuck_fail_chance_penalty'
where setting_key = 'enemy_fail_chance_penalty';

update app_settings
set setting_key = 'slow_growth_penalty'
where setting_key = 'enemy_growth_penalty';

update app_settings
set setting_key = 'jackpot_fail_chance'
where setting_key = 'self_fail_chance_penalty';

update app_settings
set setting_key = 'jackpot_crit_chance'
where setting_key = 'self_crit_chance_bonus';

update app_settings
set setting_key = 'pray_fail_bonus'
where setting_key = 'self_fail_bonus';

update app_settings
set setting_key = 'turtle_growth_bonus'
where setting_key = 'self_growth_bonus';

update app_settings
set setting_key = 'max_pending_fail_chance'
where setting_key = 'max_pending_fail_chance_penalty';

update app_settings
set setting_key = 'max_pending_crit_chance'
where setting_key = 'max_pending_crit_chance_bonus';

update app_settings
set setting_key = 'max_pending_growth'
where setting_key = 'max_pending_growth_bonus';

delete from app_settings
where setting_key = 'max_pending_growth_penalty';