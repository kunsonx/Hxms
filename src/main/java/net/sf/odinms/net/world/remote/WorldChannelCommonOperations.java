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
package net.sf.odinms.net.world.remote;

import java.rmi.RemoteException;
import java.util.List;

public interface WorldChannelCommonOperations {

    public boolean isConnected(int chrId) throws RemoteException;

    public boolean isConnected(String chrName) throws RemoteException;

    public void broadcastMessage(String sender, byte[] message) throws RemoteException;

    /**
     * 发送悄悄话
     *
     * @param sender 发送者角色名
     * @param target 接收者角色
     * @param channel 频道
     * @param message 消息
     * @return 0 - 失败 1 - 成功
     * @throws RemoteException
     */
    public int whisper(String sender, String target, int channel, String message, boolean gm) throws RemoteException;

    public void shutdown(int time) throws RemoteException;

    public void broadcastWorldMessage(String message) throws RemoteException;

    public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException;

    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException;

    public List<CheaterData> getCheaters() throws RemoteException;

    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException;

    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException;

    public void spouseChat(String sender, String target, String message) throws RemoteException;

    public void broadcastGMMessage(String sender, byte[] message) throws RemoteException;

    public void deregisterOfflinePlayer(int cid) throws RemoteException;
}