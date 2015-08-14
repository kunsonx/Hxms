/*
	
 */

package net.sf.odinms.net.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public final class NoOpHandler implements MaplePacketHandler {

	private static NoOpHandler instance = new NoOpHandler();

	private NoOpHandler() {
		// singleton
	}

	public static NoOpHandler getInstance() {
		return instance;
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// no op
	}

	@Override
	public boolean validateState(MapleClient c) {
		return true;
	}
}