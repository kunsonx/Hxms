/*
 应该是悄悄话、轻声说话
 */
package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;
import java.util.Collection;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class WhisperHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		byte mode = slea.readByte();
		if (mode == 6) { // Whisper
			slea.readInt();
			String recipient = slea.readMapleAsciiString();
			String text = slea.readMapleAsciiString();

			if (text.length() > 70 && !c.getPlayer().isGM()) {
				return;
			}
			if (!CommandProcessor.getInstance().processCommand(c, text)) {
				if (c.getPlayer().isMuted()) {
					c.getPlayer()
							.dropMessage(
									5,
									c.getPlayer().isMuted() ? "You are "
											: "The map is "
													+ "muted, therefore you are unable to talk.");
					return;
				}
				MapleCharacter player = c.getChannelServer().getPlayerStorage()
						.getCharacterByName(recipient);
				if (player != null) {
					if (player.isGM() && !c.getPlayer().isGM()) {
						c.getSession().write(
								MaplePacketCreator.getWhisperReply(recipient,
										(byte) 0));
					} else {
						player.getClient()
								.getSession()
								.write(MaplePacketCreator.getWhisper(c
										.getPlayer().getName(), c.getChannel(),
										text));
						c.getSession().write(
								MaplePacketCreator.getWhisperReply(recipient,
										(byte) 1));
					}
				} else {
					int result = 0;
					try {
						result = c
								.getChannelServer()
								.getWorldInterface()
								.whisper(c.getPlayer().getName(), recipient,
										c.getChannel(), text,
										c.getPlayer().isGM());
						c.getSession().write(
								MaplePacketCreator.getWhisperReply(recipient,
										(byte) result));
					} catch (RemoteException re) {
						ServerExceptionHandler.HandlerRemoteException(re);
						c.getSession().write(
								MaplePacketCreator.getWhisperReply(recipient,
										(byte) result));
					}
				}
			}
		} else if (mode == 5) { // Find
			slea.readInt();
			String recipient = slea.readMapleAsciiString();
			MapleCharacter player = c.getChannelServer().getPlayerStorage()
					.getCharacterByName(recipient);
			if (player != null) {
				if (player.inCS()) {
					c.getSession().write(
							MaplePacketCreator.getFindReplyWithCS(
									player.getName(), mode));
				} else {
					c.getSession().write(
							MaplePacketCreator.getFindReplyWithMap(
									player.getName(), player.getMap().getId(),
									mode));
				}
			} else { // Not found
				Collection<ChannelServer> cservs = c.getChannelServers();
				for (ChannelServer cserv : cservs) {
					player = cserv.getPlayerStorage().getCharacterByName(
							recipient);
					if (player != null) {
						break;
					}
				}
				if (player != null
						&& (player.isGM() ? c.getPlayer().isGM() : true)) {
					c.getSession().write(
							MaplePacketCreator.getFindReply(player.getName(),
									(byte) player.getClient().getChannel(),
									mode));
				} else {
					c.getSession().write(
							MaplePacketCreator.getWhisperReply(recipient,
									(byte) 0));
				}
			}
		} else if (mode == 0x44) {// 寻找好友
			slea.readInt();
			String recipient = slea.readMapleAsciiString();
			MapleCharacter player = c.getChannelServer().getPlayerStorage()
					.getCharacterByName(recipient);
			if (player != null) {
				if (player.inCS()) {
					c.getSession().write(
							MaplePacketCreator.getFindReplyWithCS(
									player.getName(), mode));
				} else {
					c.getSession().write(
							MaplePacketCreator.getFindReplyWithMap(
									player.getName(), player.getMap().getId(),
									mode));
				}
			} else { // Not found
				Collection<ChannelServer> cservs = c.getChannelServers();
				for (ChannelServer cserv : cservs) {
					player = cserv.getPlayerStorage().getCharacterByName(
							recipient);
					if (player != null) {
						break;
					}
				}
				if (player != null
						&& (player.isGM() ? c.getPlayer().isGM() : true)) {
					c.getSession().write(
							MaplePacketCreator.getFindReply(player.getName(),
									(byte) player.getClient().getChannel(),
									mode));
				} else {
					c.getSession().write(
							MaplePacketCreator.getWhisperReply(recipient,
									(byte) 0));
				}
			}
		}
	}
}
