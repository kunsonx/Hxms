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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.login.remote.LoginWorldInterface;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.net.world.remote.WorldLoginInterface;
import net.sf.odinms.net.world.remote.WorldRegistry;
import net.sf.odinms.server.ServerExceptionHandler;

/**
 * 世界实例
 *
 * @author Matze
 */
public class WorldRegistryImpl extends UnicastRemoteObject implements WorldRegistry {

    private static final long serialVersionUID = -5170574938159851746L;
    private static WorldRegistryImpl instance = null;
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WorldRegistryImpl.class);
    /*
     * 成员变量
     */
    private ChannelServerStorage channelServerStorage = new ChannelServerStorage();
    private List<LoginWorldInterface> loginServer = new LinkedList<LoginWorldInterface>();
    private Map<Integer, MapleParty> parties = new HashMap<Integer, MapleParty>();
    private AtomicInteger runningPartyId = new AtomicInteger();
    private Map<Integer, MapleMessenger> messengers = new HashMap<Integer, MapleMessenger>();
    private AtomicInteger runningMessengerId = new AtomicInteger();
    private Map<Integer, MapleGuild> guilds = new ConcurrentHashMap<Integer, MapleGuild>();
    private Map<Integer, MapleAlliance> alliances = new LinkedHashMap<Integer, MapleAlliance>();
    private WorldPlayerStorage playerStorage = new WorldPlayerStorage();

    private WorldRegistryImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT MAX(party)+1 FROM characters");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                runningPartyId.set(rs.getInt(1));
            } else {
                log.error("无法设置 组队编号. Sql [SELECT MAX(party)+1 FROM characters]");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            ServerExceptionHandler.HandlerSqlException(e);
        }
        runningMessengerId.set(1);
    }

    public static WorldRegistryImpl getInstance() {
        if (instance == null) {
            try {
                instance = new WorldRegistryImpl();
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                // can't do much anyway we are fucked ^^
                //  throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public ChannelServerStorage getChannelServerStorage() {
        return channelServerStorage;
    }

    @Override
    public WorldChannelInterface registerChannelServer(String authKey, ChannelWorldInterface cb) throws RemoteException {
        return channelServerStorage.registerChannelServer(authKey, cb);
    }

    @Override
    public void deregisterChannelServer(ChannelDescriptor channelDescriptor) throws RemoteException {
        channelServerStorage.deregisterChannelServer(channelDescriptor);
        for (LoginWorldInterface wli : loginServer) {
            wli.channelOffline(channelDescriptor);
        }
    }

    @Override
    public WorldLoginInterface registerLoginServer(String authKey, LoginWorldInterface cb) throws RemoteException {
        WorldLoginInterface ret = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM loginserver WHERE `key` = SHA1(?) AND world = ?");
            ps.setString(1, authKey);
            ps.setInt(2, WorldServer.getInstance().getConfig().getWorldId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                loginServer.add(cb);
                for (ChannelWorldInterface cwi : channelServerStorage.getServers()) {
                    cb.channelOnline(cwi.getDescriptor(), cwi.getIP());
                }
                ret = new WorldLoginInterfaceImpl();
                log.info("登陆服务器 已上线.");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            log.error("Encountered database error while authenticating loginserver", e);
        }
        return ret;
    }

    @Override
    public void deregisterLoginServer(LoginWorldInterface cb) throws RemoteException {
        loginServer.remove(cb);
    }

    public List<LoginWorldInterface> getLoginServer() {
        return new LinkedList<LoginWorldInterface>(loginServer);
    }

    public ChannelWorldInterface getChannel(ChannelDescriptor channel) {
        return channelServerStorage.getChannelWorldInterface(channel);
    }

    public Collection<ChannelDescriptor> getChannelDescriptors() {
        return channelServerStorage.getChannelDescriptors();
    }

    public Collection<ChannelWorldInterface> getAllChannelServers() {
        return channelServerStorage.getServers();
    }

    public ChannelList getChannelList(ChannelDescriptor channelDescriptor) {
        return getChannelList(channelDescriptor.getWorld());
    }

    public ChannelList getChannelList(int world) {
        return getChannelServerStorage().getChannelWorldInterfaces(world);
    }

    public MapleParty createParty(MaplePartyCharacter chrfor) {
        int partyid = runningPartyId.getAndIncrement();
        MapleParty party = new MapleParty(partyid, chrfor);
        parties.put(party.getId(), party);
        return party;
    }

    public MapleParty getParty(int partyid) {
        return parties.get(partyid);
    }

    public MapleParty disbandParty(int partyid) {
        return parties.remove(partyid);
    }

    public int createGuild(int leaderId, String name) {
        return MapleGuild.createGuild(leaderId, name);
    }

    public void setGuildLeader(int id, int Leader, int fromchrid) {
        MapleGuild guild = guilds.get(id);
        if (guild != null) {
            //System.out.println("已从总集合储存家族：");
            guild.changeRank(Leader, 1, fromchrid);
            guild.changeRank(fromchrid, 5, -2);
            guild.setLeader(Leader);
            guild.writeToDB();
        }
    }

    public void setGuild(MapleGuild guild) {
        if (guild != null) {
            guilds.put(guild.getId(), guild);
        }
    }

    public MapleGuild getGuild(int id, MapleGuildCharacter mgc) {
        if (guilds.get(id) != null) {
            return guilds.get(id);
        }

        MapleGuild g = new MapleGuild(id, mgc);

        if (g.getId() == -1) {//failed to load
            return null;
        }

        guilds.put(id, g);
        // System.out.println("");
        // System.out.println("正在读取家族：" + g.getName()
        //          + ":族长是：" + g.getLeaderId());
        //  System.out.println("");
        return g;
    }

    public void clearGuilds() {
        guilds.clear();
        try { //reload all the online characters in guilds
            for (ChannelWorldInterface cwi : this.getAllChannelServers()) {
                cwi.reloadGuildCharacters();
            }
        } catch (RemoteException re) {
            ServerExceptionHandler.HandlerRemoteException(re);
            log.error("RemoteException occurred while attempting to reload guilds.", re);
        }
    }

    public void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline, int channel) {
        MapleGuild g = getGuild(mgc.getGuildId(), mgc);
        if (g != null) {
            g.setOnline(mgc.getId(), bOnline, channel);
        }
    }

    public int addGuildMember(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            return g.addGuildMember(mgc);
        }
        return 0;
    }

    public void leaveGuild(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.leaveGuild(mgc);
        }
    }

    public void guildChat(int gid, String name, int cid, String msg) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.guildChat(name, cid, msg);
        }
    }

    public void changeRank(int gid, int cid, int newRank) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.changeRank(cid, newRank, -1);
        }
    }

    public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
        MapleGuild g = guilds.get(initiator.getGuildId());
        if (g != null) {
            g.expelMember(initiator, name, cid);
        }
    }

    public void setGuildNotice(int gid, String notice) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.setGuildNotice(notice);
        }
    }

    public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.memberLevelJobUpdate(mgc);
        }
    }

    public void changeRankTitle(int gid, String[] ranks) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.changeRankTitle(ranks);
        }
    }

    public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.setGuildEmblem(bg, bgcolor, logo, logocolor);
        }
    }

    public void disbandGuild(int gid) {
        MapleGuild g = guilds.get(gid);
        g.disbandGuild();
        guilds.remove(gid);
    }

    public boolean setGuildAllianceId(int gId, int aId) {
        MapleGuild guild = guilds.get(gId);
        if (guild != null) {
            guild.setAllianceId(aId);
            return true;
        }
        return false;
    }

    public boolean increaseGuildCapacity(int gid) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            return g.increaseCapacity();
        }
        return false;
    }

    public void gainGP(int gid, int amount) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.gainGP(amount);
        }
    }

    public MapleMessenger createMessenger(MapleMessengerCharacter chrfor) {
        int messengerid = runningMessengerId.getAndIncrement();
        MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
        messengers.put(messenger.getId(), messenger);
        return messenger;
    }

    public MapleMessenger getMessenger(int messengerid) {
        return messengers.get(messengerid);
    }

    public PlayerBuffStorage getPlayerBuffStorage() {
        return playerStorage.getBuffStorage();
    }

    public WorldPlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    public MapleAlliance getAlliance(int id) {
        synchronized (alliances) {
            if (alliances.containsKey(id)) {
                return alliances.get(id);
            }
            return null;
        }
    }

    public void addAlliance(int id, MapleAlliance alliance) {
        synchronized (alliances) {
            if (!alliances.containsKey(id)) {
                alliances.put(id, alliance);
            }
        }
    }

    public void disbandAlliance(int id) {
        synchronized (alliances) {
            MapleAlliance alliance = alliances.get(id);
            if (alliance != null) {
                for (Integer gid : alliance.getGuilds()) {
                    MapleGuild guild = guilds.get(gid);
                    guild.setAllianceId(0);
                }
                alliances.remove(id);
            }
        }
    }

    public void allianceMessage(int id, MaplePacket packet, int exception, int guildex) {
        MapleAlliance alliance = alliances.get(id);
        if (alliance != null) {
            for (Integer gid : alliance.getGuilds()) {
                if (guildex == gid) {
                    continue;
                }
                MapleGuild guild = guilds.get(gid);
                if (guild != null) {
                    guild.broadcast(packet, exception);
                }
            }
        }
    }

    public boolean addGuildtoAlliance(int aId, int guildId) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.addGuild(guildId);
            return true;
        }
        return false;
    }

    public boolean removeGuildFromAlliance(int aId, int guildId) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.removeGuild(guildId);
            return true;
        }
        return false;
    }

    public boolean setAllianceRanks(int aId, String[] ranks) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.setRankTitle(ranks);
            return true;
        }
        return false;
    }

    public boolean setAllianceNotice(int aId, String notice) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.setNotice(notice);
            return true;
        }
        return false;
    }

    public boolean increaseAllianceCapacity(int aId, int inc) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.increaseCapacity(inc);
            return true;
        }
        return false;
    }
}
