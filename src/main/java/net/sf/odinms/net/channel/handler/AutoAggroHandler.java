/*
	处理程序自动仇恨
*/

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class AutoAggroHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        // A0 00 7C 00 00 00 0C 00 00 00
        int oid = slea.readInt();
        MapleMap map = c.getPlayer().getMap();
        MapleMonster monster = map.getMonsterByOid(oid);
        if (monster != null && monster.getController() != null) {
            if (!monster.isControllerHasAggro()) {
                if (map.getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(c.getPlayer(), true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else {
                if (map.getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(c.getPlayer(), true);
                }
            }
        } else if (monster != null && monster.getController() == null) {
            monster.switchController(c.getPlayer(), true);
        }
    }
}