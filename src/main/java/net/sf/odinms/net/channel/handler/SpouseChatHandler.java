package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class SpouseChatHandler extends AbstractMaplePacketHandler {

	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		String recipient = slea.readMapleAsciiString();
		String text = slea.readMapleAsciiString();
		if (!CommandProcessor.getInstance().processCommand(c, text)) {
			MapleCharacter player = c.getChannelServer().getPlayerStorage()
					.getCharacterByName(recipient);
			if (player != null) {
				player.getClient()
						.getSession()
						.write(MaplePacketCreator.spouseChat(c.getPlayer()
								.getName(), text, 5));
				c.getSession().write(
						MaplePacketCreator.spouseChat(c.getPlayer().getName(),
								text, 4));
			} else {
				try {
					if (c.getChannelServer().getWorldInterface()
							.isConnected(recipient)) {
						c.getChannelServer()
								.getWorldInterface()
								.spouseChat(c.getPlayer().getName(), recipient,
										text);
						c.getSession().write(
								MaplePacketCreator.getWhisperReply(recipient,
										(byte) 1));
					} else {
						c.getSession().write(
								MaplePacketCreator.getWhisperReply(recipient,
										(byte) 0));
					}
				} catch (RemoteException e) {
					ServerExceptionHandler.HandlerRemoteException(e);
					c.getSession().write(
							MaplePacketCreator.getWhisperReply(recipient,
									(byte) 0));
					c.getChannelServer().reconnectWorld();
				}
			}
		}
	}
}