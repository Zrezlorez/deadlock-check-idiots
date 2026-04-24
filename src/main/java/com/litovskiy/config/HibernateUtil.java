package com.litovskiy.config;

import com.litovskiy.entity.ActivityStat;
import com.litovskiy.entity.AppSetting;
import com.litovskiy.entity.ConversationSettings;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.LinkCode;
import com.litovskiy.entity.VoiceSession;
import lombok.Getter;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.Properties;

public class HibernateUtil {

    @Getter
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {

        Properties props = new Properties();

        props.put("hibernate.connection.datasource", DataSourceProvider.get());

        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.show_sql", "false");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySettings(props)
            .build();

        Metadata metadata = new MetadataSources(registry)
            .addAnnotatedClass(Player.class)
            .addAnnotatedClass(LinkCode.class)
            .addAnnotatedClass(ActivityStat.class)
            .addAnnotatedClass(VoiceSession.class)
            .addAnnotatedClass(ConversationSettings.class)
            .addAnnotatedClass(AppSetting.class)
            .getMetadataBuilder()
            .build();

        return metadata.getSessionFactoryBuilder().build();
    }

}
