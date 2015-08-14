package net.sf.odinms.net.mina;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.MapleAESOFB;
import net.sf.odinms.tools.WriteToFile;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class MaplePacketDecoder extends CumulativeProtocolDecoder {

    private static final String DECODER_STATE_KEY = MaplePacketDecoder.class.getName() + ".STATE";
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MaplePacketDecoder.class);

    private static class DecoderState {

        public int packetlength = -1;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        DecoderState decoderState = (DecoderState) session.getAttribute(DECODER_STATE_KEY);
        if (decoderState == null) {
            decoderState = new DecoderState();
            session.setAttribute(DECODER_STATE_KEY, decoderState);

        }
        if (in.remaining() >= 4 && decoderState.packetlength == -1) {
            int packetHeader = in.getInt();
            if (!client.getReceiveCrypto().checkPacket(packetHeader)) {
                log.fatal("关闭客户端。");
                log.warn(MapleClient.getLogMessage(client, "确认返回封包失败，断开连接。"));
                session.close(false);
                return false;
            }
            decoderState.packetlength = MapleAESOFB.getPacketLength(packetHeader);
        } else if (in.remaining() < 4 && decoderState.packetlength == -1) {
            log.trace("解密... 没有足够的数据");
            return false;
        }

        if (in.remaining() >= decoderState.packetlength) {
            byte decryptedPacket[] = new byte[decoderState.packetlength];
            in.get(decryptedPacket, 0, decoderState.packetlength);
            decoderState.packetlength = -1;
            client.getReceiveCrypto().getRecvBuffer(decryptedPacket);
            out.write(decryptedPacket);

            if (log.isDebugEnabled()) {
                if (decryptedPacket.length <= 300) {
                    String decryptedPackethex = HexTool.toString(decryptedPacket);
                    WriteToFile re = new WriteToFile("客户端发送.txt");
                    re.WriteFile("客户端发送:\r\n" + decryptedPackethex + "\r\nASCII:" + HexTool.toStringFromAscii(decryptedPacket));
                    if (!MapleCodecFactory.isFilter(decryptedPackethex)) {
                        log.debug("客户端发送:\r\n" + decryptedPackethex + "\r\nASCII:" + HexTool.toStringFromAscii(decryptedPacket) + "\r\n");
                    }
                    re.CloseFile();
                }
            }
            return true;
        } else {
            log.trace("decode... not enough data to decode (need " + decoderState.packetlength + ")");
            return false;
        }
    }
}
