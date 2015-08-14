/*
 公会操作处理
 */
package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;
import java.util.Iterator;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.net.world.guild.*;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.Guild.MapleGuild_Msg;

public class GuildOperationHandler extends AbstractMaplePacketHandler {

    private boolean isGuildNameAcceptable(String name) {
        if (name.getBytes().length < 3 || name.getBytes().length > 12) {
            return false;
        }
        return true;
    }

    private void respawnPlayer(MapleCharacter mc) {
        mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(mc.getId()), false);
        mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
        if (mc.getNoPets() > 0) {
            for (MaplePet pet : mc.getPets()) {
                if (pet != null) {
                    mc.getMap().broadcastMessage(mc, MaplePacketCreator.showPet(mc, pet, false, false), false);
                }
            }
        }
    }

    private class Invited {

        public String name;
        public int gid;
        public long expiration;

        public Invited(String n, int id) {
            name = n.toLowerCase();
            gid = id;
            expiration = System.currentTimeMillis() + 60 * 60 * 1000; // 1 hr expiration
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Invited)) {
                return false;
            }
            Invited oth = (Invited) other;
            return (gid == oth.gid && name.equals(oth));
        }
    }
    private org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
    private java.util.List<Invited> invited = new java.util.LinkedList<Invited>();
    private long nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (System.currentTimeMillis() >= nextPruneTime) {
            Iterator<Invited> itr = invited.iterator();
            Invited inv;
            while (itr.hasNext()) {
                inv = itr.next();
                if (System.currentTimeMillis() >= inv.expiration) {
                    itr.remove();
                }
            }
            nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;
        }
        MapleCharacter mc = c.getPlayer();
        byte fengb = slea.readByte();
        switch (fengb) {
            case 0x02: // 创建家族
                if (c.getPlayer().getGuildid() > 0) {
                    c.getPlayer().dropMessage(1, "你已经有家族了,不能创建家族。");
                    return;
                } else if (c.getPlayer().getMeso() < 5000000) {
                    c.getPlayer().dropMessage(1, "金币不足。");
                    return;
                }
                final String guildName = slea.readMapleAsciiString();

                if (!isGuildNameAcceptable(guildName)) {
                    c.getPlayer().dropMessage(1, "你不能使用这个名字。");
                    return;
                }
                int guildId;

                try {
                    guildId = c.getChannelServer().getWorldInterface().createGuild(c.getPlayer().getId(), guildName);
                } catch (RemoteException re) {
                    ServerExceptionHandler.HandlerRemoteException(re);
                    c.getPlayer().dropMessage(5, "系统错误，请稍后再试。");
                    return;
                }
                if (guildId == -1) {
                    c.getPlayer().dropMessage(1, "你不能使用这个名字。");
                    return;
                }
                if (guildId == 0) {
                    c.getSession().write(MapleGuild_Msg.genericGuildMessage((byte) 0x1c));//提示无法处理。发生未知错误无法处理有关家族的邀请
                    return;
                }
                c.getPlayer().gainMeso(-5000000, true, false, true);
                c.getPlayer().setGuildId(guildId);
                c.getPlayer().setGuildRank(1);
                c.getPlayer().saveGuildStatus();
                c.getSession().write(MaplePacketCreator.serverNotice(1, "创建家族成功."));
                c.getSession().write(MapleGuild_Msg.CreateGuild(c.getPlayer()));
                c.getPlayer().getGuild().gainGP(500);
                c.getSession().write(MapleGuild_Msg.更新家族GP和信息(c.getPlayer()));
                respawnPlayer(c.getPlayer());
                break;
            case 0x05: // 家族邀请
                if (c.getPlayer().getGuildid() <= 0 || c.getPlayer().getGuildrank() > 2) { // 1 == guild master, 2 == jr
                    return;
                }
                String name = slea.readMapleAsciiString();
                final MapleGuildResponse mgr = MapleGuild.sendInvite(c, name);

                if (mgr != null && mgr.equals(MapleGuildResponse.NOT_IN_CHANNEL)) {
                    c.SendPacket(MaplePacketCreator.enableActions());
                    return;
                }

                if (mgr != null) {
                    c.getPlayer().getGuild().gainGP(500);
                    c.getSession().write(mgr.getPacket());
                } else {
                    Invited inv = new Invited(name, c.getPlayer().getGuildid());
                    if (!invited.contains(inv)) {
                        invited.add(inv);
                    }
                }
                break;
            case 0x06: // 接受家族邀请
                if (c.getPlayer().getGuildid() > 0) {
                    return;
                }
                guildId = slea.readInt();
                int cid = slea.readInt();

                if (cid != c.getPlayer().getId()) {
                    return;
                }
                name = c.getPlayer().getName().toLowerCase();
                Iterator<Invited> itr = invited.iterator();

                while (itr.hasNext()) {
                    Invited inv = itr.next();
                    if (guildId == inv.gid && name.equals(inv.name)) {
                        c.getPlayer().setGuildId(guildId);
                        c.getPlayer().setGuildRank(5);
                        itr.remove();

                        int s;

                        try {
                            s = c.getChannelServer().getWorldInterface().addGuildMember(c.getPlayer().getMGC());
                        } catch (RemoteException e) {
                            System.err.println("RemoteException occurred while attempting to add character to guild" + e);
                            ServerExceptionHandler.HandlerRemoteException(e);
                            c.getPlayer().dropMessage(5, "系统错误，请稍后再试。");
                            c.getPlayer().setGuildId(0);
                            return;
                        }
                        if (s == 0) {
                            c.getPlayer().dropMessage(1, "尝试加入的家族成员数已到达最高限制。");
                            c.getPlayer().setGuildId(0);
                            return;
                        }
                        c.getSession().write(MapleGuild_Msg.showGuildInfo(c.getPlayer()));
                        c.getPlayer().saveGuildStatus();
                        respawnPlayer(c.getPlayer());
                        break;
                    }
                }
                break;
            case 0x07: // 离开家族
                cid = slea.readInt();
                name = slea.readMapleAsciiString();

                if (cid != c.getPlayer().getId() || !name.equals(c.getPlayer().getName()) || c.getPlayer().getGuildid() <= 0) {
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().leaveGuild(c.getPlayer().getMGC());
                } catch (RemoteException re) {
                    System.err.println("RemoteException occurred while attempting to leave guild" + re);
                    ServerExceptionHandler.HandlerRemoteException(re);
                    c.getPlayer().dropMessage(5, "系统错误，请稍后再试。");
                    return;
                }
                c.getPlayer().getGuild().gainGP(-500);
                c.getSession().write(MapleGuild_Msg.showGuildInfo(null));
                c.getPlayer().setGuildId(0);
                c.getPlayer().saveGuildStatus();
                respawnPlayer(c.getPlayer());
                break;
            case 0x08: // 家族驱逐
                cid = slea.readInt();
                name = slea.readMapleAsciiString();

                if (c.getPlayer().getGuildrank() > 2 || c.getPlayer().getGuildid() <= 0) {
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().expelMember(c.getPlayer().getMGC(), name, cid);
                } catch (RemoteException re) {
                    System.err.println("RemoteException occurred while attempting to change rank" + re);
                    ServerExceptionHandler.HandlerRemoteException(re);
                    c.getPlayer().dropMessage(5, "系统错误，请稍后再试。");
                    return;
                }
                break;
            case 0x0e: // 家族等级职称修改
                if (c.getPlayer().getGuildid() <= 0 || c.getPlayer().getGuildrank() != 1) {
                    return;
                }
                String ranks[] = new String[5];
                for (int i = 0; i < 5; i++) {
                    ranks[i] = slea.readMapleAsciiString();
                }

                try {
                    c.getChannelServer().getWorldInterface().changeRankTitle(c.getPlayer().getGuildid(), ranks);
                } catch (RemoteException re) {
                    System.err.println("RemoteException occurred" + re);
                    ServerExceptionHandler.HandlerRemoteException(re);
                    c.getPlayer().dropMessage(5, "系统错误，请稍后再试。");
                    return;
                }
                break;
            case 0x0f: // 排名变化
                cid = slea.readInt();
                byte newRank = slea.readByte();

                if ((newRank <= 1 || newRank > 5) || c.getPlayer().getGuildrank() > 2 || (newRank <= 2 && c.getPlayer().getGuildrank() != 1) || c.getPlayer().getGuildid() <= 0) {
                    return;
                }

                try {
                    c.getChannelServer().getWorldInterface().changeRank(c.getPlayer().getGuildid(), cid, newRank);
                } catch (RemoteException re) {
                    System.err.println("RemoteException occurred while attempting to change rank" + re);
                    ServerExceptionHandler.HandlerRemoteException(re);
                    c.getPlayer().dropMessage(5, "系统错误，请稍后再试。");
                    return;
                }
                break;
            case 0x10: // 家族徽章修改
                if (c.getPlayer().getGuildid() <= 0 || c.getPlayer().getGuildrank() != 1) {
                    return;
                }

                if (c.getPlayer().getMeso() < 15000000) {
                    c.getPlayer().dropMessage(1, "金币不足。");
                    return;
                }
                final short bg = slea.readShort();
                final byte bgcolor = slea.readByte();
                final short logo = slea.readShort();
                final byte logocolor = slea.readByte();

                try {
                    c.getChannelServer().getWorldInterface().setGuildEmblem(c.getPlayer().getGuildid(), bg, bgcolor, logo, logocolor);
                } catch (RemoteException re) {
                    System.err.println("RemoteException occurred" + re);
                    ServerExceptionHandler.HandlerRemoteException(re);
                    c.getPlayer().dropMessage(5, "系统错误，请稍后再试。");
                    return;
                }

                c.getPlayer().gainMeso(-15000000, true, false, true);
                //respawnPlayer(c.getPlayer());
                c.getPlayer().getGuild().guildMessage(MapleGuild_Msg.家族徽章变更(c.getPlayer().getGuildid(), bg, bgcolor, logo, logocolor));
                break;
            case 0x1D:
                int skiild = slea.readInt();
                c.getPlayer().dropMessage(5, "家族技能暂时无法满.请期待!!");
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 0x11: // 家族公告修改
                final String notice = slea.readMapleAsciiString();
                if (notice.length() > 100) {
                    return;
                }
                try {
                    c.getChannelServer().getWorldInterface().setGuildNotice(c.getPlayer().getGuildid(), notice);
                } catch (RemoteException re) {
                    System.err.println("RemoteException occurred" + re);
                    ServerExceptionHandler.HandlerRemoteException(re);
                    c.getPlayer().dropMessage(5, "系统错误，请稍后再试。");
                    return;
                }
                break;
            case 0x1f://转移族长。
                int toid = slea.readInt();
                c.getPlayer().getGuild().setLeader(toid, c, c.getPlayer().getId());
                //c.getPlayer().getGuild().guildMessage(MapleGuild_Msg.ChangeLeader(c.getPlayer().getGuildId(), c.getPlayer().getId(), toid));
                break;
        }
    }
}
