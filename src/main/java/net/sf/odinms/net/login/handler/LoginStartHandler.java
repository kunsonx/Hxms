/*
 */
package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class LoginStartHandler implements MaplePacketHandler {

	private org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(LoginStartHandler.class);

	@Override
	public boolean validateState(MapleClient c) {
		return !c.isLoggedIn();
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// log.info("发现连接:");
		c.getSession().write(MaplePacketCreator.getHelloto());
		// c.getSession().write(MaplePacketCreator.testPacket("22 00 09 00 4D 61 70 4C 6F 67 69 6E 32 AB C8 ED 77 01"));
	}
}
