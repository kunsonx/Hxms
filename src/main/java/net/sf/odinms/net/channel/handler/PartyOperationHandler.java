//开启组队
package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class PartyOperationHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int operation = slea.readByte();
        MapleCharacter player = c.getPlayer();
        WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
        MapleParty party = player.getParty();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(player);

        switch (operation) {
            case 1: { // 创建
                if (c.getPlayer().getParty() == null) {
                    try {
                        party = wci.createParty(partyplayer);
                        player.setParty(party);
                    } catch (RemoteException e) {
                        ServerExceptionHandler.HandlerRemoteException(e);
                        c.getChannelServer().reconnectWorld();
                    }
                    c.getSession().write(MaplePacketCreator.partyCreated(party));
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "你已经存在一个队伍中，无法创建！"));
                }
                break;
            }
            case 2: { // 离开组队
                if (party != null) {
                    try {
                        if (partyplayer.equals(party.getLeader())) { // disband
                            for (MapleMapObject mapleMapObject : player.getMap().getAllMonster()) {
                                if (mapleMapObject.getType() == MapleMapObjectType.MONSTER) {
                                    MapleMonster mm = (MapleMonster) mapleMapObject;
                                    if (mm.isBoss()) {
                                        player.dropMessage("您正在挑战BOSS。不允许解散队伍。");
                                        return;
                                    }
                                }
                            }
                            wci.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                            if (player.getEventInstance() != null) {
                                player.getEventInstance().disbandParty();
                            }
                        } else {
                            wci.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                            if (player.getEventInstance() != null) {
                                player.getEventInstance().leftParty(player);
                            }
                        }
                    } catch (RemoteException e) {
                        ServerExceptionHandler.HandlerRemoteException(e);
                        c.getChannelServer().reconnectWorld();
                    }
                    player.setParty(null);
                }
                break;
            }
            case 4: { // 邀请
                //TODO store pending invitations and check against them
                String name = slea.readMapleAsciiString();
                MapleCharacter invited = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (invited != null) {
                    if (invited.getParty() == null) {
                        if (!invited.getPartyInvited()) {
                            if (party != null && party.getMembers().size() < 6) {
                                invited.setPartyInvited(true);
                                invited.getClient().getSession().write(MaplePacketCreator.partyInvite(player));
                                c.getSession().write(MaplePacketCreator.serverNotice(1, "向“" + name + "”发送了组队邀请！"));
                            } else {
                                c.getSession().write(MaplePacketCreator.serverNotice(1, "您的组队成员已达到6人，无法再邀请其他人员！"));
                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.serverNotice(5, "“" + name + "”正在做其他的事情！"));
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.serverNotice(5, "“" + name + "”已经加入其他组队！"));
                    }
                } else {
                    // c.getSession().write(MaplePacketCreator.partyStatusMessage(13));//显示未找到玩家的消息 13
             //       c.getSession().write(MaplePacketCreator.serverNotice(5, "在当前频道无法找到该玩家！"));
                }
                break;
            }
            case 5: { // 驱逐成员
                int cid = slea.readInt();
                if (partyplayer.equals(party.getLeader())) {
                    MaplePartyCharacter expelled = party.getMemberById(cid);
                    if (expelled != null && !expelled.equals(party.getLeader())) {
                        try {
                            wci.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                            if (player.getEventInstance() != null) {
                                /*if leader wants to boot someone, then the whole party gets expelled
                                 TODO: Find an easier way to get the character behind a MaplePartyCharacter
                                 possibly remove just the expel.*/
                                if (expelled.isOnline()) {
                                    MapleCharacter expellee = c.getChannelServer(expelled.getChannel()).getPlayerStorage().getCharacterById(expelled.getId());
                                    if (expellee != null && expellee.getEventInstance().getName().equals(player.getEventInstance().getName())) {
                                        player.getEventInstance().disbandParty();
                                    }
                                }
                            }
                        } catch (RemoteException e) {
                            ServerExceptionHandler.HandlerRemoteException(e);
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "不是队长，无法进行该操作！"));
                }
                break;
            }
            case 6: { //更改队长
                int nlid = slea.readInt();
                MaplePartyCharacter newleader = party.getMemberById(nlid);
                if (partyplayer.equals(party.getLeader()) && newleader.isOnline()) {
                    try {
                        party.setLeader(newleader);
                        wci.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newleader);
                    } catch (RemoteException re) {
                        ServerExceptionHandler.HandlerRemoteException(re);
                        c.getChannelServer().reconnectWorld();
                    }
                }
                break;
            }
        }
    }
}
