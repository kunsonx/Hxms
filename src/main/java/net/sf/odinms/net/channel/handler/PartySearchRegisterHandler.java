
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Quasar
 */
public class PartySearchRegisterHandler extends AbstractMaplePacketHandler {

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {

        MapleCharacter chr = c.getPlayer();
        int min = slea.readInt();
        int max = slea.readInt();
        if (chr.getLevel() < min || chr.getLevel() > max || (max - min) > 30 || min > max) { // Client editing
            //c.disconnect();
        }
        chr.setNeedsParty(true, min, max);

    }
}