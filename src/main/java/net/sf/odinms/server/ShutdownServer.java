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
package net.sf.odinms.server;

import java.rmi.RemoteException;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.ChannelManager;
import net.sf.odinms.net.channel.ChannelServer;
import org.apache.log4j.Logger;

/**
 *
 * @author Frz
 */
public class ShutdownServer implements Runnable {

    private static Logger log = Logger.getLogger(ShutdownServer.class);
    private ChannelDescriptor myChannel;

    public ShutdownServer(ChannelDescriptor channel) {
        myChannel = channel;
    }

    @Override
    public void run() {
        try {
            ChannelServer.getInstance(myChannel).shutdown();
        } catch (Throwable t) {
            log.error("SHUTDOWN ERROR", t);
        }

        int c = 200;
        while (ChannelServer.getInstance(myChannel).getConnectedClients() > 0 && c > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("ERROR", e);
            }
            c--;
        }
        try {
            ChannelServer.getWorldRegistry().deregisterChannelServer(myChannel);
        } catch (RemoteException e) {
            ServerExceptionHandler.HandlerRemoteException(e);
            // we are shutting down
        }
        try {
            ChannelServer.getInstance(myChannel).unbind();
        } catch (Throwable t) {
            log.error("SHUTDOWN ERROR", t);
        }

        boolean allShutdownFinished = true;
        for (ChannelServer cserv : ChannelManager.getChannelServers(myChannel.getWorld())) {
            if (!cserv.hasFinishedShutdown()) {
                allShutdownFinished = false;
            }
        }
    }
}