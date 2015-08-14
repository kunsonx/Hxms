package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.scripting.quest.QuestScriptManager;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class QuestActionHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(QuestActionHandler.class);

    public QuestActionHandler() {
    }

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {

        //[68 00] [02] [69 08] 8D 71 0F 00 F3 0A 4E 01 FF FF FF FF
        //[6D 00] [01] [2E 28] [6C 0F 8E 00]
        //[6D 00] [01] [54 08] [41 BF 0F 00]
        byte action = slea.readByte();
        short quest = slea.readShort();
        MapleCharacter player = c.getPlayer();
        if (action == 1) { // 接受任务
            int npc = slea.readInt();
            if (quest == 10286) {
                MapleQuest.getInstance(quest).complete(player, npc, true);
            } else {
                try {
                    MapleQuest.getInstance(quest).start(player, npc);
                } catch (Exception e) {
                    log.error("开始任务出错. 任务ID: " + quest, e);
                }
            }
        } else if (action == 2) { // 完成任务
            int npc = slea.readInt();
            slea.readInt(); // dont know *o*
            try {
                if (slea.available() >= 4L) {
                    int selection = slea.readInt();
                    MapleQuest.getInstance(quest).complete(player, npc, selection, false);
                } else {
                    MapleQuest.getInstance(quest).complete(player, npc);
                    c.getPlayer().getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(0, 10));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showBuffeffect(c.getPlayer().getId(), 0, 10, (byte) 3, 1), false);
                }
            } catch (Exception e) {
                log.error("完成任务出错. 任务ID: " + quest, e);
            }
        } else if (action == 3) { // 放弃任务
            MapleQuest.getInstance(quest).forfeit(player);
        } else if (action == 4) { //脚本开始任务
            int npc = slea.readInt();
            slea.readInt(); // dont know *o*
            QuestScriptManager.getInstance().start(c, npc, quest);
        } else if (action == 5) { //脚本结束任务
            int npc = slea.readInt();
            slea.readInt(); // dont know *o*
            QuestScriptManager.getInstance().end(c, npc, quest);
            c.getPlayer().getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(0, 10));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showBuffeffect(c.getPlayer().getId(), 0, 10, (byte) 3, 1), false);
        }
    }
}