package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import java.util.Collection;
import java.util.List;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.input.StreamUtil;

public class MoveSummonHandler extends MovementParse {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        Point startPos = slea.readPos();
        slea.skip(4);
        List<LifeMovementFragment> res = parseMovement(slea);
        MapleCharacter player = c.getPlayer();
        MapleSummon summon = null;
        /*for (List<MapleSummon> sums : c.getPlayer().getSummons().values()) {
         for (MapleSummon sum : sums) {
         if (sum.getObjectId() == oid) {
         summon = sum;
         }
         }
         }*/
        Object obj = c.getPlayer().getMap().getMapObject(oid);
        if (obj instanceof MapleSummon) {
            summon = (MapleSummon) obj;
        }
        if (summon != null) {
            updatePosition(res, summon, 0);
            player.getMap().broadcastMessage(player, MaplePacketCreator.moveSummon(player.getId(), oid, startPos, res), summon.getPosition());
        }
    }
}
