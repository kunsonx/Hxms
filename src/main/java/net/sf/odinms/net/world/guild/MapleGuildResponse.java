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

package net.sf.odinms.net.world.guild;

import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.Guild.MapleGuild_Msg;

public enum MapleGuildResponse {

    NOT_IN_CHANNEL(0x30),
    ALREADY_IN_GUILD(0x2E),//094
    NOT_IN_GUILD(0x2d);
    private int value;

    private MapleGuildResponse(int val) {
        value = val;
    }

    public int getValue() {
        return value;
    }

    public MaplePacket getPacket() {
        return MapleGuild_Msg.genericGuildMessage((byte) value);
    }
}
