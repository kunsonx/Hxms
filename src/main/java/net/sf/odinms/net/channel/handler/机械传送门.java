//机械师传送门
package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author 岚殇
 */
public class 机械传送门 extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, final MapleClient c) {
        //System.out.println("机械师传送门：" + slea.toString());
        int cid = slea.readInt();//人物id
        //Point atPos = slea.readPos(); //当前的门所在坐标
        if ((c.getPlayer().getId() == cid
                || c.getPlayer().getPartyId() != -1 && c.getPlayer().getPartyId() == c.getPlayer().getClient().getChannelServer().getPlayerStorage().getCharacterById(cid).getPartyId()) //&& c.getPlayer().getPosition() == atPos
                ) {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }
}
