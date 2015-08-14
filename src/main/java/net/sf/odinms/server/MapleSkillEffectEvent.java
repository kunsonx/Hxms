/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharAttribute;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

/**
 *
 * @author hxms
 */
public class MapleSkillEffectEvent {

	public static class EffectEventData {

		protected List<Pair<MapleBuffStat, Integer>> statups;
		protected int duration;

		public EffectEventData(List<Pair<MapleBuffStat, Integer>> statups) {
			this.statups = statups;
		}

		public EffectEventData(List<Pair<MapleBuffStat, Integer>> statups,
				int duration) {
			this.statups = statups;
			this.duration = duration;
		}

		public List<Pair<MapleBuffStat, Integer>> getStatups() {
			return statups;
		}

		public int getDuration() {
			return duration;
		}
	}

	/**
	 * 在应用BUFF时事件。
	 *
	 * @param applyfrom
	 * @param applyto
	 * @param primary
	 */
	public static void OnApplyBuffEffect(MapleStatEffect effect,
			final MapleCharacter applyfrom, final MapleCharacter applyto,
			boolean primary, EffectEventData effectEventData) {
		switch (effect.getSourceId()) {
		case 3120006:// 火连。
			MapleSkillEffectHelp.OnApplyBuffEffect_3120006_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 3220005: // 冰连。
			MapleSkillEffectHelp.OnApplyBuffEffect_3220005_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 2121004:// 终极无限
		case 2221004:// 终极无限
		case 2321004: // 终极无限
			MapleSkillEffectHelp.OnApplyBuffEffect_终极无限_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 2022125:// 灵魂祝福-物理防御力
			MapleSkillEffectHelp.OnApplyBuffEffect_2022125_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 2022126:// 灵魂祝福-魔法防御力
			MapleSkillEffectHelp.OnApplyBuffEffect_2022126_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 2022127:// 灵魂祝福-命中
			MapleSkillEffectHelp.OnApplyBuffEffect_2022127_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 2022128:// 灵魂祝福-回避
			MapleSkillEffectHelp.OnApplyBuffEffect_2022128_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 2022129:// 灵魂祝福-攻击
			MapleSkillEffectHelp.OnApplyBuffEffect_2022129_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 4221013:
			MapleSkillEffectHelp.OnApplyBuffEffect_4221013_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		case 61120008: // 终极变身
			MapleSkillEffectHelp.OnApplyBuffEffect_61120008_(effect, applyfrom,
					applyto, primary, effectEventData);
			break;
		}

	}
}
