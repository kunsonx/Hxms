package net.sf.odinms.net.channel;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import net.sf.odinms.client.BuddyList;
import net.sf.odinms.client.BuddylistEntry;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacterUtil;
import net.sf.odinms.client.BuddyList.BuddyAddResult;
import net.sf.odinms.client.BuddyList.BuddyOperation;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.ByteArrayMaplePacket;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.MapleMessenger;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.guild.MapleGuildSummary;
import net.sf.odinms.net.world.remote.CheaterData;
import net.sf.odinms.server.ShutdownServer;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.CollectionUtil;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Guild.MapleGuild_Msg;
import org.apache.mina.core.session.IoSession;

/**
 * World 对 Channel 操作实体类 [run at ChannelServer]
 *
 * @author Matze
 */
public class ChannelWorldInterfaceImpl extends UnicastRemoteObject implements ChannelWorldInterface {

    private static final long serialVersionUID = 7815256899088644192L;
    private ChannelServer server;

    public ChannelWorldInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public ChannelWorldInterfaceImpl(ChannelServer server) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.server = server;
    }

    @Override
    public void setChannelId(int id) throws RemoteException {
        server.setChannel(id);
    }

    @Override
    public int getChannelId() throws RemoteException {
        return server.getChannel();
    }

    @Override
    public String getIP() throws RemoteException {
        return server.getIP();
    }

    @Override
    public void broadcastMessage(String sender, byte[] message) throws RemoteException {
        MaplePacket packet = new ByteArrayMaplePacket(message);
        server.broadcastPacket(packet);
    }

    @Override
    public int whisper(String sender, String target, int channel, String message, boolean gm) throws RemoteException {
        if (server.getPlayerStorage().hasCharacter(target) && (!gm || server.getPlayerStorage().getCharacterByName(target).isGM())) {
            server.getPlayerStorage().getCharacterByName(target).getClient().getSession().write(MaplePacketCreator.getWhisper(sender, channel, message));
            return 1;
        }
        return 0;
    }

    @Override
    public boolean isConnected(int chrId) throws RemoteException {
        return server.getPlayerStorage().hasCharacter(chrId);
    }

    @Override
    public boolean isConnected(String chrName) throws RemoteException {
        return server.getPlayerStorage().hasCharacter(chrName);
    }

    @Override
    public void shutdown(int time) throws RemoteException {
        if (time / 60000 != 0) {
            server.broadcastPacket(MaplePacketCreator.serverNotice(0, "服务器将在 " + (time / 60000) + " 分钟后关闭维护.为避免数据丢失,请各玩家及时下线！"));
        }
        TimerManager.getInstance().schedule(new ShutdownServer(server.getDescriptor()), time);
    }

    @Override
    public void broadcastWorldMessage(String message) throws RemoteException {
        server.broadcastPacket(MaplePacketCreator.serverNotice(0, message));
    }

    @Override
    public int getConnected() throws RemoteException {
        return server.getConnectedClients();
    }

    @Override
    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        updateBuddies(characterId, channel, buddies, true);
    }

    @Override
    public void loggedOn(String name, int characterId, int channel, int buddies[]) throws RemoteException {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
        IPlayerStorage playerStorage = server.getPlayerStorage();
        for (int buddy : buddies) {
            MapleCharacter chr = playerStorage.getCharacterById(buddy);
            if (chr != null) {
                BuddylistEntry ble = chr.getBuddylist().get(characterId);
                if (ble != null && ble.isVisible()) {
                    int mcChannel;
                    if (offline) {
                        ble.setChannel(-1);
                        mcChannel = -1;
                    } else {
                        ble.setChannel(channel);
                        mcChannel = channel - 1;
                    }
                    chr.getBuddylist().put(ble);
                    chr.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                }
            }
        }
    }

    @Override
    public void updateParty(MapleParty party, PartyOperation operation, MaplePartyCharacter target) throws RemoteException {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel()) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    if (operation == PartyOperation.DISBAND) {
                        chr.setParty(null);
                    } else {
                        chr.setParty(party);
                    }
                    chr.getClient().getSession().write(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                }
            }
        }
        switch (operation) {
            case LEAVE:
            case EXPEL:
                if (target.getChannel() == server.getChannel()) {
                    MapleCharacter chr = server.getPlayerStorage().getCharacterByName(target.getName());
                    if (chr != null) {
                        chr.getClient().getSession().write(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                        chr.setParty(null);
                    }
                }
        }
    }

    @Override
    public void partyChat(MapleParty party, String chattext, String namefrom) throws RemoteException {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel() && !(partychar.getName().equals(namefrom))) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(MaplePacketCreator.multiChat(namefrom, chattext, 1));
                }
            }
        }
    }

    @Override
    public boolean isAvailable() throws RemoteException {
        return true;
    }

    @Override
    public int getLocation(String name) throws RemoteException {
        MapleCharacter chr = server.getPlayerStorage().getCharacterByName(name);
        if (chr != null) {
            return server.getPlayerStorage().getCharacterByName(name).getMapId();
        }
        return -1;
    }

    @Override
    public List<CheaterData> getCheaters() throws RemoteException {
        List<CheaterData> cheaters = new ArrayList<CheaterData>();
        List<MapleCharacter> allplayers = new ArrayList<MapleCharacter>(server.getPlayerStorage().getAllCharacters());
        for (int x = allplayers.size() - 1; x >= 0; x--) {
            MapleCharacter cheater = allplayers.get(x);
            if (cheater.getCheatTracker().getPoints() > 0) {
                cheaters.add(new CheaterData(cheater.getCheatTracker().getPoints(), MapleCharacterUtil.makeMapleReadable("ID:" + cheater.getId() + " Name: " + cheater.getName()) + " (" + cheater.getCheatTracker().getPoints() + ") " + cheater.getCheatTracker().getSummary()));
            }
        }
        Collections.sort(cheaters);
        return CollectionUtil.copyFirst(cheaters, 10);
    }

    @Override
    public BuddyAddResult requestBuddyAdd(String addName, int Fromid, String Fromname, int FromChannel, int FromLevel, int FromJobid) {
        MapleCharacter addChar = server.getPlayerStorage().getCharacterByName(addName);
        if (addChar != null) {
            BuddyList buddylist = addChar.getBuddylist();
            if (buddylist.isFull()) {
                return BuddyAddResult.BUDDYLIST_FULL;
            }
            if (!buddylist.contains(Fromid)) {
                //addChar是被添加的人
                buddylist.addBuddyRequest(addChar.getClient(), Fromid, Fromname, FromChannel, FromLevel, FromJobid);
            } else {
                if (buddylist.containsVisible(Fromid)) {
                    return BuddyAddResult.ALREADY_ON_LIST;
                }
            }
        }
        return BuddyAddResult.OK;
    }

    @Override
    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation) {
        MapleCharacter addChar = server.getPlayerStorage().getCharacterById(cid);
        if (addChar != null) {
            BuddyList buddylist = addChar.getBuddylist();
            switch (operation) {
                case ADDED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, cidFrom, channel, true));
                        addChar.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(cidFrom, channel - 1));
                    }
                    break;
                case DELETED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, cidFrom, -1, buddylist.get(cidFrom).isVisible()));
                        addChar.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(cidFrom, -1));
                    }
                    break;
            }
        }
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        IPlayerStorage playerStorage = server.getPlayerStorage();
        for (int characterId : recipientCharacterIds) {
            MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(cidFrom)) {
                    chr.getClient().getSession().write(MaplePacketCreator.multiChat(nameFrom, chattext, 0));
                }
            }
        }
    }

    @Override
    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<Integer> ret = new ArrayList<Integer>(characterIds.length);
        IPlayerStorage playerStorage = server.getPlayerStorage();
        for (int characterId : characterIds) {
            MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(charIdFrom)) {
                    ret.add(characterId);
                }
            }
        }
        int[] retArr = new int[ret.size()];
        int pos = 0;
        for (Integer i : ret) {
            retArr[pos++] = i.intValue();
        }
        return retArr;
    }

    @Override
    public void sendPacket(List<Integer> targetIds, MaplePacket packet,
            int exception)
            throws RemoteException {
        MapleCharacter c;
        for (int i : targetIds) {
            if (i == exception) {
                continue;
            }
            c = server.getPlayerStorage().getCharacterById(i);
            if (c != null) {
                c.getClient().getSession().write(packet);
            }
        }
    }

    @Override
    public void setGuildAndRank(List<Integer> cids, int guildid, int rank,
            int exception) throws RemoteException {
        for (int cid : cids) {
            if (cid != exception) {
                setGuildAndRank(cid, guildid, rank);
            }
        }
    }

    @Override
    public void setGuildAndRank(int cid, int guildid, int rank) throws RemoteException {
        MapleCharacter mc = server.getPlayerStorage().getCharacterById(cid);
        if (mc == null) {
            // log.debug("ERROR: cannot find player in given channel");
            return;
        }

        boolean bDifferentGuild;
        if (guildid == -1 && rank == -1) //just need a respawn
        {
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildid != mc.getGuildid();
            mc.setGuildId(guildid);
            mc.setGuildRank(rank);
            mc.saveGuildStatus();
        }
        if (bDifferentGuild) {
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(cid), false);
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
        }
    }

    @Override
    public void setOfflineGuildStatus(int guildid, byte guildrank, int cid) throws RemoteException {
        org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?");
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, cid);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException se) {
            log.error("SQLException: " + se.getLocalizedMessage(), se);
        }
    }

    @Override
    public void reloadGuildCharacters() throws RemoteException {
        for (MapleCharacter mc : server.getPlayerStorage().getAllCharacters()) {
            if (mc.getGuildid() > 0) {
                //multiple world ops, but this method is ONLY used 
                //in !clearguilds gm command, so it shouldn't be a problem
                server.getWorldInterface().setGuildMemberOnline(
                        mc.getMGC(), true, server.getChannel());
                server.getWorldInterface().memberLevelJobUpdate(mc.getMGC());
            }
        }
        server.reloadGuildSummary();
    }

    @Override
    public void changeEmblem(int gid, List<Integer> affectedPlayers, MapleGuildSummary mgs) throws RemoteException {
        server.updateGuildSummary(gid, mgs);
        this.sendPacket(affectedPlayers, MapleGuild_Msg.家族徽章变更(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
        this.setGuildAndRank(affectedPlayers, -1, -1, -1);	//respawn player
    }

    @Override
    public String listGMs() throws RemoteException {
        String list = "";
        for (MapleCharacter c : server.getPlayerStorage().getAllCharacters()) {
            if (c.isGM()) {
                list += c.getName() + " ";
            }
        }
        return list;
    }

    @Override
    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException {
        if (isConnected(target)) {
            MapleMessenger messenger = server.getPlayerStorage().getCharacterByName(target).getMessenger();
            if (messenger == null) {
                server.getPlayerStorage().getCharacterByName(target).getClient().getSession().write(MaplePacketCreator.messengerInvite(sender, messengerid));
                MapleCharacter from = server.getChannelServer(fromchannel).getPlayerStorage().getCharacterByName(sender);
                from.getClient().getSession().write(MaplePacketCreator.messengerNote(target, 4, 1));
            } else {
                MapleCharacter from = server.getChannelServer(fromchannel).getPlayerStorage().getCharacterByName(sender);
                from.getClient().getSession().write(MaplePacketCreator.messengerChat(sender + " : " + target + " 已经使用小纸条", "[系统]"));
            }
        }
    }

    @Override
    public void addMessengerPlayer(MapleMessenger messenger, String namefrom, int fromchannel, int position) throws RemoteException {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    MapleCharacter from = server.getChannelServer(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                    chr.getClient().getSession().write(MaplePacketCreator.addMessengerPlayer(namefrom, from, position, fromchannel - 1));
                    from.getClient().getSession().write(MaplePacketCreator.addMessengerPlayer(chr.getName(), chr, messengerchar.getPosition(), messengerchar.getChannel() - 1));
                }
            } else if (messengerchar.getChannel() == server.getChannel() && (messengerchar.getName().equals(namefrom))) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(MaplePacketCreator.joinMessenger(messengerchar.getPosition()));
                }
            }
        }
    }

    @Override
    public void removeMessengerPlayer(MapleMessenger messenger, int position) throws RemoteException {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel()) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(MaplePacketCreator.removeMessengerPlayer(position));
                }
            }
        }
    }

    @Override
    public void messengerChat(MapleMessenger messenger, String chattext, String namefrom, String user) throws RemoteException {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().getSession().write(MaplePacketCreator.messengerChat(user, chattext));
                }
            }
        }
    }

    @Override
    public void declineChat(String target, String namefrom) throws RemoteException {
        if (isConnected(target)) {
            MapleMessenger messenger = server.getPlayerStorage().getCharacterByName(target).getMessenger();
            if (messenger != null) {
                server.getPlayerStorage().getCharacterByName(target).getClient().getSession().write(
                        MaplePacketCreator.messengerNote(namefrom, 5, 0));
            }
        }
    }

    @Override
    public void updateMessenger(MapleMessenger messenger, String namefrom, int position, int fromchannel) throws RemoteException {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    MapleCharacter from = server.getChannelServer(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                    chr.getClient().getSession().write(MaplePacketCreator.updateMessengerPlayer(namefrom, from, position, fromchannel - 1));
                }
            }
        }
    }

    @Override
    public void spouseChat(String from, String target, String message) throws RemoteException {
        if (isConnected(target)) {
            server.getPlayerStorage().getCharacterByName(target).getClient().getSession().write(
                    MaplePacketCreator.spouseChat(from, message, 5));
        }
    }

    @Override
    public void broadcastGMMessage(String sender, byte[] message) throws RemoteException {
        MaplePacket packet = new ByteArrayMaplePacket(message);
        server.broadcastGMPacket(packet);
    }

    /**
     * 获得频道描述符
     *
     * @throws RemoteException
     */
    @Override
    public ChannelDescriptor getDescriptor() throws RemoteException {
        return server.getDescriptor();
    }

    @Override
    public void deregisterOfflinePlayer(int cid) throws RemoteException {
        for (MapleMap mapleMap : server.getMapFactory().getMaps().values()) {
            mapleMap.getOfflinePlayer().deregisterPlayer(cid);
        }
    }

    @Override
    public long checkClientIvKey(byte[] ivCheck) throws RemoteException {
        for (Map.Entry<Long, IoSession> entry : server.getAcceptor().getManagedSessions().entrySet()) {
            MapleClient client = (MapleClient) entry.getValue().getAttribute(MapleClient.CLIENT_KEY);
            if (client != null && client.getIvcheck() != null) {
                if (Arrays.equals(ivCheck, client.getIvcheck())) {
                    client.setIvcheck(null);
                    TimerManager.getInstance().register(new Runnable() {
                        @Override
                        public void run() {
                        }
                    }, 25000);
                    return entry.getKey();
                }
            }
        }
        return -1;
    }

    @Override
    public void disconnectClient(long clientId) throws RemoteException {
        IoSession session = server.getAcceptor().getManagedSessions().get(clientId);
        if (session != null) {
            session.close();
        }
    }
}
