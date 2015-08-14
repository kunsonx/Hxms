/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.SessionImpl;
import org.hibernate.stat.Statistics;
import org.logicalcobwebs.proxool.ProxoolFacade;

/**
 * All OdinMS servers maintain a Database Connection. This class therefore
 * "singletonices" the connection per process.
 *
 *
 * @author Frz
 */
public class DatabaseConnection {

    private final static org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(DatabaseConnection.class);
    private final static java.util.concurrent.ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<Integer, ReentrantLock>();
    private static SessionFactory sessionFactory;
    private static Field declaredField;

    static {
        try {
            sessionFactory = new Configuration().configure()
                    .buildSessionFactory();
            //JAXPConfigurator.configure("db.xml", false);
            ProxoolFacade.disableShutdownHook();
            declaredField = SessionImpl.class.getDeclaredField("interceptor");
            declaredField.setAccessible(true);
        } catch (Exception ex) {
            log.error("连接池初始化错误。");
            ex.printStackTrace();
        }
    }

    private DatabaseConnection() {
    }

    public static ReentrantLock GetLock(int accountidString) {
        synchronized (locks) {
            if (!locks.containsKey(accountidString)) {
                locks.put(accountidString, new ReentrantLock());
            }
        }
        return locks.get(accountidString);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("proxool.odinms");
    }

    public static Connection getConnection(String s) throws SQLException {
        return DriverManager.getConnection(s);
    }

    public static Session getSession() {
        return sessionFactory.openSession();
    }

    public static Session getSession(EmptyInterceptor emptyinterceptor) throws Exception {
        Session session = getSession();
        declaredField.set(session, emptyinterceptor);
        return session;
    }

    public static void getDebugInfo() {
        log.info(sessionFactory.getStatistics().getSecondLevelCacheHitCount());
    }

    public static Statistics getStatistics() {
        return sessionFactory.getStatistics();
    }
}