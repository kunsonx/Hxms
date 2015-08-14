package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author
 */
public final class PetExcludeItemsHandler extends AbstractMaplePacketHandler {

	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		slea.readLong();
		// 捡取过滤列表
		byte amount = slea.readByte();
		for (int i = 0; i < amount; i++) {
			c.getPlayer().addExcluded(slea.readInt());
		}
	}
}