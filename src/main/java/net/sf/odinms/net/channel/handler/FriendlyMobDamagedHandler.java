/*
	
 */

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author iamSTEVE
 */
public class FriendlyMobDamagedHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		int attackeroid = slea.readInt();
		slea.skip(4); // always 1?
		int damagedoid = slea.readInt();
		// TODO: Make it door more than read packets and i dont think the damage
		// is sent from client? I'll have to sniff myself sometime
	}
}