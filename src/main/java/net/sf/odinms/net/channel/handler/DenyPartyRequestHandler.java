/*
 否认组队请求处理
 */
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
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DenyPartyRequestHandler extends AbstractMaplePacketHandler {

	private final static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(DenyPartyRequestHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// 第二方收到组队邀请处理
		int mode = slea.readByte();
		WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
		MapleCharacter player = c.getPlayer();
		MapleParty party = player.getParty();
		MaplePartyCharacter partyplayer = new MaplePartyCharacter(player);
		if (mode == 0x1e) { // 拒绝邀请
			int partyid = slea.readInt();
			try {
				party = wci.getParty(partyid);
				if (party != null && party.getLeader().isOnline()) {
					MapleCharacter cfrom = party.getLeader().getPlayer();
					if (cfrom != null) {
						cfrom.dropMessage(5, "“" + player.getName()
								+ "”拒绝了组队招待！");
						// cfrom.getClient().getSession().write(MaplePacketCreator.partyStatusMessage(23,
						// c.getPlayer().getName()));
					}
				} else {
					c.getSession().write(
							MaplePacketCreator.serverNotice(5, "该组队已经不存在!"));
				}
				c.getPlayer().setPartyInvited(false); // 设置玩家已经有组队 不能再接受组队
			} catch (RemoteException e) {
				ServerExceptionHandler.HandlerRemoteException(e);
				// c.getChannelServer().reconnectWorld();
			}
		} else if (mode == 0x1f) {// 通过邀请
			int partyid = slea.readInt();
			if (!c.getPlayer().getPartyInvited()) {
				return;
			}
			if (c.getPlayer().getParty() == null) {
				try {
					party = wci.getParty(partyid);
					if (party != null && party.getLeader().isOnline()) {
						if (party.getMembers().size() < 6) {
							wci.updateParty(party.getId(), PartyOperation.JOIN,
									partyplayer);
							player.receivePartyMemberHP();
							player.updatePartyMemberHP();
						} else {
							c.getSession().write(
									MaplePacketCreator.partyStatusMessage(17));
						}
					} else {
						c.getSession()
								.write(MaplePacketCreator.serverNotice(5,
										"要加入的队伍不存在"));
					}
					c.getPlayer().setPartyInvited(false); // 设置玩家已经有组队 不能再接受组队
				} catch (RemoteException e) {
					ServerExceptionHandler.HandlerRemoteException(e);
					c.getChannelServer().reconnectWorld();
				}
			} else {
				c.getSession().write(
						MaplePacketCreator
								.serverNotice(5, "您已经有一个组队，无法加入其他组队!"));
			}
		} else {
			log.debug("组队(" + mode + ") 请截图后发送给GM");
		}
	}
}
