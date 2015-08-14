/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */
public class ChangeRoomHandler extends AbstractMaplePacketHandler {

	private static Logger log = Logger.getLogger(ChangeRoomHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		int channel = slea.readByte() + 1;
		int room = (slea.readByte() & 0xFF) - 0x80;
		if (log.isDebugEnabled()) {
			log.debug("切换房间：" + channel + "线." + room + "洞.");
		}
		c.getPlayer().saveToDB();
		c.getSession().write(MaplePacketCreator.enableActions());
		c.getPlayer().弹窗("不支持该功能。");
	}
}
