package com.litovskiy.config;

import com.litovskiy.entity.GrowthStyle;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class SchemaMigration {

    private static final String CONSTRAINT_NAME = "conversation_settings_growth_style_check";

    private SchemaMigration() {
    }

    static void apply(SessionFactory sessionFactory) {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("alter table if exists conversation_settings drop constraint if exists "
                        + CONSTRAINT_NAME);
                    statement.execute(buildConversationSettingsGrowthStyleConstraintSql());
                }
            });
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
    }

    public static String buildConversationSettingsGrowthStyleConstraintSql() {
        String allowedValues = Arrays.stream(GrowthStyle.values())
            .map(GrowthStyle::name)
            .map(value -> "'" + value + "'")
            .collect(Collectors.joining(", "));

        return "alter table if exists conversation_settings add constraint " + CONSTRAINT_NAME
            + " check (growth_style in (" + allowedValues + "))";
    }
}
