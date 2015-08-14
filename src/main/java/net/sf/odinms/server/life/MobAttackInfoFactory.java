package net.sf.odinms.server.life;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;

/**
 *
 * @author Danny (Leifde)
 */
public class MobAttackInfoFactory {

	private static final MobAttackInfoFactory instance = new MobAttackInfoFactory();
	private static MapleDataProvider dataSource = MapleDataProviderFactory
			.getDataProvider(new File(System
					.getProperty("net.sf.odinms.wzpath") + "/Mob.wz"));
	private static Map<Pair<Integer, Integer>, MobAttackInfo> mobAttacks = new ConcurrentHashMap<Pair<Integer, Integer>, MobAttackInfo>();

	public static MobAttackInfoFactory getInstance() {
		return instance;
	}

	public MobAttackInfo getMobAttackInfo(MapleMonster mob, int attack) {
		MobAttackInfo ret = mobAttacks.get(new Pair<Integer, Integer>(Integer
				.valueOf(mob.getId()), Integer.valueOf(attack)));
		if (ret != null) {
			return ret;
		}

		MapleData mobData = dataSource.getData(StringUtil.getLeftPaddedStr(
				Integer.toString(mob.getId()) + ".img", '0', 11));
		if (mobData != null) {
			MapleData infoData = mobData.getChildByPath("info/link");
			if (infoData != null) {
				String linkedmob = MapleDataTool
						.getString("info/link", mobData);
				mobData = dataSource.getData(StringUtil.getLeftPaddedStr(
						linkedmob + ".img", '0', 11));
			}
			final MapleData attackData = mobData.getChildByPath("attack"
					+ (attack + 1) + "/info");
			if (attackData != null) {
				ret = new MobAttackInfo(mob.getId(), attack);
				ret.setDeadlyAttack(attackData.getChildByPath("deadlyAttack") != null);
				ret.setMpBurn(MapleDataTool.getInt("mpBurn", attackData, 0));
				ret.setDiseaseSkill(MapleDataTool.getInt("disease", attackData,
						0));
				ret.setDiseaseLevel(MapleDataTool
						.getInt("level", attackData, 0));
				ret.setMpCon(MapleDataTool.getInt("conMP", attackData, 0));
			}
		}
		mobAttacks.put(new Pair<Integer, Integer>(Integer.valueOf(mob.getId()),
				Integer.valueOf(attack)), ret);

		return ret;
	}
}
