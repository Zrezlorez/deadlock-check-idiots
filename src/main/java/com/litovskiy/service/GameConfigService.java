package com.litovskiy.service;

import com.litovskiy.repository.AppSettingRepository;
import com.litovskiy.entity.AppSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameConfigService {

    private final AppSettingRepository appSettingRepository;

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
        AppSetting appSetting = appSettingRepository.getByKey(setting.getKey());
        return appSetting == null ? setting.getDefaultValue() : appSetting.getValue();
    }

    public void set(GameSetting setting, String rawValue) {
        String normalized = setting.normalize(rawValue);
        appSettingRepository.save(new AppSetting(setting.getKey(), normalized));
    }

    public void reset(GameSetting setting) {
        AppSetting appSetting = appSettingRepository.getByKey(setting.getKey());
        if (appSetting != null) {
            appSettingRepository.delete(appSetting);
        }
    }

    public Map<GameSetting, String> listEffectiveValues() {
        Map<GameSetting, String> result = new LinkedHashMap<>();
        Map<String, String> storedMap = toValueMap(appSettingRepository.findAll());

        for (GameSetting setting : GameSetting.values()) {
            result.put(setting, storedMap.getOrDefault(setting.getKey(), setting.getDefaultValue()));
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
