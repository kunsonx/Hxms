//宠物移动
package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MovePetHandler extends MovementParse {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //log.debug("宠物移动包："+slea.toString());
        //int petId = slea.readInt();
        int slot = slea.readInt();
        Point startPos = slea.readPos();
        slea.readInt();
        slea.skip(1);
        //Point startPos = slea.readPos();
        List<LifeMovementFragment> res = parseMovement(slea);
        if (res.isEmpty()) {
            return;
        }
        slea.skip(1);
        int xb = slea.readShort();
        slea.readInt();
        int yb = slea.readShort();
        MapleCharacter player = c.getPlayer();
        //int slot = player.getPetByUniqueId(petId);
        if (player.inCS() || slot == -1) {
            return;
        }
        if (player.getPet(slot) != null) {
            player.getPet(slot).updatePosition(res);
            player.getMap().broadcastMessage(player, MaplePacketCreator.movePet(player.getId(), startPos, slot, res, xb, yb), false);
        }
    }
}