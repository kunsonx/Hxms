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
package net.sf.odinms.net.login;

import net.sf.odinms.net.login.gateway.LoginGateway;
import java.rmi.RemoteException;
import net.sf.odinms.net.login.remote.ChannelLoadInfo;

/**
 * hxms修改 LoginWorker
 *
 * @author hxms
 */
public class LoginWorker implements Runnable {

	private static LoginWorker instance = new LoginWorker();
	public static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(LoginWorker.class);

	private LoginWorker() {
	}

	public static LoginWorker getInstance() {
		return instance;
	}

	public void updateLoad() {
		try {
			LoginServer.getInstance().getWorldInterface().isAvailable();
			ChannelLoadInfo load = LoginServer.getInstance()
					.getWorldInterface().getChannelLoad();
			for (int world = 0; world < load.getWorldCount(); world++) {
				// double loadFactor = 1200 / ((double)
				// LoginServer.getInstance().getUserLimit() /
				// load.getChannelCount(world));
				// loadFactor = 24;//50个人满
				double count = Math.min(50, LoginServer.getInstance()
						.getUserLimit());
				double loadFactor = 180 / count;// 50个人满
				for (int channel = 1; channel <= load.getChannelCount(world); channel++) {
					load.setChannelValue(world, channel, Math.min(200,
							((int) Math.floor((load.getChannelValue(world,
									channel) * loadFactor)))));
				}
			}
			LoginServer.getInstance().setLoad(load);
		} catch (RemoteException ex) {
			log.error("尝试刷新频道信息失败.");
		}
	}

	@Override
	public void run() {
	}
}