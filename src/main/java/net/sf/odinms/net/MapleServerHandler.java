/*

 */
package net.sf.odinms.net;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.tools.MapleAESOFB;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.ByteArrayByteStream;
import net.sf.odinms.tools.data.input.GenericSeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

public class MapleServerHandler extends IoHandlerAdapter {

	private final static Logger log = Logger
			.getLogger(MapleServerHandler.class);
	public final static short MAPLE_VERSION = 110; // 主版本号
	public final static String MAPLE_SECONDARY_VERSION = "1"; // 次版本号
	private PacketProcessor processor;
	private ChannelDescriptor channel;

	public MapleServerHandler(PacketProcessor processor) {
		this(processor, null);
	}

	public MapleServerHandler(PacketProcessor processor,
			ChannelDescriptor channel) {
		this.processor = processor;
		this.channel = channel;
		LoginServer.getInstance().ipCanConnect("221.231.130.80");
		SendPacketOpcode.values();
		RecvPacketOpcode.values();
	}

	public PacketProcessor getProcessor() {
		return processor;
	}

	public ChannelDescriptor getChannel() {
		return channel;
	}

	public void Reset() {
		processor.reset(this.channel == null ? PacketProcessor.Mode.LOGINSERVER
				: PacketProcessor.Mode.CHANNELSERVER);
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		Runnable r = ((MaplePacket) message).getOnSend();
		if (r != null) {
			r.run();
		}
		super.messageSent(session, message);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		MapleClient client = (MapleClient) session
				.getAttribute(MapleClient.CLIENT_KEY);
		// log.error(MapleClient.getLogMessage(client, cause.getMessage()),
		// cause);
		if (client != null) {
			// log.error(String.format("角色名：%s", client.getPlayer().getName()),
			// cause);
		}
	}

	@Override
	// 会话打开
	public void sessionOpened(IoSession session) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug(session.getRemoteAddress() + " 连接到服务器");
		}
		/*
		 * if
		 * (!LoginServer.getInstance().ipCanConnect(session.getRemoteAddress()
		 * .toString())) { log.fatal("关闭客户端。【原因：IP已不允许连接】");
		 * session.close(false); return; }
		 * LoginServer.getInstance().addConnectedIP
		 * (session.getRemoteAddress().toString());
		 */
		if (channel != null) {
			if (ChannelServer.getInstance(channel).isShutdown()) {
				log.fatal("关闭客户端。【原因：频道已关闭】");
				session.close(false);
				return;
			}
		}
		byte ivRecv[] = { (byte) (Math.random() * 255),
				(byte) (Math.random() * 255), (byte) (Math.random() * 255),
				(byte) (Math.random() * 255) };
		byte ivSend[] = { (byte) (Math.random() * 255),
				(byte) (Math.random() * 255), (byte) (Math.random() * 255),
				(byte) (Math.random() * 255) };
		// ivRecv[3] = (byte) (Math.random() * 255);
		// ivSend[3] = (byte) (Math.random() * 255);
		MapleAESOFB sendCypher = new MapleAESOFB(ivSend,
				(short) (0xFFFF - MAPLE_VERSION));
		MapleAESOFB recvCypher = new MapleAESOFB(ivRecv, MAPLE_VERSION);
		final MapleClient client = new MapleClient(sendCypher, recvCypher,
				session);
		client.setChannel(channel);
		if (channel != null && ChannelServer.getInstance(channel).isGateway()) {
			byte[] ic = new byte[8];
			System.arraycopy(ivRecv, 0, ic, 0, 4);
			System.arraycopy(ivSend, 0, ic, 4, 4);
			client.setIvcheck(ic);
		}
		session.write(MaplePacketCreator.getHello(MAPLE_VERSION,
				MAPLE_SECONDARY_VERSION, ivSend, ivRecv, false));
		session.setAttribute(MapleClient.CLIENT_KEY, client);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		synchronized (session) {
			MapleClient client = (MapleClient) session
					.getAttribute(MapleClient.CLIENT_KEY);
			if (client != null) {
				MapleCharacter chr = client.getPlayer();
				if (chr != null) {
					// log.info("{} 退出游戏", chr);
					chr.getAttribute().saveToDb();
				}
				client.disconnect();
				session.removeAttribute(MapleClient.CLIENT_KEY);
				// log.info("{} 断开连接", session.getRemoteAddress());
			}
		}
		super.sessionClosed(session);
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		byte[] content = (byte[]) message;
		SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(
				new ByteArrayByteStream(content));
		short packetId = slea.readShort();
		MapleClient client = (MapleClient) session
				.getAttribute(MapleClient.CLIENT_KEY);
		if (channel == null && client.Islogincheck() && slea.available() == 0) {
			client.RecvLoginCheckPong("");
			return;
		}
		MaplePacketHandler packetHandler = processor.getHandler(packetId);
		if (log.isTraceEnabled() || log.isInfoEnabled()) {
			String from = "";
			if (client.getPlayer() != null) {
				from = "来自 [" + client.getPlayer().getName() + "]";
			}
			if (packetHandler == null) {
				// log.info("未解密的封包1{} 长度：({}) \n包：{}", new Object[]{from,
				// content.length, HexTool.toString(content)});
				// log.info("需解密的封包{} \n长度：({}) \n包：{}\nAscii码：{}", new
				// Object[]{from, content.length, HexTool.toString(content),
				// HexTool.toStringFromAscii(content)});
			}
		}

		if (client.getPlayer() != null) {
			client.getPlayer().checkPower();
		}

		if (packetHandler != null && packetHandler.validateState(client)) {
			try {
				packetHandler.handlePacket(slea, client);
				/*
				 * if (trace) log.debug("["+
				 * packetHandler.getClass().getSimpleName() + "]");
				 */
			} catch (Throwable t) {
				t.printStackTrace();
				// log.debug(MapleClient.getLogMessage(client, "发生错误\n处理类: " +
				// packetHandler.getClass().getName() + "\n原因: " +
				// t.getMessage()));
				// StringWriter sw = new StringWriter();
				// t.printStackTrace(new PrintWriter(sw, true));
				// WriteToFile re = new
				// WriteToFile("D:\\ErrorPacket\\处理错误.txt");
				// re.WriteFile("发生错误\r\n处理类: " +
				// packetHandler.getClass().getName()
				// +" \r\n玩家名称:"+client.getPlayer().getName()+"\r\n原因: "+t.getMessage()+"\r\n 位置:\r\n "+sw.toString());
				client.getPlayer().message("系统处理错误。");
				client.getSession().write(MaplePacketCreator.enableActions());
			}
		}
	}

	@Override
	public void sessionIdle(final IoSession session, final IdleStatus status)
			throws Exception {
		MapleClient client = (MapleClient) session
				.getAttribute(MapleClient.CLIENT_KEY);

		// if (client != null && client.getPlayer() != null &&
		// log.isTraceEnabled()) {
		// log.trace("玩家空闲着", client.getPlayer().getName());
		// }

		if (client != null) {
			client.sendPing();
		}
		super.sessionIdle(session, status);
	}
}
