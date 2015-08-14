
package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class BeansGame2Handler extends AbstractMaplePacketHandler{
    
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //log.debug("豆豆更新");
        c.getSession().write(MaplePacketCreator.updateBeans(c.getPlayer().getId(), c.getPlayer().getBeans()));
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
