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

public class MoveDragonHandler extends MovementParse {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //System.out.println("龙移动："+slea.toString());
        Point startPos = slea.readPos();
        //StreamUtil.readShortPoint(slea);
        slea.skip(4); //-1 093新增
        List<LifeMovementFragment> res = parseMovement(slea);
        MapleCharacter player = c.getPlayer();
        player.getMap().broadcastMessage(player, MaplePacketCreator.moveDragon(player.getId(),startPos, res), player.getPosition());

    }
}
