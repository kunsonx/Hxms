/*
 更换频道处理程序
 */
package net.sf.odinms.net.channel.handler;

import java.net.InetAddress;
import java.rmi.RemoteException;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.server.miniGame.MapleRPSGame;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ChangeChannelHandler extends AbstractMaplePacketHandler {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChangeChannelHandler.class);

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int channel = slea.readByte() + 1;
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        String[] socket = c.getChannelServer().getIP(channel).split(":");

        if (c.getPlayer().getTrade() != null) {
            MapleTrade.cancelTrade(c.getPlayer());
        }
        MapleRPSGame.cancelRPSGame(c.getPlayer());
        c.getPlayer().cancelMagicDoor();
        /* if (c.getPlayer().getBuffedValue(MapleBuffStat.PUPPET) != null) {
         c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
         }*/

        /*if (c.getPlayer().getBuffedValue(MapleBuffStat.SUMMON) != null) {
         c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
         if (c.getPlayer().getSkillLevel(1320009) > 0) {
         for (int i = 0; i < 5; i++) {
         c.getSession().write(MaplePacketCreator.cancelBuff(null, 2022125 + i));
         }
         }
         }*/

        if (c.getPlayer().isBanned()) {
            c.getPlayer().dropMessage("你已被游戏管理员禁止任何操作。");
            return;
        }
        if (!c.getPlayer().getDiseases().isEmpty()) {
            c.getPlayer().cancelAllDebuffs();
        }
        try {
            c.getChannelServer().getWorldInterface().addBuffsToStorage(c.getPlayer().getId(), c.getPlayer().getAllBuffs());
        } catch (RemoteException e) {
            ServerExceptionHandler.HandlerRemoteException(e);
            c.getChannelServer().reconnectWorld();
        }
        c.getPlayer().saveToDB(true);
        if (c.getPlayer().getCheatTracker() != null) {
            c.getPlayer().getCheatTracker().dispose();
        }
        if (c.getPlayer().getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(c.getPlayer());
            try {
                c.getChannelServer().getWorldInterface().silentLeaveMessenger(c.getPlayer().getMessenger().getId(), messengerplayer);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                c.getChannelServer().reconnectWorld();
            }
        }
        c.getPlayer().getMap().removePlayer(c.getPlayer());
        c.getChannelServer().removePlayer(c.getPlayer());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
        try {
            c.getSession().write(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
