update app_settings set setting_value = 'fuck_cost_percent' where setting_value = 'enemy_fail_cost_percent';
update app_settings set setting_value = 'enemy_fail_chance_penalty' where setting_value = 'fuck_fail_chance_penalty';
update app_settings set setting_value = 'enemy_growth_penalty' where setting_value = 'slow_growth_penalty';
update app_settings set setting_value = 'self_fail_chance_penalty' where setting_value = 'jackpot_fail_chance';
update app_settings set setting_value = 'self_crit_chance_bonus' where setting_value = 'jackpot_crit_chance';
update app_settings set setting_value = 'self_fail_bonus' where setting_value = 'pray_fail_bonus';
update app_settings set setting_value = 'self_growth_bonus' where setting_value = 'turtle_growth_bonus';
update app_settings set setting_value = 'max_pending_fail_chance_penalty' where setting_value = 'max_pending_fail_chance';
update app_settings set setting_value = 'max_pending_crit_chance_bonus' where setting_value = 'max_pending_crit_chance';
update app_settings set setting_value = 'max_pending_growth_bonus' where setting_value = 'max_pending_growth';
delete from app_settings where setting_value = 'max_pending_growth_penalty'