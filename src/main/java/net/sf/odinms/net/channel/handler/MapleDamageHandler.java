/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import java.util.Collections;
import java.util.List;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharAttribute;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.net.channel.MaplePvp;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

/**
 *
 * @author hxms
 */
public class MapleDamageHandler {

	/**
	 * 在攻击时处理显示狂龙蓄气槽
	 *
	 * @param player
	 */
	public static void ShowKuangLongNuQiStatsHandler(MapleCharacter player) {
		if (player.equalsJob(MapleJob.KuangLong_4)
				&& !player.getBuffManager().hasBuff(61120008)) {
			Integer combo = player.getAttribute().getDataValue(
					MapleCharAttribute.KUANGLONGCOMBO);
			if (combo == null) {
				combo = 0;
			} else if (combo > 1000) {
				combo = 1000;
				player.getAttribute().setDataValue(
						MapleCharAttribute.KUANGLONGCOMBO, combo);
			}
			player.getClient()
					.getSession()
					.write(MaplePacketCreator.giveBuff(0, 0, Collections
							.singletonList(Pair.Create(MapleBuffStat.狂龙蓄气,
									combo)), player));
		}
	}

	/**
	 * 处理添加狂龙攻击次数所得怒气
	 *
	 * @param attackCount
	 */
	public static void GainKuangLongNuQiHandler(int attackCount,
			MapleCharacter player) {
		if (player.equalsJob(MapleJob.KuangLong_4)
				&& !player.getBuffManager().hasBuff(61120008)) {
			Integer combo = player.getAttribute().getDataValue(
					MapleCharAttribute.KUANGLONGCOMBO);
			if (combo == null) {
				combo = 0;
			}
			if (combo < 1000) {
				combo += attackCount * 2;

				player.getAttribute().setDataValue(
						MapleCharAttribute.KUANGLONGCOMBO, combo);
			}
		}
	}

	/**
	 * 处理调用 MaplePvp
	 */
	public static void MaplePvpSystemHandler(
			DamageParseHandler.AttackInfo attack, MapleCharacter player,
			MapleMap map) {
		// -------------------PK开始
		if ((attack.skill != 2301002 && attack.skill != 4201004 && attack.skill != 1111008)
				&& (MaplePvp.PVP_MAP == 0 || MaplePvp.PVP_MAP == player
						.getMapId())
				&& player.getClient().getChannel() == MaplePvp.PVP_CHANNEL) {
			MaplePvp.doPvP(player, map, attack);
		}
		// -------------------PK结束
	}

	/**
	 * 重新计算所有对单个怪物伤害。
	 */
	public static int CalcAllDamage(List<Integer> oned) {
		int totDamageToOneMonster = 0;
		for (Integer eachd : oned) {
			if (totDamageToOneMonster + eachd > 0) {
				totDamageToOneMonster += eachd.intValue();
			}
			// totDamageToOneMonster += eachd.intValue();
		}
		/*
		 * if (GameConstants.秋秋冒险岛 && totDamageToOneMonster > 40) {
		 * totDamageToOneMonster /= 40; } else if (totDamageToOneMonster > 5) {
		 */
		totDamageToOneMonster = totDamageToOneMonster > 5 ? totDamageToOneMonster / 5
				: totDamageToOneMonster;
		/*
		 * if (totDamageToOneMonster > 5) { totDamageToOneMonster /= 5; }
		 */
		// }
		return totDamageToOneMonster;
	}
}
