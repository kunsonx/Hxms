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

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.Guild.ALLIANCE_msg;
import org.apache.log4j.Logger;

/**
 *
 * @author XoticStory.
 */
public class MapleAlliance implements java.io.Serializable {

    public static final long serialVersionUID = 24081985245L;
    private int[] guilds = new int[5];
    private int allianceId = -1;
    private int capacity;
    private String name;
    private String notice = "";
    private String rankTitles[] = new String[5];
    private static Logger log = Logger.getLogger(MapleAlliance.class);

    private MapleAlliance() {
    }

    public MapleAlliance(String name, int id, int guild1, int guild2) {
        this.name = name;
        allianceId = id;
        guilds[0] = guild1;
        guilds[1] = guild2;
        guilds[2] = -1; // UGH GRRRR. LOL
        guilds[3] = -1;
        guilds[4] = -1;
        rankTitles[0] = "族长"; // WTFBBQHAX LOL
        rankTitles[1] = "副族长";
        rankTitles[2] = "成员";
        rankTitles[3] = "成员";
        rankTitles[4] = "成员";
    }

    public static MapleAlliance loadAlliance(int id) {
        // LOAD HERE
        if (id <= 0) {
            return null;
        }
        MapleAlliance alliance = new MapleAlliance();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM alliance WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            alliance.allianceId = id;
            alliance.capacity = rs.getInt("capacity");
            alliance.name = rs.getString("name");
            alliance.notice = rs.getString("notice");
            for (int i = 1; i <= 5; i++) {
                alliance.rankTitles[i - 1] = rs.getString("rank_title" + i);
            }
            for (int i = 1; i <= 5; i++) {
                alliance.guilds[i - 1] = rs.getInt("guild" + i);
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("SELECT COUNT(*) FROM guilds WHERE guildid = ?");
            for (int i = 0; i < alliance.guilds.length; i++) {
                int j = alliance.guilds[i];
                if (j != -1) {
                    ps.setInt(1, j);
                    rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        alliance.guilds[i] = -1;
                    }
                    rs.close();
                }
            }
            ps.close();
            con.close();
        } catch (SQLException e) {
            log.error("载入联盟错误：", e);
        }
        return alliance;
    }

    public static void disbandAlliance(MapleClient c, int allianceId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM `alliance` WHERE id = ?");
            ps.setInt(1, allianceId);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            c.getChannelServer().getWorldInterface().allianceMessage(c.getPlayer().getGuild().getAllianceId(), ALLIANCE_msg.解散联盟(allianceId), -1, -1);
            c.getChannelServer().getWorldInterface().disbandAlliance(allianceId);
        } catch (RemoteException e) {
            ServerExceptionHandler.HandlerRemoteException(e);
            c.getChannelServer().reconnectWorld();
        }
    }

    public static boolean canBeUsedAllianceName(String name) {
        if (name.contains(" ") || name.length() > 12) { // im using starswith because the 'contains' method fails.
            return false;
        }

        boolean ret = true;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM alliance WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = false;
            }
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException e) {
            return false;
        }
        return ret;
    }

    public static MapleAlliance createAlliance(MapleCharacter chr1, MapleCharacter chr2, String name) {
        int id = 0;
        int guild1 = chr1.getGuildid();
        int guild2 = chr2.getGuildid();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO `alliance` (`name`, `guild1`, `guild2`) VALUES (?, ?, ?)");
            ps.setString(1, name);
            ps.setInt(2, guild1);
            ps.setInt(3, guild2);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            id = rs.getInt(1);
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        MapleAlliance alliance = new MapleAlliance(name, id, guild1, guild2);
        try {
            WorldChannelInterface wci = chr1.getClient().getChannelServer().getWorldInterface();
            wci.setGuildAllianceId(guild1, id);
            wci.setGuildAllianceId(guild2, id);
            chr1.setAllianceRank(1);
            chr1.saveGuildStatus();
            chr2.setAllianceRank(2);
            chr2.saveGuildStatus();
            wci.addAlliance(id, alliance);
            wci.allianceMessage(id, ALLIANCE_msg.makeNewAlliance(alliance, chr1.getClient()), -1, -1);
        } catch (RemoteException e) {
            ServerExceptionHandler.HandlerRemoteException(e);
            chr1.getClient().getChannelServer().reconnectWorld();
            return null;
        }
        return alliance;
    }

    public void saveToDB() {
        log.debug("联盟数据保存!!");

        StringBuilder sb = new StringBuilder();
        sb.append("capacity = ?, ");
        sb.append("notice = ?, ");
        for (int i = 1; i <= 5; i++) {
            sb.append("rank_title").append(i).append(" = ?, ");
        }
        for (int i = 1; i <= 5; i++) {
            sb.append("guild").append(i).append(" = ?, ");
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE `alliance` SET " + sb.toString() + " WHERE id = ?");
            ps.setInt(1, this.capacity);
            ps.setString(2, this.notice);
            for (int i = 0; i < rankTitles.length; i++) {
                ps.setString(i + 3, rankTitles[i]);
            }
            for (int i = 0; i < guilds.length; i++) {
                ps.setInt(i + 8, guilds[i]);
            }
            ps.setInt(13, this.allianceId);
            ps.executeQuery();
            ps.close();
            con.close();
        } catch (SQLException e) {
        }
    }

    public boolean addRemGuildFromDB(int gid, boolean add) {

        boolean ret = false;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM alliance WHERE id = ?");
            ps.setInt(1, this.allianceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int avail = -1;
                for (int i = 1; i <= 5; i++) {
                    int guildId = rs.getInt("guild" + i);
                    if (add) {
                        if (guildId == -1) {
                            avail = i;
                            break;
                        }
                    } else {
                        if (guildId == gid) {
                            avail = i;
                            break;
                        }
                    }
                }
                rs.close();
                if (avail != -1) { // empty slot
                    PreparedStatement ps1 = con.prepareStatement("UPDATE alliance SET guild" + avail + " = ? WHERE id = ?");
                    if (add) {
                        ps1.setInt(1, gid);
                    } else {
                        ps1.setInt(1, -1);
                    }
                    ps1.setInt(2, this.allianceId);
                    ps1.executeUpdate();
                    ret = true;
                    ps1.close();
                }
            }
            ps.close();
            con.close();
        } catch (SQLException e) {
        }
        return ret;
    }

    public boolean removeGuild(int gid) {
        synchronized (guilds) {
            int gIndex = getGuildIndex(gid);
            if (gIndex != -1) {
                guilds[gIndex] = -1;
            }
            return addRemGuildFromDB(gid, false);
        }
    }

    public boolean addGuild(int gid) {
        synchronized (guilds) {
            if (getGuildIndex(gid) == -1) {
                int emptyIndex = getGuildIndex(-1);
                if (emptyIndex != -1) {
                    guilds[emptyIndex] = gid;
                    return addRemGuildFromDB(gid, true);
                }
            }
        }
        return false;
    }

    public int getGuildIndex(int gid) {
        for (int i = 0; i < guilds.length; i++) {
            if (guilds[i] == gid) {
                return i;
            }
        }
        return -1;
    }

    public void setRankTitle(String[] ranks) {
        rankTitles = ranks;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE `alliance` SET `rank_title1` =  ?, `rank_title2` =  ?, `rank_title3` =  ?, `rank_title4` =  ?, `rank_title5` =  ?, `capacity` = ? where `id` = ?");
            for (int i1 = 1; i1 <= 5; i1++) {
                ps.setString(i1, rankTitles[i1 - 1]);
            }
            ps.setInt(6, capacity);
            ps.setInt(7, getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setNotice(String notice) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE `alliance` SET `notice` = ? where id = ?");
            ps.setString(1, notice);
            ps.setInt(2, getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return allianceId;
    }

    public String getName() {
        return name;
    }

    public String getRankTitle(int rank) {
        return rankTitles[rank - 1];
    }

    public String getAllianceNotice() {
        return notice;
    }

    public List<Integer> getGuilds() {
        List<Integer> guilds_ = new LinkedList<Integer>();
        for (int guild : guilds) {
            if (guild != -1) {
                guilds_.add(guild);
            }
        }
        return guilds_;
    }

    public String getNotice() {
        String getvip1 = null;
        try {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("SELECT notice from alliance where id=?");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                getvip1 = rs.getString("notice");
            }

            ps.close();
            rs.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.notice = getvip1;
        return this.notice;
    }

    public void increaseCapacity(int inc) {
        capacity += inc;
    }

    public int getCapacity() {
        return capacity;
    }

    public void Alliance_Select() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM alliance WHERE id = ?");
            ps.setInt(1, getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            log.debug("删除联盟出错" + ex);
        }
    }

    public int getGuildIdByGuildName(String name) {
        int id = -1;
        PreparedStatement ps;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("guildid");
            } else {
                log.debug("查询不到此家族");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            log.debug("通过家族Id查询家族出错" + ex);
        }
        return id;
    }
}