/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.net.channel.handler;

import javax.script.ScriptEngine;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Jay Estrella
 */
public class ScriptedItemHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		MapleCharacter player = c.getPlayer();
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		slea.readInt(); // trash stamp (thx rmzero)
		byte itemSlot = (byte) slea.readShort(); // item sl0t (thx rmzero)
		int itemId = slea.readInt(); // itemId
		int npcId = ii.getScriptedItemNpc(itemId);
		IItem item = c.getPlayer().getInventory(ii.getInventoryType(itemId))
				.getItem(itemSlot);

		if (item == null || item.getItemId() != itemId
				|| item.getQuantity() <= 0 || npcId == 0) {
			if (player.isGM()) {
				player.弹窗(String.format("无法执行该脚本物品,物品ID：%d,NPCID：%d", itemId,
						npcId));
			}
			return;
		}
		if (player.isGM()) {
			player.dropMessage(String
					.format("正在为脚本物品:%d 执行NPC：%d ,如果需要在脚本内获得物品ID, 请在脚本内使用变量：[script_itemid]. 如果无法运行对应NPC,请手动添加NPC文件进行业务处理.",
							itemId, npcId));
		}
		ScriptEngine engine = NPCScriptManager.getInstance()
				.start(c, npcId, -1);
		if (engine != null) {
			engine.put("script_itemid", itemId);
		}
	}
}