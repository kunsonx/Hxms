/*
 * 双击人物出现的
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CharInfoRequestHandler extends AbstractMaplePacketHandler {
//6A 00 [8B 41 94 03] [82 A1 42 00] FF
    //99 00 7B 21 AC 00 E5 00 00 00 FF 00

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int actionId = slea.readInt();
        if (actionId <= c.getLastActionId()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        c.setLastActionId(actionId);
        int cid = slea.readInt();
        MapleCharacter player = (MapleCharacter) c.getPlayer().getMap().getMapObject(cid);
        if (player != null && (!player.isGM() || (c.getPlayer().isGM() && player.isGM()))) {
            c.getSession().write(MaplePacketCreator.charInfo(player));
        } else {
            MaplePacket packet = c.getPlayer().getMap().getOfflinePlayer().getPlayerInfo(cid);
            if (packet != null) {
                c.getSession().write(packet);
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }

    }
}