package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.SendPacketOpcode;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;
import org.apache.log4j.Logger;

public class NPCAnimation extends AbstractMaplePacketHandler {

    private static Logger log = Logger.getLogger(NPCAnimation.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        int length = (int) slea.available();

        if (length == 10) { // NPC Talk
            mplew.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
            int id = slea.readInt();
            byte b1, b2;
            b1 = slea.readByte();
            b2 = slea.readByte();
            int id3 = slea.readInt();


            if (b2 != -1) {
                mplew.writeInt(id);
                mplew.write(b1);
                mplew.write(b2);
                mplew.writeInt(id3);//97
                c.getSession().write(mplew.getPacket());
            }
        /*    if (b2 == -1) {
                log.debug("NPC动画数据： ID1 : " + id + "; ID2：" + b1 + "|" + b2 + " ;ID3: " + id3 + "\r\n数据：" + slea.toString());
            }    */
        } else if (length > 10) { // NPC Move
            byte[] bytes = slea.read(length - 9);
            mplew.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
            mplew.write(bytes);
            c.getSession().write(mplew.getPacket());
        } else {
            log.info("未知NPC谈话长度：" + length);
        }
    }
}
