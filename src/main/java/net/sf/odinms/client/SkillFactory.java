package net.sf.odinms.client;

import net.sf.odinms.provider.MapleSkillInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.sf.odinms.client.skills.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.Element;
import org.apache.log4j.Logger;

public class SkillFactory {

	private static Map<Integer, Skill> skills = new HashMap<Integer, Skill>();
	private static Logger log = Logger.getLogger(SkillFactory.class);

	public static void InitCache() {

		PreparedStatement ps;
		try {
			Connection c = DatabaseConnection.getConnection();
			ps = c.prepareStatement("SELECT\n" + "skills.skillid\n" + "FROM\n"
					+ "skills");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				SkillInfoManager.getSkill(rs.getInt("skillid"));
			}
			rs.close();
			ps.close();
			c.close();
		} catch (SQLException ex) {
			log.debug("初始化技能缓存失败：" + ex.getMessage());
		}
		log.info("已初始化技能缓存...");
	}

	/*
	 * public static ISkill getSkill(int id) { ISkill ret = (ISkill)
	 * skills.get(Integer.valueOf(id)); int jobid = id / 10000; MapleData
	 * skillData; if (ret == null) { if (jobid >= 1000 && jobid <= 2218) {
	 * skillData = datasource.getData(getLeftPaddedStr(String.valueOf(id /
	 * 10000), '0', 4) + ".img").getChildByPath("skill/" +
	 * getLeftPaddedStr(String.valueOf(id), '0', 8)); } else { skillData =
	 * datasource.getData(getLeftPaddedStr(String.valueOf(id / 10000), '0', 3) +
	 * ".img").getChildByPath("skill/" + getLeftPaddedStr(String.valueOf(id),
	 * '0', 7)); } if (skillData != null) { ret = loadFromData(id, skillData);
	 * skills.put(Integer.valueOf(id), ret); } else { return null; } } return
	 * ret; }
	 */
	public static Skill getSkill(int id) {
		if (skills.containsKey(id)) {
			return skills.get(id);
		} else {
			Skill ret = null;
			MapleSkillInfo info = SkillInfoManager.getSkill(id);
			if (info != null) {
				ret = loadFromData(id, info);
			}
			skills.put(id, ret);
			return ret;
		}
	}

	public static Integer[] getSkills(int jobid) {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("select id from wz_sd where jobid = ?");
			ps.setInt(1, jobid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
			rs.close();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return ids.toArray(new Integer[0]);
	}

	public static String getSkillName(int id) {
		Skill skill = getSkill(id);
		return skill != null ? skill.getName() : null;
	}

	public static Skill loadFromData(int id, MapleData data) {
		// System.out.println("正在解析的技能id:"+id);
		// System.out.println("正在解析的技能名字:"+getSkillName(id));
		Skill ret = new Skill(id);
		ret.setName(data.getName());
		boolean isBuff = false;
		int skillType = MapleDataTool.getInt("skillType", data, -1);
		String elem = MapleDataTool.getString("elemAttr", data, null);
		ret.masterLevel = MapleDataTool.getInt("masterLevel", data, 0);
		ret.isCommon = (data.getChildByPath("common") != null);

		if (ret.isCommon) {
			ret.maxLevel = MapleDataTool.getInt(data.getChildByPath("common")
					.getChildByPath("maxLevel"), 1);
		}

		if (elem != null) {
			ret.element = Element.getFromChar(elem.charAt(0));
		} else {
			ret.element = Element.PHYSICAL;
		}

		MapleData infos = data.getChildByPath("info");
		if (infos != null && infos.getChildren() != null) {
			for (MapleData object : infos.getChildren()) {
				ret.infos.put(object.getName(), object.getData().toString());
			}
		}

		MapleData effect = data.getChildByPath("effect");
		if (skillType != -1) {
			if (skillType == 2 || id == 风灵使者.终极弓 || id == 魂骑士.终极剑) {
				isBuff = true;
			}
		} else {
			MapleData action = data.getChildByPath("action");
			MapleData hit = data.getChildByPath("hit");
			MapleData ball = data.getChildByPath("ball");
			isBuff = (effect != null) && (hit == null) && (ball == null);
			isBuff |= ((action != null) && (MapleDataTool.getString("0",
					action, "").equals("alert2")));

			switch (id) {
			case 1121006: // 突进
			case 1221007: // 突进
			case 1321003: // 突进
			case 1311005: // 龙之献祭
			case 2111002: // 末日烈焰
			case 2111003: // 致命毒雾
			case 2301002: // 群体治愈
			case 3110001: // 贯穿箭
			case 3210001: // 贯穿箭
			case 4101005: // 生命吸收
			case 4111003: // 影网术
			case 4201004: // 神通术
			case 4221006: // 烟幕弹
			case 1121001: // 磁石
			case 1221001: // 磁石
			case 1321001: // 磁石
			case 5201006: // 激退射杀
			case 5111004: // 能量耗转
			case 12111005: // 火牢术屏障
			case 14101006: // 吸血
			case 14111001: // 影网术
			case 14111006: // 毒炸弹
				isBuff = false;
				break;
			case 1001: // 团队治疗
			case 1002: // 疾风步
			case 1004: // 骑兽技能
			case 1005: // 英雄之回声
				// 战士
			case 1001003: // 圣甲术
				// 英雄技能
			case 1101004: // 快速剑
			case 1101006: // 愤怒之火
			case 1101007: // 伤害反击
			case 1111002: // 斗气集中
			case 1111007: // 防御崩坏
			case 1121000: // 冒险岛勇士
			case 1121002: // 稳如泰山
			case 1121010: // 葵花宝典
			case 1121011: // 勇士的意志
				// 圣骑士
			case 1201004: // 快速剑
			case 1201005: // 快速钝器
			case 1201006: // 压制术
			case 1201007: // 伤害反击
			case 1211004: // 火焰冲击
			case 1211006: // 寒冰冲击
			case 1211008: // 雷鸣冲击
			case 1211009: // 魔击无效
			case 1220013: // 祝福护甲
			case 1221000: // 冒险岛勇士
			case 1221002: // 稳如泰山
			case 1221003: // 圣灵之剑
			case 1221004: // 圣灵之锤
			case 1221012: // 勇士的意志
				// 黑骑士
			case 1301006: // 极限防御
			case 1301007: // 神圣之火
			case 1311006: // 龙咆哮
			case 1311007: // 魔击无效
			case 1311008: // 龙之魂
			case 1321000: // 冒险岛勇士
			case 1321002: // 稳如泰山
			case 1321007: // 灵魂助力
			case 1320008: // 灵魂治愈
			case 1320009: // 灵魂祝福
			case 1321010: // 勇士的意志
				// 法师
			case 2001002: // 魔法盾
			case 2001003: // 魔法铠甲
				// 火毒导师
			case 2101001: // 精神力
			case 2101003: // 缓速术
			case 2111004: // 封印术
			case 2111005: // 魔法狂暴
			case 2111007: // 快速移动精通
			case 2111008: // 自然力重置
			case 2121000: // 冒险岛勇士
				// case 2121002: // 魔法反击
			case 2121003: // 迷雾爆发
			case 2121004: // 终极无限
			case 2121005: // 冰破魔兽
			case 2121008: // 勇士的意志
				// 冰雷导师
			case 2201001: // 精神力
			case 2201003: // 缓速术
			case 2211004: // 封印术
			case 2211005: // 魔法狂暴
			case 2211007: // 快速移动精通
			case 2211008: // 自然力重置
			case 2221000: // 冒险岛勇士
				// case 2221002: // 魔法反击
			case 2221004: // 终极无限
			case 2221005: // 火魔兽
			case 2221008: // 勇士的意志
			case 2221009: // 冰河锁链
				// 主教
			case 2301003: // 神之保护
			case 2301004: // 祝福
			case 2311001: // 净化
			case 2311003: // 神圣祈祷
			case 2311005: // 巫毒术
			case 2311006: // 圣龙召唤
			case 2311007: // 快速移动精通
			case 2321000: // 冒险岛勇士
				// case 2321002: // 魔法反击
			case 2321003: // 强化圣龙
			case 2321004: // 终极无限
			case 2321005: // 圣灵之盾
			case 2321009: // 勇士的意志
				// 弓箭手
			case 3001003: // 集中术
				// 神射手
			case 3101002: // 快速箭
			case 3101004: // 无形箭
			case 3111002: // 替身术
			case 3111005: // 银鹰召唤
			case 3121000: // 冒险岛勇士
			case 3121002: // 火眼晶晶
			case 3121006: // 火凤凰
			case 3121008: // 集中精力
			case 3121009: // 勇士的意志
				// 游侠
			case 3201002: // 快速弩
			case 3201004: // 无形箭
			case 3211002: // 替身术
			case 3211005: // 金鹰召唤
			case 3221000: // 冒险岛勇士
			case 3221002: // 火眼晶晶
			case 3221005: // 冰凤凰
			case 3221006: // 刺眼箭
			case 3221008: // 勇士的意志
				// 飞侠
			case 4001003: // 隐身术
				// 隐士
			case 4101003: // 快速暗器
			case 4101004: // 轻功
			case 4111001: // 聚财术
			case 4111002: // 影分身
			case 4111007: // 黑暗杂耍
			case 4121000: // 冒险岛勇士
			case 4121004: // 忍者伏击
			case 4121006: // 暗器伤人
			case 4121009: // 勇士的意志
				// 侠盗
			case 4201002: // 快速短刀
			case 4201003: // 轻功
			case 4211003: // 敛财术
			case 4211005: // 金钱护盾
			case 4211007: // 黑暗杂耍
			case 4211008: // 影分身
			case 4211009: // 二段跳
			case 4221000: // 冒险岛勇士
			case 4221004: // 忍者伏击
			case 4221008: // 勇士的意志
				// 海盗
			case 5001005: // 疾驰
				// 冲锋队长
			case 5101006: // 急速拳
			case 5101007: // 橡木伪装
			case 5110001: // 能量获得
			case 5111005: // 超人变形
			case 5121000: // 冒险岛勇士
			case 5121003: // 超级变身
			case 5121008: // 勇士的意志
			case 5121009: // 极速领域
				// 船长
			case 5211001: // 章鱼炮台
			case 5211002: // 海鸥空袭
			case 5221000: // 冒险岛勇士
			case 5220002: // 超级章鱼炮台
			case 5221006: // 武装
			case 5221010: // 勇士的意志
				// 管理员
			case 9001001: // 上乘轻功
			case 9001002: // 神圣祈祷
			case 9001003: // 枫印祝福
			case 9001004: // 隐藏术
			case 9001008: // 神圣之火
				// 骑士团
			case 10001001: // 团队治疗
			case 10001002: // 疾风步
			case 10001004: // 骑兽技能
			case 10001005: // 英雄之回声
				// 魂骑士
			case 11001001: // 圣甲术
			case 11001004: // 魂精灵
			case 11101001: // 快速剑
			case 11101002: // 终极剑
			case 11101003: // 愤怒之火
			case 11111001: // 斗气集中
			case 11111007: // 灵魂属性
				// 炎术士
			case 12001001: // 魔法盾
			case 12001002: // 魔法铠甲
			case 12001004: // 炎精灵
			case 12101000: // 精神力
			case 12101001: // 缓速术
			case 12101004: // 魔法狂暴
			case 12101005: // 自然力重置
			case 12111002: // 封印术
			case 12111004: // 火魔兽
				// 风灵使者
			case 13001002: // 集中术
			case 13001004: // 风精灵
			case 13101001: // 快速箭
			case 13101002: // 终极弓
			case 13101003: // 无形箭
			case 13101006: // 风影漫步
			case 13111004: // 替身术
			case 13111005: // 信天翁
				// 夜行者
			case 14001003: // 隐身术
			case 14001005: // 夜精灵
			case 14101002: // 快速暗器
			case 14101003: // 轻功
			case 14111000: // 影分身
				// 奇袭者
			case 15001003: // 疾驰
			case 15001004: // 雷精灵
			case 15100004: // 能量获得
			case 15101002: // 急速拳
			case 15101006: // 雷鸣
			case 15111001: // 能量耗转
			case 15111002: // 超人变形
			case 15111005: // 极速领域
			case 15111006: // 闪光击
				// 战神
			case 20001001: // 团队治疗
			case 20001002: // 疾风步
			case 20001004: // 骑兽技能
			case 20001005: // 英雄之回声
			case 20000012: // 精灵的祝福
			case 21000000: // 矛连击强化
			case 21001003: // 快速矛
			case 21100005: // 连环吸血
			case 21101003: // 抗压
			case 21111001: // 灵巧击退
			case 战神.冰雪矛: // 冰雪矛
			case 21121000: // 冒险岛勇士
			case 21120007: // 战神之盾:
			case 21121003: // 战神的意志
			case 21121008: // 勇士的意志
				// 龙神
			case 20011001: // 团队治疗
			case 20011002: // 疾风步
			case 20011004: // 骑兽技能
			case 20011005: // 英雄之回声
			case 22111001: // 魔法盾
			case 22121001: // 自然力重置
			case 22131001: // 魔法屏障
			case 22141002: // 魔法狂暴
			case 龙神.缓速术: // 缓术速
			case 22151003: // 抗魔领域
			case 22161003: // 极光恢复
			case 22171000: // 冒险岛勇士
			case 22171004: // 勇士的意志
			case 龙神.玛瑙的祝福:
			case 龙神.玛瑙的保佑:
			case 龙神.玛瑙的意志:
				// 暗影双刀
			case 4301002: // 快速双刀
			case 4310000: // 恢复术
			case 4311001: // 暗影轻功
			case 4330001: // 进阶隐身术
			case 4331003: // 死亡猫头鹰
			case 4331002: // 镜像分身
			case 4341000: // 冒险岛勇士
			case 4341002: // 终极斩
			case 4341006: // 傀儡召唤
			case 4341007: // 荆棘
			case 4341008: // 勇士的意志
				// 幻灵斗师
			case 30001001: // 潜入
			case 32001003: // 黑暗灵气
			case 32101002: // 蓝色灵气
			case 32101003: // 黄色灵气
			case 32101004: // 伤害吸收
			case 32101005: // 快速长杖
			case 32110000: // 进阶蓝色灵气
			case 32111004: // 转化
			case 32111005: // 霸体
			case 32111006: // 重生
			case 32111010: // 快速移动精通
			case 32121003: // 飓风
			case 32121005: // 稳如泰山
			case 32121007: // 冒险岛勇士
			case 32121008: // 勇士的意志
			case 32120000: // 进阶黑暗灵气
			case 32120001: // 进阶黄色灵气
				// 豹弩游侠
			case 33001001: // 美洲豹骑士
			case 33001003: // 快速弩
			case 33111003: // 野性陷阱
				// 机械师
			case 35001001: // 火焰喷射器
			case 35101009: // 强化火焰喷射器
			case 35001002: // 金属机甲：原型
			case 35101006: // 机械加速
			case 35101007: // 完美机甲
			case 35111001: // 人造卫星
			case 35111009: // 人造卫星
			case 35111010: // 人造卫星
			case 35111004: // 金属机甲：重机枪
			case 35111002: // 磁场
			case 35111005: // 加速器：EX-7
			case 35111011: // 治疗机器人：H-LX
			case 35121003: // 战争机器：泰坦
			case 35121005: // 金属机甲：导弹战车
			case 35121006: // 卫星防护
			case 35121007: // 冒险岛勇士
			case 35121008: // 勇士的意志
			case 35121009: // 机器人工厂：RM1
			case 35121010: // 放大器：AF-11
				// case 35121011: // 机器人工厂召唤技能
			case 机械师.金属机甲_重机枪_4转:
			case 拳手.幸运骰子:
			case 枪手.幸运骰子:
			case 机械师.幸运骰子:
				isBuff = true;
				break;
			}
		}

		if (data.getChildByPath("keydown") != null) {
			ret.hasCharge = true;
		}

		if (!ret.isCommon) // 老的函数
		{
			for (MapleData level : data.getChildByPath("level")) {
				ret.effects.add(MapleStatEffect.loadSkillEffectFromData(level,
						id, 0, true, isBuff));
			}
		} else // 新的xml结构
		{
			for (int level = 1; level <= ret.maxLevel; ++level) {
				ret.effects
						.add(MapleStatEffect.loadSkillEffectFromData(
								data.getChildByPath("common"), id, level,
								false, isBuff));
			}
		}

		ret.animationTime = MapleDataTool.getInt("effect/animationTime", data,
				0);
		;

		return ret;
	}
}
