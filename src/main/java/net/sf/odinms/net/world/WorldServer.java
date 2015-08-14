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
package net.sf.odinms.net.world;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.servlet.GeneralServer;
import net.sf.odinms.net.servlet.GeneralServerType;
import net.sf.odinms.net.servlet.WorldServerConfig;
import net.sf.odinms.server.ServerExceptionHandler;

/**
 *
 * @author Matze
 */
public class WorldServer extends GeneralServer {

    //日志
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WorldServer.class);
    //单一实例
    private static WorldServer instance = null;

    private WorldServer() {
        super(new WorldServerConfig());
        try {
            DatabaseConnection.getConnection().close();
        } catch (Exception e) {
            log.error("Could not configuration", e);
        }
    }

    public synchronized static WorldServer getInstance() {
        if (instance == null) {
            instance = new WorldServer();
        }
        return instance;
    }


    public static void main(String[] args) {
        try {
            if (CheckFilePermit()) {
                return;
            }
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT,
                    new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
            registry.rebind("WorldRegistry", WorldRegistryImpl.getInstance());
            log.info("世界服务器 已上线.");
        } catch (RemoteException ex) {
            ServerExceptionHandler.HandlerRemoteException(ex);
            log.error("不能初始化 RMI 系统", ex);
        }
    }

    @Override
    public GeneralServerType getServerType() {
        return GeneralServerType.WORLD;
    }

    @Override
    public final WorldServerConfig getConfig() {
        return (WorldServerConfig) _Config;
    }
}
