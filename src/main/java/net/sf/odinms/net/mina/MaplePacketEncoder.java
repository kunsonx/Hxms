/*

 */
package net.sf.odinms.net.mina;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.MapleAESOFB;
import net.sf.odinms.tools.WriteToFile;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class MaplePacketEncoder implements ProtocolEncoder {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MaplePacketEncoder.class);

    public static void getCaller() {

// 堆栈跟踪中的元素，它由 Throwable.getStackTrace()

// 返回。每个元素表示单独的一个堆栈帧。所有的堆栈帧（堆栈顶部的那个堆栈帧除外）都表示一个方法调用。堆栈顶部的帧表示生成堆栈跟踪的执行点。通常，这是创建对应于堆栈跟踪的

// throwable 的点。



        StackTraceElement stack[] = (new Throwable()).getStackTrace();

        for (int i = 0; i < stack.length; i++) {

            log.info("堆栈数组的大小是：" + stack.length);

            /*
             * for (StackTraceElement ste : stack) {
             *
             * if (ste.getFileName().equals("MaplePacketCreator.java")) {
             * log.debug("无效包头调用地址：" + ste.getMethodName() + "\t"); WriteToFile
             * wt = new WriteToFile("ErrorPacketHandle.txt");
             * wt.WriteFile("无效包头调用地址：" + ste.getMethodName() + "\t");
             * wt.CloseFile(); } //log.debug(ste.getFileName()); }
             */

            log.info("*******************");

            StackTraceElement ste = stack[i];

            log.info(ste.getClassName() + "." + ste.getMethodName()
                    + "(...)");

            log.info(i + "--" + ste.getMethodName());

            log.info(i + "--" + ste.getFileName());

            log.info(i + "--" + ste.getLineNumber());

        }

    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            byte[] input = ((MaplePacket) message).getBytes();

            if (log.isDebugEnabled()) {
                if (input.length <= 140000) {
                    // 服务端反馈
                    String inputhex = HexTool.toString(input);
                    WriteToFile re = new WriteToFile("服务端反馈.txt");
                    re.WriteFile("服务端反馈:\r\n" + inputhex + "\r\nASCII:" + HexTool.toStringFromAscii(input));
                    if (!MapleCodecFactory.isFilter(inputhex)) {
                        log.debug("服务端反馈:\r\n" + inputhex + "\r\nASCII:" + HexTool.toStringFromAscii(input));
                    }
                    re.CloseFile();
                    /*
                     * if (inputhex.startsWith("FE FF") || inputhex.equals("26
                     * 00")) { getCaller(); }
                     */
                }
            }

            final MapleAESOFB send_crypto = client.getSendCrypto();
            send_crypto.getSendBuffer(input, out, client);
        } else {
            out.write(IoBuffer.wrap(((MaplePacket) message).getBytes()));
        }
    }

    @Override
    public void dispose(IoSession session) throws Exception {
    }
}
