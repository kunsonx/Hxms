/*
	召唤兽伤害
*/

package net.sf.odinms.net.channel.handler;

import java.util.List;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Jan
 */
public class DamageSummonHandler extends AbstractMaplePacketHandler implements MaplePacketHandler {

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        // D0 00 64 00 00 00 FF 00 00 00 00 13 7C 92 00 00
        // D0 00 CF 26 1B 00 FF 06 02 00 00 84 CD 6C 00 00
        // D0 00 CF 26 1B 00 FF 06 02 00 00 84 CD 6C 00 00
        // D0 00 DE 27 1B 00 FF E5 01 00 00 84 CD 6C 00 01
        int summonObjectid = slea.readInt();
        slea.readByte();
        int damage = slea.readInt();
        int monsterIdFrom = slea.readInt();
        int unkByte = slea.readByte();
        MapleCharacter player = c.getPlayer();
        MapleSummon summon = null;
        for (List<MapleSummon> sums : c.getPlayer().getSummons().values()) {
            for (MapleSummon sum : sums) {
                if (sum.getObjectId() == summonObjectid) {
                    summon = sum;
                }
            }
        }
        
        if (summon != null) {
            if (summon != null) {
                summon.addHP(-damage);
                if (summon.getHP() <= 0) {
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(summon, true), summon.getPosition());
                    c.getPlayer().getMap().removeMapObject(summon);
                    c.getPlayer().removeVisibleMapObject(summon);
                    //c.getPlayer().getSummons().remove(summon.getSkill());
                    c.getPlayer().removeSummon(summon.getSkill());
                }
            }
            player.getMap().broadcastMessage(player, MaplePacketCreator.damageSummon(player.getId(), summonObjectid, damage, monsterIdFrom, unkByte), summon.getPosition());
        }
    }
}