package com.litovskiy.repository;

import com.litovskiy.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
    AppSetting getByKey(String key);

}
