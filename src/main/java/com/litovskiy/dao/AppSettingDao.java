package com.litovskiy.dao;

import com.litovskiy.entity.AppSetting;

import java.util.List;

public class AppSettingDao extends BaseDao {

    public AppSetting find(String key) {
        return execute(session -> session.get(AppSetting.class, key));
    }

    public List<AppSetting> findAll() {
        return execute(session -> session.createQuery(
                "from AppSetting a order by a.key",
                AppSetting.class)
            .getResultList());
    }

    public void save(AppSetting appSetting) {
        executeVoid(session -> session.merge(appSetting));
    }

    public void delete(AppSetting appSetting) {
        executeVoid(session -> {
            AppSetting attached = session.contains(appSetting) ? appSetting : session.merge(appSetting);
            session.remove(attached);
        });
    }
}
