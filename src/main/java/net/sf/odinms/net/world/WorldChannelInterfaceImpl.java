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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.login.remote.LoginWorldInterface;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.remote.CheaterData;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.net.world.remote.WorldLocation;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.CollectionUtil;

/**
 * Channel 对 World 操作实现类 [run at WorldServer]
 *
 * @author Matze
 */
public class WorldChannelInterfaceImpl extends UnicastRemoteObject implements WorldChannelInterface {

    private static final long serialVersionUID = -5568606556435590482L;
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WorldChannelInterfaceImpl.class);
    /**
     * 成员变量
     */
    private ChannelWorldInterface cb;
    private boolean ready = false;
    private ChannelDescriptor channelDescriptor;

    public WorldChannelInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public WorldChannelInterfaceImpl(ChannelWorldInterface cb) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.cb = cb;
        this.channelDescriptor = cb.getDescriptor();
    }

    @Override
    public int getGameServerPort() throws RemoteException {
        return ChannelServerStorage.getPort() - 1 + (channelDescriptor.getWorld() * 10 + channelDescriptor.getId());
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() throws RemoteException {
        return channelDescriptor;
    }

    @Override
    public void serverReady() throws RemoteException {
        ready = true;
        for (LoginWorldInterface wli : WorldRegistryImpl.getInstance().getLoginServer()) {
            try {
                wli.channelOnline(channelDescriptor, cb.getIP());
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        log.info(String.format("世界 %d 频道服务器 %d 已上线.", channelDescriptor.getWorld(), channelDescriptor.getId()));
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public String getIP(int channel) throws RemoteException {
        String result = "0.0.0.0:0";
        ChannelWorldInterface cwi = all().getChannel(channel);
        if (cwi != null) {
            try {
                result = cwi.getIP();
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        return result;
    }

    @Override
    public int whisper(String sender, String target, int channel, String message, boolean gm) throws RemoteException {
        int result = 0;
        for (ChannelWorldInterface cwi : all()) {
            try {
                result = cwi.whisper(sender, target, channel, message, gm);
                if (result > 0) {
                    return result;
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        return result;
    }

    @Override
    public boolean isConnected(int chrId) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                if (cwi.isConnected(chrId)) {
                    return true;
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        return false;
    }

    @Override
    public boolean isConnected(String chrName) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                if (cwi.isConnected(chrName)) {
                    return true;
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        return false;
    }

    @Override
    public void broadcastMessage(String sender, byte[] message) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.broadcastMessage(sender, message);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public int find(String charName) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                if (cwi.isConnected(charName)) {
                    return cwi.getChannelId();
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        return -1;
    }

    // can we generify this
    @Override
    public int find(int characterId) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                if (cwi.isConnected(characterId)) {
                    return cwi.getChannelId();
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                //  WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
        return -1;
    }

    @Override
    public void shutdown(int time) throws RemoteException {
        for (LoginWorldInterface lwi : WorldRegistryImpl.getInstance().getLoginServer()) {
            try {
                lwi.shutdown();
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterLoginServer(lwi);
            }
        }
        for (ChannelWorldInterface cwi : WorldRegistryImpl.getInstance().getChannelServerStorage().getServers()) {
            try {
                cwi.shutdown(time);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                //WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public Map<Integer, Integer> getConnected() throws RemoteException {
        Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
        int total = 0;
        ChannelList list = all();
        for (ChannelDescriptor descriptor : list.getChannelDescriptors()) {
            try {
                int curConnected = list.getChannel(descriptor.getId()).getConnected();
                ret.put(descriptor.getId(), curConnected);
                total += curConnected;
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        ret.put(0, total);
        return ret;
    }

    @Override
    public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.loggedOn(name, characterId, channel, buddies);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.loggedOff(name, characterId, channel, buddies);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    //TODO only notify channels where partymembers are?
    @Override
    public void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) throws RemoteException {
        MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        switch (operation) {
            case JOIN:
                party.addMember(target);
                break;
            case EXPEL:
            case LEAVE:
                party.removeMember(target);
                break;
            case DISBAND:
                WorldRegistryImpl.getInstance().disbandParty(partyid);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                party.updateMember(target);
                break;
            case CHANGE_LEADER:
                party.setLeader(target);
                break;
            default:
                throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
        }
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.updateParty(party, operation, target);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public MapleParty createParty(MaplePartyCharacter chrfor) throws RemoteException {
        return WorldRegistryImpl.getInstance().createParty(chrfor);
    }

    @Override
    public MapleParty getParty(int partyid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getParty(partyid);
    }

    @Override
    public void partyChat(int partyid, String chattext, String namefrom) throws RemoteException {
        MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.partyChat(party, chattext, namefrom);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                //   WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public boolean isAvailable() throws RemoteException {
        return true;
    }

    @Override
    public WorldLocation getLocation(String charName) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                if (cwi.isConnected(charName)) {
                    return new WorldLocation(cwi.getLocation(charName), cwi.getChannelId());
                }
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        return null;
    }

    @Override
    public List<CheaterData> getCheaters() throws RemoteException {
        List<CheaterData> allCheaters = new ArrayList<CheaterData>();
        for (ChannelWorldInterface cwi : all()) {
            try {
                allCheaters.addAll(cwi.getCheaters());
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 10);
    }

    @Override
    public ChannelWorldInterface getChannelInterface(int channel) {
        ChannelWorldInterface cwi = all().getChannel(channel);
        return cwi;
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            cwi.buddyChat(recipientCharacterIds, cidFrom, nameFrom, chattext);
        }
    }

    @Override
    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<CharacterIdChannelPair>(characterIds.length);
        ChannelList list = all();
        for (ChannelDescriptor descriptor : list.getChannelDescriptors()) {
            ChannelWorldInterface cwi = list.getChannel(descriptor.getId());
            for (int charid : cwi.multiBuddyFind(charIdFrom, characterIds)) {
                foundsChars.add(new CharacterIdChannelPair(charid, descriptor.getId()));
            }
        }
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }

    @Override
    public MapleGuild getGuild(int id, MapleGuildCharacter mgc) throws RemoteException {
        return WorldRegistryImpl.getInstance().getGuild(id, mgc);
    }

    @Override
    public void clearGuilds() throws RemoteException {
        WorldRegistryImpl.getInstance().clearGuilds();
    }

    @Override
    public void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline, int channel) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildMemberOnline(mgc, bOnline, channel);
    }

    @Override
    public int addGuildMember(MapleGuildCharacter mgc) throws RemoteException {
        return WorldRegistryImpl.getInstance().addGuildMember(mgc);
    }

    @Override
    public void guildChat(int gid, String name, int cid, String msg) throws RemoteException {
        WorldRegistryImpl.getInstance().guildChat(gid, name, cid, msg);
    }

    @Override
    public void leaveGuild(MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl.getInstance().leaveGuild(mgc);
    }

    @Override
    public void changeRank(int gid, int cid, int newRank) throws RemoteException {
        WorldRegistryImpl.getInstance().changeRank(gid, cid, newRank);
    }

    @Override
    public void expelMember(MapleGuildCharacter initiator, String name, int cid) throws RemoteException {
        WorldRegistryImpl.getInstance().expelMember(initiator, name, cid);
    }

    @Override
    public void setGuildNotice(int gid, String notice) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildNotice(gid, notice);
    }

    @Override
    public void memberLevelJobUpdate(MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl.getInstance().memberLevelJobUpdate(mgc);
    }

    @Override
    public void changeRankTitle(int gid, String[] ranks) throws RemoteException {
        WorldRegistryImpl.getInstance().changeRankTitle(gid, ranks);
    }

    @Override
    public int createGuild(int leaderId, String name) throws RemoteException {
        return WorldRegistryImpl.getInstance().createGuild(leaderId, name);
    }

    @Override
    public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildEmblem(gid, bg, bgcolor, logo, logocolor);
    }

    @Override
    public void disbandGuild(int gid) throws RemoteException {
        WorldRegistryImpl.getInstance().disbandGuild(gid);
    }

    @Override
    public boolean increaseGuildCapacity(int gid) throws RemoteException {
        return WorldRegistryImpl.getInstance().increaseGuildCapacity(gid);
    }

    @Override
    public void gainGP(int gid, int amount) throws RemoteException {
        WorldRegistryImpl.getInstance().gainGP(gid, amount);
    }

    @Override
    public String listGMs() throws RemoteException {
        String list = "";
        for (ChannelWorldInterface cwi : all()) {
            list += cwi.listGMs();
        }
        return list;
    }

    @Override
    public MapleMessenger createMessenger(MapleMessengerCharacter chrfor) throws RemoteException {
        return WorldRegistryImpl.getInstance().createMessenger(chrfor);
    }

    @Override
    public MapleMessenger getMessenger(int messengerid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getMessenger(messengerid);
    }

    @Override
    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.messengerInvite(sender, messengerid, target, fromchannel);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public void leaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);

        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.removeMessengerPlayer(messenger, position);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target);

        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public void messengerChat(int messengerid, String chattext, String namefrom, String now) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.messengerChat(messenger, chattext, namefrom, now);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public void declineChat(String target, String namefrom) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.declineChat(target, namefrom);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public void updateMessenger(int messengerid, String namefrom, int fromchannel) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        int position = messenger.getPositionByName(namefrom);

        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.updateMessenger(messenger, namefrom, position, fromchannel);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
                // WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentRemoveMember(target);
    }

    @Override
    public void silentJoinMessenger(int messengerid, MapleMessengerCharacter target, int position) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentAddMember(target, position);
    }

    @Override
    public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) throws RemoteException {
        PlayerBuffStorage buffStorage = WorldRegistryImpl.getInstance().getPlayerBuffStorage();
        buffStorage.addBuffsToStorage(chrid, toStore);
    }

    @Override
    public List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) throws RemoteException {
        PlayerBuffStorage buffStorage = WorldRegistryImpl.getInstance().getPlayerBuffStorage();
        return buffStorage.getBuffsFromStorage(chrid);
    }

    @Override
    public void spouseChat(String sender, String target, String message) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.spouseChat(sender, target, message);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public void broadcastGMMessage(String sender, byte[] message) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.broadcastGMMessage(sender, message);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public MapleAlliance getAlliance(int id) throws RemoteException {
        return WorldRegistryImpl.getInstance().getAlliance(id);
    }

    @Override
    public void addAlliance(int id, MapleAlliance alliance) throws RemoteException {
        WorldRegistryImpl.getInstance().addAlliance(id, alliance);
    }

    @Override
    public void disbandAlliance(int id) throws RemoteException {
        WorldRegistryImpl.getInstance().disbandAlliance(id);
    }

    @Override
    public void allianceMessage(int id, MaplePacket packet, int exception, int guildex) throws RemoteException {
        WorldRegistryImpl.getInstance().allianceMessage(id, packet, exception, guildex);
    }

    @Override
    public boolean setAllianceNotice(int aId, String notice) throws RemoteException {
        return WorldRegistryImpl.getInstance().setAllianceNotice(aId, notice);
    }

    @Override
    public boolean setAllianceRanks(int aId, String[] ranks) throws RemoteException {
        return WorldRegistryImpl.getInstance().setAllianceRanks(aId, ranks);
    }

    @Override
    public boolean removeGuildFromAlliance(int aId, int guildId) throws RemoteException {
        return WorldRegistryImpl.getInstance().removeGuildFromAlliance(aId, guildId);
    }

    @Override
    public boolean addGuildtoAlliance(int aId, int guildId) throws RemoteException {
        return WorldRegistryImpl.getInstance().addGuildtoAlliance(aId, guildId);
    }

    @Override
    public boolean setGuildAllianceId(int gId, int aId) throws RemoteException {
        return WorldRegistryImpl.getInstance().setGuildAllianceId(gId, aId);
    }

    @Override
    public boolean increaseAllianceCapacity(int aId, int inc) throws RemoteException {
        return WorldRegistryImpl.getInstance().increaseAllianceCapacity(aId, inc);
    }

    @Override
    public void broadcastWorldMessage(String message) throws RemoteException {
        for (ChannelWorldInterface cwi : all()) {
            try {
                cwi.broadcastWorldMessage(message);
            } catch (RemoteException e) {
                ServerExceptionHandler.HandlerRemoteException(e);
            }
        }
    }

    @Override
    public void setGuildLeader(int id, int labe, int fromchrid) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildLeader(id, labe, fromchrid);
    }

    @Override
    public void setGuild(MapleGuild guild) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuild(guild);
    }

    private ChannelList all() {
        return WorldRegistryImpl.getInstance().getChannelList(channelDescriptor);
    }

    @Override
    public void registryOfflinePlayer(int cid) throws RemoteException {
        WorldRegistryImpl.getInstance().getPlayerStorage().registryOfficePlayer(cid, channelDescriptor);
    }

    @Override
    public void deregisterOfflinePlayer(int cid) throws RemoteException {
        ChannelDescriptor cd = WorldRegistryImpl.getInstance().getPlayerStorage().deregisterOfficePlayer(cid);
        if (cd != null) {
            WorldRegistryImpl.getInstance().getChannel(cd).deregisterOfflinePlayer(cid);
        }
    }
}
