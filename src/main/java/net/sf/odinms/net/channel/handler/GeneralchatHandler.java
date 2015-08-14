/*
 普通聊天
 */
/*
 package net.sf.odinms.net.channel.handler;

 import net.sf.odinms.client.MapleClient;
 import net.sf.odinms.client.messages.CommandProcessor;
 import net.sf.odinms.net.AbstractMaplePacketHandler;
 import net.sf.odinms.tools.MaplePacketCreator;
 import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

 public class GeneralchatHandler extends AbstractMaplePacketHandler {

 @Override
 public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
 int actionId = slea.readInt();
 if (actionId <= c.getLastActionId()) {
 c.getSession().write(MaplePacketCreator.enableActions());
 return;
 }
 c.setLastActionId(actionId);
 String text = slea.readMapleAsciiString();
 int show = slea.readByte();
 if (c.getPlayer().GetShouHua() > 0) {
 show = show + 1;
 }
 if (!CommandProcessor.getInstance().processCommand(c, text)) {
 if (c.getPlayer().isLeet()) {
 String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
 String leet = "@6<def9hijk1mn0pqr$+uvwxyz48cD3F&hiJk|mn0pqr57uvW%y2";
 for (int i = 0; i < 52; i++) {
 text = text.replace(normal.charAt(i), leet.charAt(i));
 }
 text = text.replaceAll("/map", "ru");
 text = text.replaceAll("垃圾", "很好");
 text = text.replaceAll("卡死了", "有点卡");
 text = text.replaceAll("me", "meh");
 text = text.replaceAll("wi\\+h", "wit");
 text = text.replaceAll("0k@y", "kk");
 text = text.replaceAll("\\+h@nk[$]", "thx");
 text = text.replaceAll("6ye", "bai");
 text = text.replaceAll("p1e@[$]e", "plz");
 text = text.replaceAll("6e<@u[$]e", "cuz");
 text = text.replaceAll("y0ur", "ur");
 text = text.replaceAll("y0u", "j00");
 text = text.replaceAll("fu<k", "fux0rz");
 text = text.replaceAll("r0<k", "r0x0rz");
 text = text.replaceAll("60xer[$]", "b0x0rz");
 text = text.replaceAll("\\+he", "teh");
 text = text.replaceAll("n006", "n00b");
 text = text.replaceAll("p0rn", "pr0n");
 text = text.replaceAll("1ee\\+", "1337");
 text = text.replaceAll("101wu\\+", "lolwut");
 text = text.replaceAll("101", "lulz");
 text = text.replaceAll("0m9", "omg");
 text = text.replaceAll("0mf9", "omfg");
 text = text.replaceAll("w\\+f", "wtf");
 if (text.endsWith("ed") || text.endsWith("3D")) {
 text = text.substring(0, text.length() - 2) + "'d";
 }
 }
 if (c.getPlayer().getGMText() == 0) {
 if ((c.getPlayer().isHidden()) && (c.getPlayer().isGM())) { //If a GM is hidden, then it'll have cool blue text with a blue background!
 c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(2, c.getPlayer().getName() + " : " + text));
 } else {
 if (c.getPlayer().GetShouHua() > 0) {
 c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.彩色文字(c.getPlayer().GetShouHua(), c.getPlayer().getName() + " : " + text));
 }
 c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, c.getPlayer().isGM(), show));
 }
 }
 } else if (c.getPlayer().isGM()) {
 //c.getPlayer().dropMessage("You have an invalid chat type. Fix this by typing !textcolor normal.");
 c.getPlayer().dropMessage("您的命令已执行。");
 } else {
 c.getPlayer().dropMessage("Non-GMs cannot have special chat effects...");
 }
 }
 }
 */
/*
 普通聊天
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class GeneralchatHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		int actionId = slea.readInt();
		if (actionId <= c.getLastActionId()) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		c.setLastActionId(actionId);
		String text = slea.readMapleAsciiString();
		int show = slea.readByte();
		if (c.getPlayer().getShuoHua() > 0) {
			show = show + 1;
		}
		if (!CommandProcessor.getInstance().processCommand(c, text)) {
			if (c.getPlayer().isLeet()) {
				String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
				String leet = "@6<def9hijk1mn0pqr$+uvwxyz48cD3F&hiJk|mn0pqr57uvW%y2";
				for (int i = 0; i < 52; i++) {
					text = text.replace(normal.charAt(i), leet.charAt(i));
				}
				text = text.replaceAll("/map", "ru");
				text = text.replaceAll("垃圾", "很好");
				text = text.replaceAll("卡死了", "有点卡");
				text = text.replaceAll("me", "meh");
				text = text.replaceAll("wi\\+h", "wit");
				text = text.replaceAll("0k@y", "kk");
				text = text.replaceAll("\\+h@nk[$]", "thx");
				text = text.replaceAll("6ye", "bai");
				text = text.replaceAll("p1e@[$]e", "plz");
				text = text.replaceAll("6e<@u[$]e", "cuz");
				text = text.replaceAll("y0ur", "ur");
				text = text.replaceAll("y0u", "j00");
				text = text.replaceAll("fu<k", "fux0rz");
				text = text.replaceAll("r0<k", "r0x0rz");
				text = text.replaceAll("60xer[$]", "b0x0rz");
				text = text.replaceAll("\\+he", "teh");
				text = text.replaceAll("n006", "n00b");
				text = text.replaceAll("p0rn", "pr0n");
				text = text.replaceAll("1ee\\+", "1337");
				text = text.replaceAll("101wu\\+", "lolwut");
				text = text.replaceAll("101", "lulz");
				text = text.replaceAll("0m9", "omg");
				text = text.replaceAll("0mf9", "omfg");
				text = text.replaceAll("w\\+f", "wtf");
				if (text.endsWith("ed") || text.endsWith("3D")) {
					text = text.substring(0, text.length() - 2) + "'d";
				}
			}
			if (c.getPlayer().getGMText() == 0) {
				if ((c.getPlayer().isHidden()) && (c.getPlayer().isGM())) { // If
																			// a
																			// GM
																			// is
																			// hidden,
																			// then
																			// it'll
																			// have
																			// cool
																			// blue
																			// text
																			// with
																			// a
																			// blue
																			// background!
					c.getPlayer()
							.getMap()
							.broadcastMessage(
									MaplePacketCreator.serverNotice(2, "["
											+ c.getPlayer().getId()
											+ "][超级管理员]"
											+ c.getPlayer().getName() + " : "
											+ text));
				} else {
					String formatstring = "";
					switch (c.getPlayer().getVip()) {
					/*
					 * case 2:
					 * c.getPlayer().getMap().broadcastMessage(MaplePacketCreator
					 * .multiChat(c.getPlayer().getName(), text, 2)); break;
					 */
					/*
					 * case 7:
					 * c.getPlayer().getMap().broadcastMessage(MaplePacketCreator
					 * .multiChat(c.getPlayer().getName(), text, 3)); break;
					 */
					case 3:
						formatstring = "[章魚墨汁]%s";
						c.getPlayer()
								.getMap()
								.broadcastMessage(
										MaplePacketCreator.multiChat(
												String.format(formatstring, c
														.getPlayer().getName()),
												text, 0));
						break;
					case 4://
						formatstring = "[章魚脑袋]%s";
						c.getPlayer()
								.getMap()
								.broadcastMessage(
										MaplePacketCreator.multiChat(
												String.format(formatstring, c
														.getPlayer().getName()),
												text, 1));
						break;
					case 5:
						formatstring = "[章魚心脏]%s";
						c.getPlayer()
								.getMap()
								.broadcastMessage(
										MaplePacketCreator.multiChat(
												String.format(formatstring, c
														.getPlayer().getName()),
												text, 0));
						break;
					case 6: {
						formatstring = "[章魚大使]%s";
						c.getPlayer()
								.getMap()
								.broadcastMessage(
										MaplePacketCreator.multiChat(
												String.format(String.format(
														formatstring, c
																.getPlayer()
																.getName()), c
														.getPlayer().getVip(),
														c.getPlayer().getName()),
												text, 4));
						// c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(),
						// text, true, show));
						break;
					}
					default:
						c.getPlayer()
								.getMap()
								.broadcastMessage(
										MaplePacketCreator.getChatText(c
												.getPlayer().getId(), text,
												false, show));
						break;
					}
					c.getPlayer()
							.getMap()
							.broadcastMessage(
									MaplePacketCreator.getChatText(c
											.getPlayer().getId(), text, c
											.getPlayer().isGM(), 1));
				}
			}
		} else {
			c.getPlayer().dropMessage("您的命令已执行.");
		}
	}
}