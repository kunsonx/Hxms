package net.sf.odinms.net;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

public enum RecvPacketOpcode implements WritableIntValueHolder {

	PONG, STRANGE_DATA,
	// LOGIN
	LOGIN_START, // 083的
	LOGIN_PASSWORD, GUEST_LOGIN, LICENSE_REQUEST, SERVERLIST_REREQUEST, CHARLIST_REQUEST, SERVERSTATUS_REQUEST, SET_GENDER, CHAR_SELECT, CHECK_CHAR_NAME, CREATE_CHAR, ERROR_LOG, RELOG, TOUCHING_CSup,
	// CHANNEL
	PLAYER_LOGGEDIN, CHANGE_MAP, CHANGE_CHANNEL, CHOU_JIANG, // 迷之蛋抽奖的
	ENTER_CASH_SHOP, MOVE_PLAYER, CANCEL_CHAIR, USE_CHAIR, CLOSE_RANGE_ATTACK, RANGED_ATTACK, MAGIC_ATTACK, PASSIVE_ENERGY, TAKE_DAMAGE, GENERAL_CHAT, CLOSE_CHALKBOARD, FACE_EXPRESSION, USE_ITEMEFFECT, NPC_TALK, NPC_TALK_MORE, NPC_SHOP, STORAGE, HIRED_MERCHANT_REQUEST, DUEY_ACTION, ITEM_SORT, // 物品集合
	ITEM_SORT2, // 物品排序
	ITEM_MOVE, // 物品移动
	USE_ITEM, CANCEL_ITEM_EFFECT, USE_FISHING_ITEM, USE_SUMMON_BAG, PET_FOOD, USE_MOUNT_FOOD, USE_CASH_ITEM, USE_CATCH_ITEM, USE_SKILL_BOOK, USE_RETURN_SCROLL, MAKER_SKILL, USE_UPGRADE_SCROLL, DISTRIBUTE_AP, DISTRIBUTE_AUTO_AP, HEAL_OVER_TIME, DISTRIBUTE_SP, SPECIAL_MOVE, CANCEL_BUFF, SKILL_EFFECT, MESO_DROP, GIVE_FAME, CHAR_INFO_REQUEST, ARAN_COMBO, DRAGON_MOVE, SPAWN_PET, CANCEL_DEBUFF, CHANGE_MAP_SPECIAL, USE_INNER_PORTAL, TROCK_ADD_MAP, // 缩地石
	QUEST_ACTION, SKILL_MACRO, TREASUER_CHEST, MULTI_CHAT, WHISPER, SPOUSE_CHAT, MESSENGER, PLAYER_INTERACTION, // 有交易
																												// 。
	PARTY_OPERATION, DENY_PARTY_REQUEST, GUILD_OPERATION, DENY_GUILD_REQUEST, BUDDYLIST_MODIFY, NOTE_ACTION, USE_DOOR, CHANGE_KEYMAP, RING_ACTION, OPEN_FAMILY, ADD_FAMILY, ACCEPT_FAMILY, USE_FAMILY, ALLIANCE_OPERATION, BBS_OPERATION, ENTER_MTS, SOLOMON, MOVE_PET, PET_CHAT, PET_COMMAND, PET_LOOT, PET_AUTO_POT, SUMMON_SKILL, MOVE_SUMMON, SUMMON_ATTACK, DAMAGE_SUMMON, MOVE_LIFE, AUTO_AGGRO, MOB_DAMAGE_MOB, MONSTER_BOMB, NPC_ACTION, ITEM_PICKUP, HYPNOTIZE, DAMAGE_REACTOR, TOUCH_REACTOR, PARTY_SEARCH_REGISTER, PARTY_SEARCH_START, PLAYER_UPDATE, TOUCHING_CS, CASHGIFT, COUPON_CODE, MAPLETV,
	// MTS_OP,
	SCRIPTED_ITEM, REVIVE_ITEM, SUMMON_TALK, VICIOUS_HAMMER, CHATROOM_SYSTEM, DragonBall1, DragonBall2, USE_STAR_SCROLL, USE_POTENTIAL_SCROLL, USE_MAGNIFLER_SCROLL, MONSTER_BOMB_SKILL, // 灵魂助力
																																															// 机器人工厂
	SPECIAL_SUMMON2, // 人造卫星
	SPECIAL_SKILL_EFFECT, // 机械技能特效
	SPECIAL_DOOR, // 机械师传送门
	BEANS_GAME1, // 豆豆机点[start]
	BEANS_GAME2, // 豆豆机点[退出]
	UNKNOW_SKILL, // 在加技能时候出现的包 长度6位 包头后面是未知的int
	CHECK_TIME, // 时间检测包[10 15 30 35秒都可能有 估计是盛大用来检测整点开放的活动/任务的]
	PLAYER_COMMAND, // 玩家命令。
	CHRONOSPHERING, // 超时空传送
	CHANGE_ROOM, // 切换房间
	MOVE_ANDROID, FACE_ANDROID, MARRIED_COMPLETE, // 结婚完成
	CHANGE_CMD, ATTACKEFFECT, OPEN_POWER;
	private int code = -2;

	public void setValue(int code) {
		this.code = code;
	}

	@Override
	public int getValue() {
		return code;
	}

	public static Properties getDefaultProperties()
			throws FileNotFoundException, IOException {
		/*
		 * ResourceBundle resb1 = ResourceBundle.getBundle("recvops",
		 * Locale.SIMPLIFIED_CHINESE);
		 */
		Properties props = new Properties();
		props.load(Class.class.getResourceAsStream("/recvops.properties"));
		System.out.println(props.getProperty("VERSION"));
		/*
		 * for (String key : resb1.keySet()) { props.put(key,
		 * resb1.getString(key)); }
		 */
		return props;
	}

	static {
		try {
			ExternalCodeTableGetter.客户端包头获取(getDefaultProperties(), values());
		} catch (IOException e) {
			throw new RuntimeException("Failed to load recvops", e);
		}
	}
}