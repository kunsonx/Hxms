package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.scripting.quest.QuestScriptManager;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class NPCMoreTalkHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte lastMsg = slea.readByte(); // 00 (last msg type I think)
        byte action = slea.readByte(); // 00 = end chat, 01 == follow
        //if (lastMsg == 9) {
        if (lastMsg == 3 || lastMsg == 4) { //093更改
            if (action != 0) {
                String returnText = "";
                int returnNumber = 0;
                if (lastMsg == 4) {
                    returnNumber = slea.readInt();
                } else {
                    returnText = slea.readMapleAsciiString();
                }
                if (c.getQM() != null) {
                    c.getQM().setGetText(returnText);
                    if (c.getQM().isStart()) {
                        QuestScriptManager.getInstance().start(c, action, lastMsg, -1);
                    } else {
                        QuestScriptManager.getInstance().end(c, action, lastMsg, -1);
                    }
                } else {
                    c.getCM().setGetText(returnText);
                    if (returnNumber != 0) {
                        c.getCM().setNumber(returnNumber);
                    }
                    NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                }
            } else {
                if (c.getQM() != null) {
                    c.getQM().dispose();
                } else {
                    c.getCM().dispose();
                }
            }
        } else {
            int selection = -1;
            //093更改
            /*
             * if (slea.available() >= 4) { selection = slea.readInt(); if
             * (selection < 0) { if (c.getQM() != null) { c.getQM().dispose(); }
             * else { c.getCM().dispose(); } return; } } else if
             * (slea.available() > 0) { selection = slea.readByte(); if
             * (slea.available() == 1) { c.getCM().setCash(slea.readByte() ==
             * 1); } }
             */
            if (slea.available() > 0) {
                selection = slea.readByte();
            }
            if (c.getQM() != null) {
                if (c.getQM().isStart()) {
                    QuestScriptManager.getInstance().start(c, action, lastMsg, selection);
                } else {
                    QuestScriptManager.getInstance().end(c, action, lastMsg, selection);
                }
            } else if (c.getCM() != null) {
                NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
            }
        }
    }
}