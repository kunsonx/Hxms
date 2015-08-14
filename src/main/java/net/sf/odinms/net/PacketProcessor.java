/*
 包头
 */
package net.sf.odinms.net;

import net.sf.odinms.net.channel.handler.*;
import net.sf.odinms.net.handler.KeepAliveHandler;
import net.sf.odinms.net.login.handler.*;

public final class PacketProcessor {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(PacketProcessor.class);

	public enum Mode {

		LOGINSERVER, CHANNELSERVER
	};

	private static PacketProcessor instance;
	private MaplePacketHandler[] handlers;

	private PacketProcessor() {
		int maxRecvOp = 0;
		for (RecvPacketOpcode op : RecvPacketOpcode.values()) {
			if (op.getValue() > maxRecvOp) {
				maxRecvOp = op.getValue();
			}
		}
		handlers = new MaplePacketHandler[maxRecvOp + 1];
	}

	public MaplePacketHandler getHandler(short packetId) {
		if (packetId > handlers.length) {
			return null;
		}
		MaplePacketHandler handler = handlers[packetId];
		if (handler != null) {
			return handler;
		}
		return null;
	}

	public void registerHandler(RecvPacketOpcode code,
			MaplePacketHandler handler) {
		try {
			handlers[code.getValue()] = handler;
		} catch (ArrayIndexOutOfBoundsException aiobe) {
			// log.info("客户端包头:"+code.name()+"不存在");
		}
	}

	public synchronized static PacketProcessor getProcessor(Mode mode) {
		if (instance == null) {
			instance = new PacketProcessor();
			instance.reset(mode);
		}
		return instance;
	}

	public void reset(Mode mode) {
		handlers = new MaplePacketHandler[handlers.length];
		registerHandler(RecvPacketOpcode.PONG, new KeepAliveHandler());
		if (mode == Mode.LOGINSERVER) {
			registerHandler(RecvPacketOpcode.LOGIN_START,
					new LoginStartHandler());
			registerHandler(RecvPacketOpcode.LOGIN_PASSWORD,
					new LoginPasswordHandler());
			registerHandler(RecvPacketOpcode.CHARLIST_REQUEST,
					new CharlistRequestHandler());
			registerHandler(RecvPacketOpcode.SERVERSTATUS_REQUEST,
					new ServerStatusRequestHandler());
			registerHandler(RecvPacketOpcode.LICENSE_REQUEST,
					new LicenseRequest());
			registerHandler(RecvPacketOpcode.SET_GENDER, new SetGenderHandler());
			registerHandler(RecvPacketOpcode.CHAR_SELECT,
					new CharSelectedHandler());
			registerHandler(RecvPacketOpcode.CHECK_CHAR_NAME,
					new CheckCharNameHandler());
			registerHandler(RecvPacketOpcode.CREATE_CHAR,
					new CreateCharHandler());
			registerHandler(RecvPacketOpcode.PLAYER_UPDATE, new UpdateHandler());
			registerHandler(RecvPacketOpcode.ERROR_LOG, new ErrorLogHandler());
			registerHandler(RecvPacketOpcode.RELOG, new RelogRequestHandler());
		} else if (mode == Mode.CHANNELSERVER) {
			registerHandler(RecvPacketOpcode.PLAYER_LOGGEDIN,
					new PlayerLoggedinHandler());
			registerHandler(RecvPacketOpcode.ADD_FAMILY, new FamilyAddHandler());
			registerHandler(RecvPacketOpcode.USE_FAMILY, new FamilyUseHandler());
			registerHandler(RecvPacketOpcode.ACCEPT_FAMILY,
					new AcceptFamilyHandler());
			registerHandler(RecvPacketOpcode.CHANGE_MAP, new ChangeMapHandler());
			registerHandler(RecvPacketOpcode.CHANGE_CHANNEL,
					new ChangeChannelHandler());
			registerHandler(RecvPacketOpcode.CHANGE_ROOM,
					new ChangeRoomHandler());
			registerHandler(RecvPacketOpcode.ENTER_CASH_SHOP,
					new EnterCashShopHandler());
			registerHandler(RecvPacketOpcode.MOVE_PLAYER,
					new MovePlayerHandler());
			registerHandler(RecvPacketOpcode.CANCEL_CHAIR,
					new CancelChairHandler());
			registerHandler(RecvPacketOpcode.USE_CHAIR, new UseChairHandler());
			registerHandler(RecvPacketOpcode.CLOSE_RANGE_ATTACK,
					new CloseRangeDamageHandler());
			registerHandler(RecvPacketOpcode.RANGED_ATTACK,
					new RangedAttackHandler());
			registerHandler(RecvPacketOpcode.MAGIC_ATTACK,
					new MagicDamageHandler());
			registerHandler(RecvPacketOpcode.PASSIVE_ENERGY,
					new PassiveEnergyHandler());
			registerHandler(RecvPacketOpcode.TAKE_DAMAGE,
					new TakeDamageHandler());
			registerHandler(RecvPacketOpcode.GENERAL_CHAT,
					new GeneralchatHandler());
			registerHandler(RecvPacketOpcode.CLOSE_CHALKBOARD,
					new CloseChalkboardHandler());
			registerHandler(RecvPacketOpcode.FACE_EXPRESSION,
					new FaceExpressionHandler());
			registerHandler(RecvPacketOpcode.USE_ITEMEFFECT,
					new UseItemEffectHandler());
			registerHandler(RecvPacketOpcode.NPC_TALK, new NPCTalkHandler());
			registerHandler(RecvPacketOpcode.NPC_TALK_MORE,
					new NPCMoreTalkHandler());
			registerHandler(RecvPacketOpcode.NPC_SHOP, new NPCShopHandler());
			registerHandler(RecvPacketOpcode.STORAGE, new StorageHandler());
			registerHandler(RecvPacketOpcode.CHOU_JIANG, new choujiangHandler());
			registerHandler(RecvPacketOpcode.HIRED_MERCHANT_REQUEST,
					new HiredMerchantRequestHandler());
			registerHandler(RecvPacketOpcode.DUEY_ACTION,
					new DueyActionHandler());
			registerHandler(RecvPacketOpcode.ITEM_SORT, new ItemSortHandler());
			registerHandler(RecvPacketOpcode.ITEM_SORT2, new ItemSort2Handler());
			registerHandler(RecvPacketOpcode.ITEM_MOVE, new ItemMoveHandler());
			registerHandler(RecvPacketOpcode.USE_ITEM, new UseItemHandler());
			registerHandler(RecvPacketOpcode.CANCEL_ITEM_EFFECT,
					new CancelItemEffectHandler());
			registerHandler(RecvPacketOpcode.USE_FISHING_ITEM,
					new FishingHandler());
			registerHandler(RecvPacketOpcode.USE_SUMMON_BAG, new UseSummonBag());
			registerHandler(RecvPacketOpcode.PET_FOOD, new PetFoodHandler());
			registerHandler(RecvPacketOpcode.USE_MOUNT_FOOD,
					new MountFoodHandler());
			registerHandler(RecvPacketOpcode.USE_CASH_ITEM,
					new UseCashItemHandler());
			registerHandler(RecvPacketOpcode.USE_CATCH_ITEM,
					new UseCatchItemHandler());
			registerHandler(RecvPacketOpcode.USE_SKILL_BOOK,
					new SkillBookHandler());
			registerHandler(RecvPacketOpcode.USE_RETURN_SCROLL,
					new UseReturnScrollHandler());
			registerHandler(RecvPacketOpcode.MAKER_SKILL,
					new MakerSkillHandler());
			registerHandler(RecvPacketOpcode.USE_UPGRADE_SCROLL,
					new ScrollHandler());
			registerHandler(RecvPacketOpcode.USE_POTENTIAL_SCROLL, new 潜能附加());
			registerHandler(RecvPacketOpcode.USE_MAGNIFLER_SCROLL, new 放大镜());
			registerHandler(RecvPacketOpcode.USE_STAR_SCROLL, new 装备强化());
			registerHandler(RecvPacketOpcode.DISTRIBUTE_AP,
					new DistributeAPHandler());
			registerHandler(RecvPacketOpcode.DISTRIBUTE_AUTO_AP,
					new DistributeAutoAPHandler());
			registerHandler(RecvPacketOpcode.HEAL_OVER_TIME,
					new HealOvertimeHandler());
			registerHandler(RecvPacketOpcode.DISTRIBUTE_SP,
					new DistributeSPHandler());
			registerHandler(RecvPacketOpcode.SPECIAL_MOVE,
					new SpecialMoveHandler());
			registerHandler(RecvPacketOpcode.CANCEL_BUFF,
					new CancelBuffHandler());
			registerHandler(RecvPacketOpcode.SKILL_EFFECT,
					new SkillEffectHandler());
			registerHandler(RecvPacketOpcode.MESO_DROP, new MesoDropHandler());
			registerHandler(RecvPacketOpcode.GIVE_FAME, new GiveFameHandler());
			registerHandler(RecvPacketOpcode.CHAR_INFO_REQUEST,
					new CharInfoRequestHandler());
			registerHandler(RecvPacketOpcode.SPAWN_PET, new SpawnPetHandler());
			registerHandler(RecvPacketOpcode.CANCEL_DEBUFF,
					new CancelDebuffHandler());
			registerHandler(RecvPacketOpcode.CHANGE_MAP_SPECIAL,
					new ChangeMapSpecialHandler());
			registerHandler(RecvPacketOpcode.USE_INNER_PORTAL,
					new InnerPortalHandler());
			registerHandler(RecvPacketOpcode.TROCK_ADD_MAP,
					new TrockAddMapHandler());// 缩地石
			registerHandler(RecvPacketOpcode.QUEST_ACTION,
					new QuestActionHandler());
			registerHandler(RecvPacketOpcode.SKILL_MACRO,
					new SkillMacroHandler());
			registerHandler(RecvPacketOpcode.MULTI_CHAT, new MultiChatHandler());
			registerHandler(RecvPacketOpcode.WHISPER, new WhisperHandler());
			registerHandler(RecvPacketOpcode.SPOUSE_CHAT,
					new SpouseChatHandler());
			registerHandler(RecvPacketOpcode.MESSENGER, new MessengerHandler());
			registerHandler(RecvPacketOpcode.PLAYER_INTERACTION,
					new PlayerInteractionHandler());
			registerHandler(RecvPacketOpcode.PARTY_OPERATION,
					new PartyOperationHandler());
			registerHandler(RecvPacketOpcode.DENY_PARTY_REQUEST,
					new DenyPartyRequestHandler());
			registerHandler(RecvPacketOpcode.GUILD_OPERATION,
					new GuildOperationHandler());
			registerHandler(RecvPacketOpcode.DENY_GUILD_REQUEST,
					new DenyGuildRequestHandler());
			registerHandler(RecvPacketOpcode.BUDDYLIST_MODIFY,
					new BuddylistModifyHandler());
			registerHandler(RecvPacketOpcode.NOTE_ACTION,
					new NoteActionHandler());
			registerHandler(RecvPacketOpcode.USE_DOOR, new DoorHandler());
			registerHandler(RecvPacketOpcode.CHANGE_KEYMAP,
					new KeymapChangeHandler());
			registerHandler(RecvPacketOpcode.RING_ACTION,
					new RingActionHandler());
			registerHandler(RecvPacketOpcode.ALLIANCE_OPERATION,
					new AllianceOperationHandler());
			registerHandler(RecvPacketOpcode.BBS_OPERATION,
					new BBSOperationHandler());
			registerHandler(RecvPacketOpcode.ENTER_MTS, new EnterMTSHandler());
			registerHandler(RecvPacketOpcode.SOLOMON, new SolomonHandler());
			registerHandler(RecvPacketOpcode.MOVE_PET, new MovePetHandler());
			registerHandler(RecvPacketOpcode.PET_CHAT, new PetChatHandler());
			registerHandler(RecvPacketOpcode.PET_COMMAND,
					new PetCommandHandler());
			registerHandler(RecvPacketOpcode.PET_LOOT, new PetLootHandler());
			registerHandler(RecvPacketOpcode.PET_AUTO_POT,
					new PetAutoPotHandler());
			registerHandler(RecvPacketOpcode.SUMMON_SKILL,
					new SummomSkillHandler());
			registerHandler(RecvPacketOpcode.MOVE_SUMMON,
					new MoveSummonHandler());
			registerHandler(RecvPacketOpcode.DRAGON_MOVE,
					new MoveDragonHandler());
			registerHandler(RecvPacketOpcode.SUMMON_ATTACK,
					new SummonDamageHandler());
			registerHandler(RecvPacketOpcode.DAMAGE_SUMMON,
					new DamageSummonHandler());
			registerHandler(RecvPacketOpcode.MOVE_LIFE, new MoveLifeHandler());
			registerHandler(RecvPacketOpcode.AUTO_AGGRO, new AutoAggroHandler());
			registerHandler(RecvPacketOpcode.MONSTER_BOMB,
					new MonsterBombHandler());
			registerHandler(RecvPacketOpcode.NPC_ACTION, new NPCAnimation());
			registerHandler(RecvPacketOpcode.ITEM_PICKUP,
					new ItemPickupHandler());
			registerHandler(RecvPacketOpcode.HYPNOTIZE, new HypnotizeHandler());
			registerHandler(RecvPacketOpcode.DAMAGE_REACTOR,
					new ReactorHitHandler());
			registerHandler(RecvPacketOpcode.TOUCH_REACTOR,
					new TouchReactorHandler());
			registerHandler(RecvPacketOpcode.MOB_DAMAGE_MOB,
					new FriendlyMobDamagedHandler());
			registerHandler(RecvPacketOpcode.PARTY_SEARCH_REGISTER,
					new PartySearchRegisterHandler());
			registerHandler(RecvPacketOpcode.PARTY_SEARCH_START,
					new PartySearchStartHandler());
			registerHandler(RecvPacketOpcode.PLAYER_UPDATE,
					new PlayerUpdateHandler());
			registerHandler(RecvPacketOpcode.TOUCHING_CS, new CashShopHandler());
			registerHandler(RecvPacketOpcode.TOUCHING_CSup,
					new TouchingCashShopupHandler());
			registerHandler(RecvPacketOpcode.CASHGIFT,
					new CashShopGiftHandler());
			registerHandler(RecvPacketOpcode.MAPLETV, new MapleTVHandler());
			// registerHandler(RecvPacketOpcode.MTS_OP, new MTSHandler());
			registerHandler(RecvPacketOpcode.SCRIPTED_ITEM,
					new ScriptedItemHandler());
			registerHandler(RecvPacketOpcode.REVIVE_ITEM,
					new ReviveItemHandler());
			registerHandler(RecvPacketOpcode.SUMMON_TALK,
					new SummonTalkHandler());
			registerHandler(RecvPacketOpcode.VICIOUS_HAMMER, new 金锤子());
			registerHandler(RecvPacketOpcode.CHATROOM_SYSTEM,
					new ChatRoomHandler());
			registerHandler(RecvPacketOpcode.DragonBall1,
					new DragonBall1Handler());
			registerHandler(RecvPacketOpcode.DragonBall2,
					new DragonBall2Handler());
			registerHandler(RecvPacketOpcode.ARAN_COMBO, new AranComboHandler());
			registerHandler(RecvPacketOpcode.MONSTER_BOMB_SKILL,
					new MonsterBombSkillHandler());
			registerHandler(RecvPacketOpcode.SPECIAL_SUMMON2, new 人造卫星());
			registerHandler(RecvPacketOpcode.SPECIAL_SKILL_EFFECT, new 机械技能特效());
			registerHandler(RecvPacketOpcode.SPECIAL_DOOR, new 机械传送门());
			registerHandler(RecvPacketOpcode.BEANS_GAME1,
					new BeansGame1Handler());
			registerHandler(RecvPacketOpcode.BEANS_GAME2,
					new BeansGame2Handler());
			registerHandler(RecvPacketOpcode.UNKNOW_SKILL,
					new UnknowSkillHandler());
			registerHandler(RecvPacketOpcode.CHECK_TIME, new CheckTimeHandler());
			registerHandler(RecvPacketOpcode.PLAYER_COMMAND,
					new PlayerCommandHandler());
			registerHandler(RecvPacketOpcode.CHRONOSPHERING,
					new ChronospheringHandler());
			registerHandler(RecvPacketOpcode.MOVE_ANDROID,
					new MoveAndroidHandler());
			registerHandler(RecvPacketOpcode.FACE_ANDROID,
					new AndroidEmotionHandler());
			registerHandler(RecvPacketOpcode.CHANGE_CMD,
					new MapleGameHandler.HandlerSetCmd());
			registerHandler(RecvPacketOpcode.ATTACKEFFECT,
					new MapleGameHandler.AttackEffectHandler());
			registerHandler(RecvPacketOpcode.OPEN_POWER,
					new MapleGameHandler.PowerSwitchHandler());
		} else {
			throw new RuntimeException("Unknown packet processor mode");
		}
	}
}
