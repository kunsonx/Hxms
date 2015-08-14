/*
 玩家移动
 */
package net.sf.odinms.net.channel.handler;

import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.movement.AbsoluteLifeMovement;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MovePlayerHandler extends MovementParse {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(MovePlayerHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// log.debug("人物移动封包"+slea.toString());
		slea.skip(17);
		List<LifeMovementFragment> res = parseMovement(slea);// 解析移动的动作语法
		c.getPlayer().setLastRes(res);
		if (res != null) {
			if (slea.available() != 18) {
				log.debug("slea.available != 18 (movement parsing error)");
				// return;
			}
			MapleCharacter player = c.getPlayer();
			MovePlayerHandler(res, player, false);
		}
		// c.checkLogin();
	}

	public static void MovePlayerHandler(List<LifeMovementFragment> res,
			MapleCharacter player, boolean fromdamage) {
		try {
			MaplePacket packet = MaplePacketCreator.movePlayer(player.getId(),
					res);
			if (!player.isHidden()) { // 如果玩家不是处于GM隐身状态
				player.getMap().broadcastMessage(player, packet, false);
			} else {
				for (MapleCharacter toplayer : player.getMap().getCharacters()) {
					if ((toplayer.isHidden() || (toplayer.getGm() > 0))
							&& toplayer != player) {
						toplayer.getClient().getSession().write(packet);
					}
				}
			}
			if (CheatingOffense.FAST_MOVE.isEnabled()
					|| CheatingOffense.HIGH_JUMP.isEnabled()) {
				checkMovementSpeed(player, res);
			}
			updatePosition(res, player, 0);
			if (!fromdamage) {
				player.getMap().movePlayer(player, player.getPosition());
			}
		} catch (Exception e) {
			log.warn("玩家移动失败 (" + player.getName() + ")", e);
		}
	}

	private static void checkMovementSpeed(MapleCharacter chr,
			List<LifeMovementFragment> moves) {
		double playerSpeedMod = chr.getSpeedMod() + 0.005;
		boolean encounteredUnk0 = false;
		for (LifeMovementFragment lmf : moves) {
			if (lmf.getClass() == AbsoluteLifeMovement.class) {
				final AbsoluteLifeMovement alm = (AbsoluteLifeMovement) lmf;
				double speedMod = Math.abs(alm.getPixelsPerSecond().x) / 125.0;
				if (speedMod > playerSpeedMod) {
					if (alm.getUnk() == 0) { // to prevent FJ fucking us
						encounteredUnk0 = true;
					}
					if (!encounteredUnk0) {
						if (speedMod > playerSpeedMod) {
							chr.getCheatTracker().registerOffense(
									CheatingOffense.FAST_MOVE);
						}
					}
				}
			}
		}
	}
}