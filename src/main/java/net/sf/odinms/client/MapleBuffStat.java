package net.sf.odinms.client;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

public enum MapleBuffStat implements Serializable, Buffstat {

	WATK(1, 0), // 物攻(葵花宝典、愤怒火) 106
	WDEF(1, 1), // 防御(圣甲术、铠甲) 106 0x300000000L
	MATK(1, 2), // 魔攻(精神力等) 1066666666
	MDEF(1, 3), // 魔法防御 106 0x800000000L
	ACC(1, 4), // 命中率 106
	AVOID(1, 5), // 回避率 106
	HANDS(1, 6), // 手技
	SPEED(1, 7), // 速度 疾风步 106
	JUMP(1, 8), // 跳跃力(轻功等) 106
	MAGIC_GUARD(1, 9), // 魔法盾 106
	DARKSIGHT(1, 10), // 隐身术 106
	BOOSTER(1, 11), // 快速矛 快速武器 快速箭 魔法狂暴等快速技能 106
	POWERGUARD(1, 12), // 伤害反弹 106
	HYPERBODYHP(1, 13), // 神圣之火HP 106
	HYPERBODYMP(1, 14), // 神圣之火MP 106
	INVINCIBLE(1, 15), // 神之保护 106
	SOULARROW(1, 16), // 无形箭 106
	STUN(1, 17), // 击晕效果 106
	POISON(1, 18), // 毒效果 106
	SEAL(1, 19), // 封印效果 106
	DARKNESS(1, 20), // 暗黑 106
	COMBO(1, 21), // 斗气集中 106
	WK_CHARGE(1, 22), // 寒冰冲击 火焰冲击 雷鸣冲击 冰雪矛 106
	DRAGONBLOOD(1, 23), // 龙之魂 持续-HP 加攻击需要另外的BUFF 092
	HOLY_SYMBOL(1, 24), // 神圣祈祷 106 加经验
	MESOUP(1, 25), // 聚财术
	SHADOWPARTNER(1, 26), // 影分身 106
	PICKPOCKET(1, 27), // 敛财术 106
	PUPPET(1, 28), // 替身术
	MESOGUARD(1, 28), // 金钱护盾 092
	WEAKEN(1, 30), // 虚弱 106

	MORPH(2, 1), // 变身 橡木伪装 106丢
	RECOVERY(2, 2), // 新手治疗 106
	MAPLE_WARRIOR(2, 3), // 冒险岛勇士 106
	STANCE(2, 4), // 稳如泰山 106
	SHARP_EYES(2, 5), // 火眼晶晶 106
	MANA_REFLECTION(2, 6), // 魔法反击 106
	SHADOW_CLAW(2, 8), // 暗器伤人 106
	INFINITY(2, 9), // 终极无限 106
	进阶祝福(2, 10), // 圣灵之盾 106
	幻影步_回避几率(2, 11), // 幻影步_回避几率 106
	BLIND(2, 12), // 刺眼箭 致盲 106丢
	CONCENTRATE(2, 13), // 集中精力 106
	ECHO_OF_HERO(2, 15), // 英雄的回声
	GHOST_MORPH(2, 17), // 变身药的BUFF
	BERSERK_FURY(2, 27), // 勇士的意志
	闪光击(2, 28), // ????
	骑士团主动终极(2, 30), // 风灵使者的终极弓比较特别 是Buff
	自然力重置(2, 31), // 106

	// 先Mask
	飓风(0, 0), // 092
	潜入(0, 0), // 092
	转化(0, 0), // 092
	重生(0, 0), // 092
	机械师(0, 0), // 机械众多Buff都用这一个值
	灵气(0, 0), // 092
	黑暗灵气(0, 0), // 092
	蓝色灵气(0, 0), // 092
	黄色灵气(0, 0), // 092
	霸体(0, 0), // 092
	灵巧击退(0, 0), // 092
	伤害吸收(0, 0), // 092
	抗压(0, 0), // 092
	魔法屏障(0, 0), // 092
	// 荆棘(0x80000000000000L, 3),//093取消
	缓速术(0, 0), // 龙神的缓速是BUFF 别的职业不是 092
	快速移动精通(0, 0), // 092
	连环吸血(0, 0), // 092
	暴走HP(0, 0), // 提升最大hp 和神圣之火不一样
	暴走攻击(0, 0), // 092 - 0x200000000000000L 提升攻击力上限 同时使豹子出现翅膀
	地雷(0, 0), // 092 - 0x400000000000000L
	吞噬1(0, 0), 吞噬2(0, 0), 吞噬3(0, 0), 吞噬4(0, 0), 吞噬5(0, 0), 死亡猫头鹰(0, 0), 终极斩(0,
			0), 矛连击强化(0, 0), // 参数是连击数 10 20 30 40 50 60 70 80 90 100
	战神之盾(0, 0), 灵魂之石(0, 0), 抗魔领域(0, 0), 战斗命令(0, 0), 祝福护甲_物理攻击力(4, 19), 祝福护甲_防御次数(
			8, 6), // 被动产生的BUFF
	EWatk(0, 0), // 灵魂祝福攻击 变身 //原来是2
	EWdef(0, 0), // 灵魂祝福物防 变身
	EMdef(0, 0), // 灵魂祝福魔防 变身
	风影漫步(0, 0), 完美机甲(0, 0), 卫星防护(0, 0), 骑宠1(0, 0), // 骑宠1 093
	骑宠2(0, 0), // 骑宠2 093
	玛瑙的保佑(0, 0), 玛瑙的意志(0, 0), 祝福(0, 0),
	// 特别处理
	能量获得(0, 0), // 能量获得
	极速领域(0, 0), // 093
	疾驰_龙卷风(0, 0), // 疾驰 龙卷风 虽然要叠加2次 但是 |= 叠加2个相同的值的话数值是不变的 所以直接发总的叠加枚举
	幸运骰子(0, 0), // 多buff 单独处理
	射手_精神连接(0, 0), 幻影步_敏捷(0, 0), E_Watk(0, 0), E_Matk(0, 0), EMaxHp(0, 0), EMaxMp(
			0, 0), EAcc(0, 0), EAvoid(0, 0), EJump(0, 0), ESpeed(0, 0), EAllStat(
			0, 0), 魔力精通(0, 0), 神圣魔法盾(0, 0), 神秘瞄准术(0, 0), // 已失效
	天使戒指(0, 0), 精神注入_伤害(6, 9), 精神注入_暴击(6, 15), 火焰咆哮_攻击力(5, 10, true, 1), 闪耀之光(
			0, 0), 月光祝福_命中率(5, 14, true, 1), 水盾_ASR(6, 4), 水盾_TER(6, 5), 黑暗忍耐_防御力(
			6, 26), 黑暗变形_HP增加(6, 7, true, 1), 黑暗变形_攻击力(7, 3), 水盾_伤害减少(6, 6), 圣歌祈祷_无视防御力(
			7, 25), 古老意志_体力(3, 26), 古老意志_攻击(4, 20), 吸血鬼之触_伤害转化(6, 25), 无限精气_主MASK(
			5, 29), 坐骑状态(10, 27, true), 幻影屏障(7, 27), 灵魂助力(0, 0), 龙之力(0, 0), 侠盗本能_击杀点(
			0, 0), 神秘的运气(7, 26), 幸运的保护_HP(7, 3, true, 1), 幸运的保护_MP(7, 19, true,
			1), 巨人药水(4, 25), 灵魂之怒_最大暴击(0, 0), 灵魂之怒_最小暴击(0, 0), 灵魂之怒_伤害增加(0, 0), 狂龙蓄气(
			8, 15), KUANGLONG_BL(6, 15), KUANGLONG_WS(2, 4), KUANGLONG_GJSD(7,
			31, true, 1), KUANGLONG_GJL(7, 8, true, 1), KUANGLONG_JIANBI(8, 20), 攻击上限(
			8, 30, true, 1), 驱邪_伤害减少(6, 4), 驱邪_异常状态抗性(6, 5), 驱邪_所有属性抗性(6, 6), 恶魔恢复_定时恢复百分比(
			9, 27), 恶魔复仇者(8, 13), 尖兵电力(10, 1), 尖兵电池时间(10, 14, true, 1), 神秘代码_BOSS伤害(
			9, 24), 神秘代码_总伤害增加(7, 31, true, 1);
	private int position;
	private int mask;
	private SerializeSpawn serializeSpawn = null;
	private boolean isfoot;
	private int footIndex = -1;
	static final long serialVersionUID = 9179541991234738569L;

	private MapleBuffStat(int position, int mask) {
		this(position, mask, false);
	}

	private MapleBuffStat(int position, int mask, boolean isfoot) {
		this.position = position;
		this.mask = mask;
		this.isfoot = isfoot;
	}

	private MapleBuffStat(int position, int mask, boolean isfoot, int fI) {
		this.position = position;
		this.mask = mask;
		this.isfoot = isfoot;
		this.footIndex = fI;
	}

	public int getFootIndex() {
		return footIndex;
	}

	@Override
	public int getPosition() {
		return position;
	}

	@Override
	public int getMask() {
		return mask;
	}

	private final static List<MapleBuffStat> SpawnStatsList = Arrays.asList(
			巨人药水, SPEED, MORPH, COMBO, SHADOWPARTNER);

	public static List<MapleBuffStat> getSpawnList() {
		return SpawnStatsList;
	}

	public static List<MapleBuffStat> getSpawnList(MapleCharacter chr) {
		/*
		 * ArrayList<MapleBuffStat> buffs = new ArrayList<MapleBuffStat>(); for
		 * (MapleBuffStat mapleBuffStat : SpawnStatsList) { if
		 * (chr.getEffects().containsKey(mapleBuffStat)) {
		 * buffs.add(mapleBuffStat); } } return buffs;
		 */
		return chr.getBuffManager().getSpawnList(getSpawnList());
	}

	public boolean isFoot() {
		return isfoot;
	}

	@Override
	public int getValue(boolean foreign, boolean give) {
		int value = 1 << mask;
		if (!foreign) {// 加给自己的
			if (give) {// 加
			} else {// 消除
			}
		} else {// 加给别人的。
			if (give) {// 加
				switch (this) {
				case JUMP:
					value = 0;
					break;
				}
			} else {// 消除
			}
		}
		return value;
	}

	public SerializeSpawn getSerializeSpawn() {
		if (serializeSpawn == null) {
			maskserializeSpawn();
		}
		return serializeSpawn;
	}

	private void maskserializeSpawn() {
		switch (this) {
		case SPEED: {
			serializeSpawn = new MapleBuffStat.SerializeSpawn() {
				@Override
				public void Serialize(MaplePacketLittleEndianWriter m,
						MapleCharacter chr) {
					m.write(chr.getBuffedValue(MapleBuffStat.SPEED));
				}
			};
			break;
		}
		case MORPH: {
			serializeSpawn = new MapleBuffStat.SerializeSpawn() {
				@Override
				public void Serialize(MaplePacketLittleEndianWriter m,
						MapleCharacter chr) {
					m.writeShort(chr.getBuffedValue(MapleBuffStat.MORPH)); // 变身的id
																			// 1001
					m.writeInt(chr.getBuffSource(MapleBuffStat.MORPH)); // skillid
				}
			};
			break;
		}
		case SHADOWPARTNER: {
			serializeSpawn = new MapleBuffStat.SerializeSpawn() {
				@Override
				public void Serialize(MaplePacketLittleEndianWriter m,
						MapleCharacter chr) {
					m.writeShort(chr
							.getBuffedValue(MapleBuffStat.SHADOWPARTNER));
					m.writeInt(chr.getBuffSource(MapleBuffStat.SHADOWPARTNER)); // skillid
				}
			};
			break;
		}
		case 射手_精神连接: {
			serializeSpawn = new MapleBuffStat.SerializeSpawn() {
				@Override
				public void Serialize(MaplePacketLittleEndianWriter m,
						MapleCharacter chr) {
					m.writeShort((short) chr.getSkillLevel(chr
							.getBuffSource(MapleBuffStat.射手_精神连接)));
					m.writeInt(chr.getBuffSource(MapleBuffStat.射手_精神连接)); // skillid
				}
			};
			break;
		}
		case COMBO: {
			serializeSpawn = new MapleBuffStat.SerializeSpawn() {
				@Override
				public void Serialize(MaplePacketLittleEndianWriter m,
						MapleCharacter chr) {
					m.write(chr.getBuffedValue(MapleBuffStat.COMBO));
				}
			};
			break;
		}
		case 天使戒指: {
			serializeSpawn = new MapleBuffStat.SerializeSpawn() {
				@Override
				public void Serialize(MaplePacketLittleEndianWriter m,
						MapleCharacter chr) {
					m.writeShort(1);
					m.writeInt(-chr.getBuffSource(MapleBuffStat.天使戒指));
				}
			};
			break;
		}
		case 巨人药水: {
			serializeSpawn = new MapleBuffStat.SerializeSpawn() {
				@Override
				public void Serialize(MaplePacketLittleEndianWriter m,
						MapleCharacter chr) {
					m.writeShort(chr.getBuffedValue(MapleBuffStat.巨人药水));
					m.writeInt(-chr.getBuffSource(MapleBuffStat.巨人药水));
				}
			};
			break;
		}
		case DARKSIGHT:
		case 水盾_伤害减少:
			break;
		case 黑暗变形_HP增加: {
			serializeSpawn = new MapleBuffStat.SerializeSpawn() {
				@Override
				public void Serialize(MaplePacketLittleEndianWriter m,
						MapleCharacter chr) {
					m.writeShort(chr.getSkillLevel(chr
							.getBuffSource(MapleBuffStat.黑暗变形_HP增加)));
					m.writeInt(chr.getBuffSource(MapleBuffStat.黑暗变形_HP增加));
				}
			};
			break;
		}
		default:
			break;
		}
	}

	public interface SerializeSpawn {

		void Serialize(MaplePacketLittleEndianWriter mplew, MapleCharacter chr);
	}
}
