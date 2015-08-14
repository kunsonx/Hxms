package net.sf.odinms.net.channel.handler;

import java.util.List;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author 岚殇
 */

/*
 * 人造卫星 泰坦 处理类
 */
public final class 人造卫星 extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //System.out.println("人造卫星/泰坦处理类" + slea.toString());
        //Collections<List<MapleSummon> summons = c.getPlayer().getSummons().values();
        int oid = slea.readInt();
        MapleSummon summon = null;
        for (List<MapleSummon> summons : c.getPlayer().getSummons().values()) {
            for (MapleSummon sum : summons) {
                if (sum.getObjectId() == oid) {
                    summon = sum;
                    break;
                }
            }
        }
        if (summon != null) {
            c.getPlayer().cancelEffect(summon.getEffect(), false, -1);
            //System.out.println("存在人造卫星/泰坦");
            /*c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(summon, true), summon.getPosition());
            c.getPlayer().getMap().removeMapObject(summon);
            c.getPlayer().removeVisibleMapObject(summon);
            //c.getPlayer().getSummons().remove(summon.getSkill());
            c.getPlayer().removeSummon(summon.getSkill());*/
        } else {
            //c.getPlayer().getSummons().clear();
            //System.out.println("不存在人造卫星/泰坦");
        }
    }
}
