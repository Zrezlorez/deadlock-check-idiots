package com.litovskiy.service;

import com.litovskiy.dao.AppSettingDao;
import com.litovskiy.entity.AppSetting;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameConfigService {

    private final AppSettingDao appSettingDao;

    public GameConfigService(AppSettingDao appSettingDao) {
        this.appSettingDao = appSettingDao;
    }

    public double getDouble(GameSetting setting) {
        return Double.parseDouble(getRawValue(setting));
    }

    public int getInt(GameSetting setting) {
        return Integer.parseInt(getRawValue(setting));
    }

    public ChronoUnit getChronoUnit(GameSetting setting) {
        return ChronoUnit.valueOf(getRawValue(setting));
    }

    public String getRawValue(GameSetting setting) {
        AppSetting appSetting = appSettingDao.find(setting.key());
        return appSetting == null ? setting.defaultValue() : appSetting.getValue();
    }

    public void set(GameSetting setting, String rawValue) {
        String normalized = setting.normalize(rawValue);
        appSettingDao.save(new AppSetting(setting.key(), normalized));
    }

    public void reset(GameSetting setting) {
        AppSetting appSetting = appSettingDao.find(setting.key());
        if (appSetting != null) {
            appSettingDao.delete(appSetting);
        }
    }

    public Map<GameSetting, String> listEffectiveValues() {
        Map<GameSetting, String> result = new LinkedHashMap<>();
        Map<String, String> storedMap = toValueMap(appSettingDao.findAll());

        for (GameSetting setting : GameSetting.values()) {
            result.put(setting, storedMap.getOrDefault(setting.key(), setting.defaultValue()));
        }

        return result;
    }

    private Map<String, String> toValueMap(List<AppSetting> storedSettings) {
        Map<String, String> storedMap = new LinkedHashMap<>();
        for (AppSetting storedSetting : storedSettings) {
            storedMap.put(storedSetting.getKey(), storedSetting.getValue());
        }
        return storedMap;
    }
}
