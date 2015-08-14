/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Admin
 */
public class AndroidEmotionHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		int emote = slea.readInt();
		MapleCharacter chr = c.getPlayer();
		if (emote > 0 && chr != null && chr.getMap() != null && !chr.isHidden()
				&& emote <= 17 && chr.getAndroid() != null) { // O_o
			chr.getMap().broadcastMessage(
					MaplePacketCreator.showAndroidEmotion(chr.getId(), emote));
		}
	}
}
