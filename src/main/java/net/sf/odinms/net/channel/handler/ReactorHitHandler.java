package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.MapleReactor;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Lerk
 */
public class ReactorHitHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ReactorHitHandler.class);

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //19 01 [66 00 00 00] [00 00 00 00] [02 00 00 00] [47 01] 00 00 00 00
        //19 01 [6F 00 00 00] [00 00 00 00] [00 00 00 00] [A5 01] 00 00 00 00
        int oid = slea.readInt();
        slea.readInt();
        int charPos = slea.readInt();
        short stance = slea.readShort();
        MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (reactor != null && reactor.isAlive()) {
            reactor.hitReactor(charPos, stance, c);
        }
    }
}