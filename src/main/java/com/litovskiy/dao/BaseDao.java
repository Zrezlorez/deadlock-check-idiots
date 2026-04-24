package com.litovskiy.dao;

import com.litovskiy.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Function;

public abstract class BaseDao {

    protected <T> T execute(Function<Session, T> action) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            T result = action.apply(session);

            tx.commit();
            return result;

        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;

        } finally {
            session.close();
        }
    }

    protected void executeVoid(java.util.function.Consumer<Session> action) {
        execute(session -> {
            action.accept(session);
            return null;
        });
    }
}