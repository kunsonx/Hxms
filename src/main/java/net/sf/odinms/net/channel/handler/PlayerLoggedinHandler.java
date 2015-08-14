package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.CharacterIdChannelPair;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.PlayerBuffValueHolder;
import net.sf.odinms.net.world.guild.MapleAlliance;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.Guild.ALLIANCE_msg;
import net.sf.odinms.tools.Guild.MapleGuild_Msg;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.PhoneType;
import net.sf.odinms.tools.Randomizer;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class PlayerLoggedinHandler extends AbstractMaplePacketHandler {

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(PlayerLoggedinHandler.class);
	private final List<InventoryActionsInfo> infos = Collections.emptyList();

	@Override
	public boolean validateState(MapleClient c) {
		return !c.isLoggedIn();
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		int cid = slea.readInt();
		MapleCharacter player = null;
		try {
			player = MapleCharacter.loadCharFromDB(cid, c, true);
		} catch (Exception e) {
			log.error("连接角色没找到", e);
		}
		c.setAccID(player.getAccountid());
		int state = c.getLoginState();
		boolean allowLogin = true;
		ChannelServer channelServer = c.getChannelServer();
		synchronized (this) {
			try {
				WorldChannelInterface worldInterface = channelServer
						.getWorldInterface();
				if (state == MapleClient.LOGIN_SERVER_TRANSITION) {
					for (String charName : c.loadCharacterNames(c.getWorld())) {
						if (worldInterface.isConnected(charName)) {
							allowLogin = false;
							break;
						}
					}
				}
			} catch (RemoteException e) {
				ServerExceptionHandler.HandlerRemoteException(e);
				channelServer.reconnectWorld();
				allowLogin = false;
			}
			if (state != MapleClient.LOGIN_SERVER_TRANSITION || !allowLogin) {
				player.saveToDB();
				log.fatal("关闭客户端。");
				c.getSession().close(false);
				return;
			}
			c.updateLoginState(MapleClient.LOGIN_LOGGEDIN);
		}
		c.setPlayer(player);
		player.recalcLocalStats();
		ChannelServer cserv = c.getChannelServer();
		cserv.addPlayer(player);
		try {
			List<PlayerBuffValueHolder> buffs = c.getChannelServer()
					.getWorldInterface().getBuffsFromStorage(cid);
			if (buffs != null) {
				c.getPlayer().silentGiveBuffs(buffs);
			}
			c.getChannelServer().getWorldInterface()
					.deregisterOfflinePlayer(player.getAccountid());
			if (player.getGuildid() > 0) {
				MapleGuild g = player.getClient().getChannelServer()
						.getGuild(player.getMGC());
				if (g.getMGC(player.getId()) == null) {
					player.setGuildId(0);
				}
			}
		} catch (RemoteException e) {
			ServerExceptionHandler.HandlerRemoteException(e);
			c.getChannelServer().reconnectWorld();
		}
		c.getSession().write(MaplePacketCreator.getCharInfo(player));
		player.getMap().addPlayer(player);
		try {
			Collection<BuddylistEntry> buddies = player.getBuddylist()
					.getBuddies();
			int buddyIds[] = player.getBuddylist().getBuddyIds();
			cserv.getWorldInterface().loggedOn(player.getName(),
					player.getId(), c.getChannel(), buddyIds);
			if (player.getParty() != null) {
				channelServer.getWorldInterface().updateParty(
						player.getParty().getId(), PartyOperation.LOG_ONOFF,
						new MaplePartyCharacter(player));
			}
			CharacterIdChannelPair[] onlineBuddies = cserv.getWorldInterface()
					.multiBuddyFind(player.getId(), buddyIds);
			for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
				BuddylistEntry ble = player.getBuddylist().get(
						onlineBuddy.getCharacterId());
				ble.setChannel(onlineBuddy.getChannel());
				player.getBuddylist().put(ble);
			}
			c.getSession().write(MaplePacketCreator.updateBuddylist(buddies));
			c.getSession().write(MaplePacketCreator.loadFamily(player));
			if (player.getFamilyId() > 0) {
				c.getSession().write(MaplePacketCreator.getFamilyInfo(player));
			}

			if (player.getGuildid() > 0) {
				c.getChannelServer()
						.getWorldInterface()
						.setGuildMemberOnline(player.getMGC(), true,
								c.getChannel());
				c.getSession().write(MapleGuild_Msg.showGuildInfo(player));
				int allianceId = player.getGuild().getAllianceId();
				if (allianceId > 0) {
					MapleAlliance newAlliance = channelServer
							.getWorldInterface().getAlliance(allianceId);
					if (newAlliance == null) {
						newAlliance = MapleAlliance.loadAlliance(allianceId);
						channelServer.getWorldInterface().addAlliance(
								allianceId, newAlliance);
					}
					c.getSession().write(
							ALLIANCE_msg.getAllianceInfo(newAlliance));
					c.getSession().write(
							ALLIANCE_msg.getGuildAlliances(newAlliance, c));
				}
			}
		} catch (RemoteException e) {
			ServerExceptionHandler.HandlerRemoteException(e);
			log.info("REMOTE THROW", e);
			// channelServer.reconnectWorld();
		}
		player.updatePartyMemberHP();
		for (MapleQuestStatus status : player.getStartedQuests()) {
			if (status.hasMobKills()) {
				c.getSession().write(
						MaplePacketCreator.updateQuestMobKills(status));
			}
		}
		CharacterNameAndId pendingBuddyRequest = player.getBuddylist()
				.pollPendingRequest();
		if (pendingBuddyRequest != null) {
			player.getBuddylist().put(
					new BuddylistEntry(pendingBuddyRequest.getName(),
							pendingBuddyRequest.getId(), -1, false));
			c.getSession().write(
					MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest
							.getId(), pendingBuddyRequest.getName(),
							pendingBuddyRequest.getLevel(), c.getChannel(), c
									.getPlayer().getJobid()));
		}

		if (!c.getPlayer().hasMerchant() && c.getPlayer().tempHasItems()) {
			c.getPlayer().dropMessage(1, "请通过弗兰德里取回保管的物品");
		}
		if (player.getMapId() == 555000000) {
			// c.getSession().write(MaplePacketCreator.sendHint("欢迎光临我的朋友,请点击NPC：#r[冒险岛管理员]#k 与它谈话.",
			// 450, 5));
			player.startMapEffect("欢迎.请点击NPC：[黑鼻白毛] 与它谈话！", 5121030);
		}
		int vip = c.getPlayer().getVip();
		try {
			if (c.getPlayer().isGM()) {
				c.getPlayer().dropMessage(
						"伟大的管理员大神上线巡逻了！    所有在线人数为:" + c.getPlayer().Lianjie()
								+ "人");
				player.getClient()
						.getSession()
						.write(MaplePacketCreator.sendHint(
								String.format(
										"欢迎来到%s\r\n#b@帮助 #k可查看游戏命令\r\n#b@npc#k 打开万能NPC\r\n#r祝大家游戏愉快天天开心...",
										channelServer.getServerName()), 300, 5));

			} else {
				String name = "[Game Master]";
				if (vip > 1) {
					int value = GameConstants.getLoginTipType(vip);
					c.getChannelServer()
							.getWorldInterface()
							.broadcastMessage(
									null,
									MaplePacketCreator
											.serverMessage(
													value,
													c.getChannel(),
													name
															+ (value == PhoneType.绿色抽奖公告
																	.getValue() ? " :     "
																	: " : ")
															+ GameConstants
																	.getLoginTip(
																			vip,
																			player.getName()),
													false,
													false,
													GameConstants
															.getDisplay_item())
											.getBytes());
				}
				player.getClient()
						.getSession()
						.write(MaplePacketCreator.sendHint(
								String.format(
										"欢迎来到%s\r\n#b@帮助 #k可查看游戏命令\r\n#b@npc#k 打开万能NPC\r\n#r祝大家游戏愉快天天开心...",
										channelServer.getServerName()), 300, 5));
			}
		} catch (RemoteException e) {
			ServerExceptionHandler.HandlerRemoteException(e);
			c.getChannelServer().reconnectWorld();
		}
		c.getPlayer()
				.getAttribute()
				.setDataValue(ConstantTable._PLAYER_DATA_LOGINTIME,
						System.currentTimeMillis());
		c.getPlayer()
				.getAttribute()
				.getAttribute()
				.put("p_lastLoginTime",
						GameConstants.getFormatter().format(new Date()));
		c.getSession().write(
				MaplePacketCreator.sendNecklace_Expansion(player
						.getNecklace_Expansion() != null
						|| player.getInventory(MapleInventoryType.EQUIPPED)
								.getItem((short) -37) != null));// 项链扩充
		player.showMapleTips();// 服务器公告提示
		player.sendKeymap();// 键盘表。
		player.showNote();// 小纸条
		c.getSession().write(
				MaplePacketCreator.sendAutoHpPot(c.getPlayer().getAutoHpPot()));// 自动吃药？
		c.getSession().write(
				MaplePacketCreator.sendAutoMpPot(c.getPlayer().getAutoMpPot()));
		// player.checkDuey();//送货员 duey
		player.sendMacros();// 发送技能宏
		player.checkMessenger();
		// player.expirationTask(); //检查物品是否过期\
		player.sendAddAttackLimit();
		player.sendDemonAvengerPacket();
		player.getAttribute().clearAttributes();
		c.getSession().write(MaplePacketCreator.showCharCash(c.getPlayer())); // 更新角色点卷/抵用券数量
		if (player.getAndroid() != null) {
			c.getSession()
					.write(MaplePacketCreator.spawnAndroid(player,
							player.getAndroid()));
			c.getSession().write(
					MaplePacketCreator.showAndroidEmotion(player.getId(),
							Randomizer.getInstance().nextInt(17) + 1));
		}
		if (c.getPlayer().getJob().IsTrailblazer()) {// 换线就没能量了
			c.getPlayer().initPower();
		}
		c.getSession().write(MaplePacketCreator.InventoryActions(false, infos));
		// 盛大103属性混乱BUG。
		c.getPlayer().saveToDB();// 保存修改到数据库。
		log.info("玩家[" + c.getPlayer().getName() + "]进入游戏了..");
	}
}
