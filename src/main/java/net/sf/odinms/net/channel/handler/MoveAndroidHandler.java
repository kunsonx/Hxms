/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Admin
 */
public class MoveAndroidHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		MapleCharacter chr = c.getPlayer();
		slea.skip(8);
		final List<LifeMovementFragment> res = MovementParse
				.parseMovement(slea);

		if (res != null && chr != null && !res.isEmpty()
				&& chr.getMap() != null && chr.getAndroid() != null) { // map
																		// crash
																		// hack
			final Point pos = new Point(chr.getAndroid().getPos());
			chr.getAndroid().updatePosition(res);
			chr.getMap().broadcastMessage(chr,
					MaplePacketCreator.moveAndroid(chr.getId(), pos, res),
					false);
		}
	}
}
