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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelDescriptor;

public class MapleGuildCharacter implements java.io.Serializable // alias for a character
{

    public static final long serialVersionUID = 2058609046116597760L;
    private int level, id, jobid;
    private int guildrank, guildid;
    private int allianceRank;
    private boolean online;
    private String name;
    private ChannelDescriptor channel;

    // either read from active character...
    // if it's online
    public MapleGuildCharacter(MapleCharacter c) {
        name = c.getName();
        level = c.getLevel();
        id = c.getId();
        channel = c.getClient().getChannelDescriptor();
        jobid = c.getJob().getId();
        guildrank = c.getGuildrank();
        guildid = c.getGuildid();
        online = true;
        allianceRank = c.getAlliancerank();
    }

    // or we could just read from the database
    public MapleGuildCharacter(int id, int lv, String name, ChannelDescriptor channel, int job, int rank, int gid, boolean on, int allianceRank) {
        this.level = lv;
        this.id = id;
        this.name = name;
        this.channel = channel;
        jobid = job;
        online = on;
        guildrank = rank;
        guildid = gid;
        this.allianceRank = allianceRank;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int l) {
        level = l;
    }

    public int getId() {
        return id;
    }

    public void setChannel(int ch) {
        channel.setId(ch);
    }

    public int getChannel() {
        return channel.getId();
    }

    public ChannelDescriptor getChannelDescriptor() {
        return channel;
    }

    public int getJobId() {
        return jobid;
    }

    public void setJobId(int job) {
        jobid = job;
    }

    public int getGuildId() {
        return guildid;
    }

    public void setGuildId(int gid) {
        guildid = gid;
    }

    public void setGuildRank(int rank) {
        guildrank = rank;
    }

    public int getGuildRank() {
        return guildrank;
    }

    public boolean isOnline() {
        return online;
    }

    public String getName() {
        return name;
    }

    public void setAllianceRank(int rank) {
        allianceRank = rank;

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET `alliancerank` = ? where id = ?");
            ps.setInt(1, this.allianceRank);
            ps.setInt(2, getId());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getAllianceRank() {
        int getvip1 = 0;
        try {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("SELECT alliancerank from characters where id = ?");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                getvip1 = rs.getInt("alliancerank");
            }

            ps.close();
            rs.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.allianceRank = getvip1;
        return allianceRank;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MapleGuildCharacter)) {
            return false;
        }

        MapleGuildCharacter o = (MapleGuildCharacter) other;
        return (o.getId() == id && o.getName().equals(name));
    }

    public void setOnline(boolean f) {
        online = f;
    }
}
