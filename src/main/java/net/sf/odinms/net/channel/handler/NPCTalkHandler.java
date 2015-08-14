package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class NPCTalkHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        long currenttime = System.currentTimeMillis();
        if ((currenttime - chr.getLasttime()) < 1000L) {
            chr.dropMessage("悠着点，点的太快会掉线的。");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.setLasttime(System.currentTimeMillis());
        int oid = slea.readInt();
        slea.readInt();
        if (c.getPlayer().getMap().getMapObject(oid) == null || !c.getPlayer().getMap().getMapObject(oid).getType().equals(MapleMapObjectType.NPC)) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleNPC npc = (MapleNPC) c.getPlayer().getMap().getMapObject(oid);
        
        if (npc.getId() == 9010009) {
            c.getSession().write(MaplePacketCreator.sendDuey((byte) 9, DueyActionHandler.loadItems(c.getPlayer())));
        } else if (npc.hasShop()) {
            if (c.getPlayer().getShop() != null) {
                c.getPlayer().setShop(null);
                c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 20));
            }
            npc.sendShop(c);
        } else {
            if (c.getCM() != null || c.getQM() != null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            NPCScriptManager.getInstance().start(c, npc.getId(), -1);
            // 0 = next button
            // 1 = yes no
            // 2 = accept decline
            // 5 = select a link
        }
    }
}