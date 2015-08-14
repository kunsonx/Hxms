/*
更换特殊地图程序,自由市场 渔场等
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public final class ChangeMapSpecialHandler extends AbstractMaplePacketHandler {

    private org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChangeMapSpecialHandler.class);

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //6E 00 [01] [09 00 69 6E 46 69 73 68 69 6E 67] [11 FF 22 00]    //渔场
        slea.readByte();
        String startwp = slea.readMapleAsciiString();
        slea.readInt();
        MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);
        if (portal != null) {
            portal.enterPortal(c);
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }
}
