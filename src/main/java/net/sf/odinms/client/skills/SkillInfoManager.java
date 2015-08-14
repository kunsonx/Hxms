/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client.skills;

import java.util.Map;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.provider.MapleSQLData;
import net.sf.odinms.provider.MapleSQLDataValue;
import net.sf.odinms.provider.MapleSkillInfo;
import org.hibernate.Session;

/**
 *
 * @author Admin
 */
public class SkillInfoManager {

    private static Map<Integer, MapleSkillInfo> cache = new java.util.concurrent.ConcurrentHashMap<Integer, MapleSkillInfo>();

    private static MapleSkillInfo getSkillInfoFromData(MapleSkillDatabase database) {
        if (database == null) {
            return null;
        }
        MapleSkillInfo info = new MapleSkillInfo(database.getId(), database.getName(), database.getJobid());
        for (MapleSkillDatabaseInfo dinfoDatabaseInfo : database.getValues()) {
            info.doCreate(dinfoDatabaseInfo.getPath().split("/"), dinfoDatabaseInfo.getValue(), 0, info);
        }

        return info;
    }

    public static MapleSkillDatabase getDatabaseObj(MapleSkillInfo info) {
        MapleSkillDatabase base = new MapleSkillDatabase();
        base.setId(info.getId());
        base.setJobid(info.getJobid());
        base.setName(info.getName());

        for (MapleSQLDataValue value : info.getValues()) {
            MapleSkillDatabaseInfo dainfo = new MapleSkillDatabaseInfo();
            dainfo.setPath(value.getName());
            dainfo.setValue(value.getValue());
            base.getValues().add(dainfo);
        }
        for (MapleSQLData nodeData : info.getNodes()) {
            readNodes(nodeData, base, nodeData.getName());
        }


        return base;
    }

    public static void readNodes(MapleSQLData info, MapleSkillDatabase base, String path) {
        if (info.hasNodes()) {
            for (MapleSQLData mapleSkillData : info.getNodes()) {
                readNodes(mapleSkillData, base, path + "/" + mapleSkillData.getName());
            }
        }
        if (info.hasValues()) {
            for (MapleSQLDataValue value : info.getValues()) {
                MapleSkillDatabaseInfo dainfo = new MapleSkillDatabaseInfo();
                dainfo.setPath(path + "/" + value.getName());
                dainfo.setValue(value.getValue());
                base.getValues().add(dainfo);
            }
        }
        if (!info.hasNodes() && !info.hasValues()) {
            MapleSkillDatabaseInfo dainfo = new MapleSkillDatabaseInfo();
            dainfo.setPath(path);
            base.getValues().add(dainfo);
        }
    }

    public static MapleSkillInfo getSkill(int id) {
        MapleSkillInfo info;
        if (cache.containsKey(id)) {
            info = cache.get(id);
        } else {
            Session session = DatabaseConnection.getSession();
            info = getSkillInfoFromData((MapleSkillDatabase) session.get(MapleSkillDatabase.class, id));
            session.close();
            if (info != null) {
                cache.put(id, info);
            }
        }
        return info;
    }
}
