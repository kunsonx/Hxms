/*
   怪物炸弹 闹钟召唤的小怪 黑水雷
 * 双刀的怪物炸弹技能可以使所有怪都变成炸弹类小怪
*/

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MonsterBombHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(oid);
        if (!c.getPlayer().isAlive() || monster == null) {
            return;
        }
        monster.getMap().broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), 1));
        if(monster.getId() != 8500003 && monster.getId() != 8500004) //大小黑水雷
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.giveMonsterbBomb(monster));
        c.getPlayer().getMap().removeMapObject(monster);
    }
}