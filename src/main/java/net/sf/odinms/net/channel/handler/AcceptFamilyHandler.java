/*
 接受家族的程序 
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author
 */
public final class AcceptFamilyHandler extends AbstractMaplePacketHandler {

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(AcceptFamilyHandler.class);

	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		// log.info("111111111111111111");
		// log.debug(slea.toString());
		int inviterId = slea.readInt();
		// String inviterName = slea.readMapleAsciiString();
		MapleCharacter inviter = c.getChannelServer()
				.getCharacterFromAllServers(inviterId);
		if (inviter != null) {
			inviter.getClient()
					.getSession()
					.write(MaplePacketCreator.sendFamilyJoinResponse(true, c
							.getPlayer().getName()));
		}
		c.getSession().write(MaplePacketCreator.sendFamilyMessage());
	}
}