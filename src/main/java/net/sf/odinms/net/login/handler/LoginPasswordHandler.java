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
package net.sf.odinms.net.login.handler;

import java.util.Calendar;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.net.login.LoginWorker;
import net.sf.odinms.tools.DateUtil;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class LoginPasswordHandler implements MaplePacketHandler {

	@Override
	public boolean validateState(MapleClient c) {
		return !c.isLoggedIn();
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// 01 00
		// 09 00 39 34 38 34 35 39 35 39 39
		// 07 00 34 37 34 32 34 38 38
		// 02 00 4C 4F 4F 50 F0 10 A3 58 00 00 00 00 D6 A0 00 00 00 00
		slea.skip(21);
		String login = slea.readMapleAsciiString().trim();
		String pwd = slea.readMapleAsciiString().trim();

		c.setAccountName(login);

		int loginok = 0;
		boolean ipBan = c.hasBannedIP();
		boolean macBan = false;

		loginok = c.login(login, pwd, ipBan || macBan);
		ipBan = c.hasBannedIP();
		macBan = false;
		Calendar tempbannedTill = c.getTempBanCalendar();
		if (loginok == 0 && (ipBan || macBan)) {
			loginok = 3;
			if (macBan) {
				String[] ipSplit = c.getRemoteAddress().split(":");
				MapleCharacter.ban(ipSplit[0],
						"Enforcing account ban, account " + login, false);
			}
		}

		if (tempbannedTill != null && tempbannedTill.getTimeInMillis() != 0) {
			long tempban = DateUtil.getFileTimestamp(tempbannedTill
					.getTimeInMillis());
			byte reason = c.getBanReason();
			c.getSession()
					.write(MaplePacketCreator.getTempBan(tempban, reason));
			return;
		} else if (loginok != 0) {
			c.getSession().write(MaplePacketCreator.getLoginFailed(loginok));
			return;
		}
		if (c.getGender() == -1) {
			c.updateLoginState(MapleClient.ENTERING_PIN);
			c.getSession().write(MaplePacketCreator.genderNeeded(c));
		} else {
			c.getSession().write(MaplePacketCreator.getAuthSuccess(c));
			c.updateLoginState(MapleClient.LOGIN_LOGGEDIN);
			for (int i = 0; i < LoginServer.getInstance().getLoad()
					.getWorldCount(); i++) {
				c.getSession().write(
						MaplePacketCreator.getServerList(i, LoginServer
								.getInstance().getServerName(), LoginServer
								.getInstance().getLoad()));
			}
			c.getSession().write(MaplePacketCreator.getEndOfServerList());
			LoginWorker.getInstance().updateLoad();
		}
	}
}