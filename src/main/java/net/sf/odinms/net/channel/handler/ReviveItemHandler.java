/*
	
 */

package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ReviveItemHandler extends AbstractMaplePacketHandler {

	private static int item;
	private static Point position;

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		item = slea.readInt();
		int x = slea.readInt();
		int y = slea.readInt();

		position = new Point(x, y);
	}

	public static int getItemId() {
		return item;
	}

	public static Point getPosition() {
		return position;
	}
}