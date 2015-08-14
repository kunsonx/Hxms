/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools.ext.wz;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.odinms.client.ItemOption;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.skills.MapleSkillDatabase;
import net.sf.odinms.client.skills.SkillInfoManager;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataDirectoryEntry;
import net.sf.odinms.provider.MapleDataFileEntry;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.provider.MapleSQLData;
import net.sf.odinms.provider.MapleSkillInfo;
import net.sf.odinms.provider.wz.MapleDataType;
import net.sf.odinms.server.CashItemInfo;
import net.sf.odinms.server.CashPackageInfo;
import net.sf.odinms.server.CashPackageList;
import net.sf.odinms.server.MapleItemIcon;
import net.sf.odinms.server.MapleItemInventryType;
import net.sf.odinms.server.ORM.MapleAndroidInfo;
import net.sf.odinms.server.ORM.MapleAndroidInfoFace;
import net.sf.odinms.server.ORM.MapleAndroidInfoHair;
import net.sf.odinms.server.ORM.MapleEquipStats;
import net.sf.odinms.server.ORM.MapleExpCardInfo;
import net.sf.odinms.server.ORM.MapleExpCardListInfo;
import net.sf.odinms.server.ORM.MapleItemMeso;
import net.sf.odinms.server.ORM.MapleItemName;
import net.sf.odinms.server.ORM.MapleItemPrice;
import net.sf.odinms.server.ORM.MapleItemSlotMax;
import net.sf.odinms.server.ORM.MapleItemType;
import net.sf.odinms.server.ORM.MapleItemWholePrice;
import net.sf.odinms.server.ORM.MapleScriptedItemNpc;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleMonsterSkill;
import net.sf.odinms.server.life.MapleMonsterStats;
import net.sf.odinms.server.life.MapleNPCStats;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.server.maps.MapleMapInfo;
import net.sf.odinms.server.maps.MapleReactorStats;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.logicalcobwebs.proxool.HouseKeeperController;

/**
 *
 * @author Administrator
 */
public class CreateDatabaseInfo {

    /**
     * @param args the command line arguments
     */
    private static Logger log = Logger.getLogger(CreateDatabaseInfo.class);
    public static SessionFactory sessionFactory;
    private static MapleDataProvider dataProvider;
    protected static MapleDataProvider itemData;
    protected static MapleDataProvider equipData;
    protected static MapleDataProvider stringData;
    private static List<MapleData> ItemsData;
    private static MapleDataProvider datasource = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Skill.wz"));
    private static MapleData stringData__ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Skill.img");
    private static MapleDataProvider mdata = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Mob.wz"));
    private static MapleDataProvider ndata = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Npc.wz"));
    private static MapleDataProvider stringDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz"));
    private static MapleData mobStringData = stringDataWZ.getData("Mob.img");
    private static MapleData npcStringData = stringDataWZ.getData("Npc.img");
    private static Map<Integer, MapleMonsterStats> monsterStats = new HashMap<Integer, MapleMonsterStats>();
    private static MapleDataProvider rdata = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Reactor.wz"));
    private static MapleDataProvider mapdata = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Map.wz/Map"));
    private static Map<Integer, MapleReactorStats> reactorStats = new HashMap<Integer, MapleReactorStats>();

    public static void main(String[] args) throws ClassNotFoundException {
        itemData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Item.wz"));
        equipData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Character.wz"));
        stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz"));
        log.info("程序已载入。测试数据库连接。。。");
        try {
            Session session = DatabaseConnection.getSession();
            HouseKeeperController.stopall();
            Transaction transaction = session.beginTransaction();
            System.out.println("打开数据库连接：" + session.isOpen());
            log.info("数据库测试成功。");
            log.info("重新创建数据库成功。");

            /*
             * Commodity.img
             */
            /*
             * 切换数据目录
             */
            dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "Etc.wz"));

            session.createQuery("DELETE FROM MapleItemInventryType").executeUpdate();
            session.createQuery("DELETE FROM CashItemInfo").executeUpdate();
            session.createQuery("DELETE FROM CashPackageList").executeUpdate();
            session.createQuery("DELETE FROM ItemOption").executeUpdate();
            session.createQuery("DELETE FROM MapleScriptedItemNpc").executeUpdate();
            session.createQuery("DELETE FROM MapleExpCardListInfo").executeUpdate();
            session.createQuery("DELETE FROM MapleItemSlotMax").executeUpdate();
            session.createQuery("DELETE FROM MapleItemMeso").executeUpdate();
            session.createQuery("DELETE FROM MapleItemWholePrice").executeUpdate();
            session.createQuery("DELETE FROM MapleItemType").executeUpdate();
            session.createQuery("DELETE FROM MapleItemPrice").executeUpdate();
            session.createQuery("DELETE FROM MapleAndroidInfo").executeUpdate();
            session.createQuery("DELETE FROM MapleSkillDatabase").executeUpdate();
            session.createQuery("DELETE FROM MapleEquipStats").executeUpdate();
            session.createQuery("DELETE FROM MapleMonsterStats").executeUpdate();
            session.createQuery("DELETE FROM MapleNPCStats").executeUpdate();
            session.createQuery("DELETE FROM MapleReactorStats").executeUpdate();
            session.createQuery("DELETE FROM MapleMapInfo").executeUpdate();
            session.createQuery("DELETE FROM MapleItemName").executeUpdate();

            for (MapleMapInfo string : getMapleMapInfos()) {
                session.saveOrUpdate(string);
            }

            System.gc();
            for (MapleReactorStats mapleReactorStats : getReactorStatse()) {
                session.saveOrUpdate(mapleReactorStats);
            }
            System.gc();

            for (MapleNPCStats mapleMonsterStats : getNpcStatse()) {
                session.save(mapleMonsterStats);
            }
            System.gc();

            for (MapleMonsterStats mapleMonsterStats : getMonsterStatse()) {
                session.saveOrUpdate(mapleMonsterStats);
            }
            System.gc();

            for (MapleItemInventryType mapleItemInventryType : getItemsType()) {
                session.saveOrUpdate(mapleItemInventryType);
            }
            System.gc();
            for (CashItemInfo cashItemInfo : getCommodity()) {
                session.saveOrUpdate(cashItemInfo);
            }
            System.gc();
            for (CashPackageList cashPackageList : getCashpackage()) {
                session.saveOrUpdate(cashPackageList);
            }
            System.gc();
            for (ItemOption itemOption : getOptions()) {
                session.saveOrUpdate(itemOption);
            }
            System.gc();
            for (MapleScriptedItemNpc mapleScriptedItemNpc : getScriptedItemNpc()) {
                session.saveOrUpdate(mapleScriptedItemNpc);
            }
            System.gc();
            for (MapleExpCardListInfo mapleExpCardListInfo : getExpCardListInfos()) {
                session.saveOrUpdate(mapleExpCardListInfo);
            }
            System.gc();

            for (MapleItemSlotMax max : getItemSlotMaxs()) {
                session.saveOrUpdate(max);
            }
            System.gc();
            for (MapleItemMeso meso : getitemMesos()) {
                session.saveOrUpdate(meso);
            }
            System.gc();
            for (MapleItemWholePrice wp : getItemWholePrices()) {
                session.saveOrUpdate(wp);
            }
            System.gc();
            for (MapleItemType type : getMapleItemTypes()) {
                session.saveOrUpdate(type);
            }
            System.gc();
            for (MapleItemPrice ipItemPrice : getItemPrices()) {
                session.saveOrUpdate(ipItemPrice);
            }

            System.gc();
            for (MapleAndroidInfo mapleAndroidInfo : getAndroidInfos()) {
                session.saveOrUpdate(mapleAndroidInfo);
            }
            System.gc();
            for (MapleSkillDatabase mapleSkillInfo : getSkillInfo()) {
                session.saveOrUpdate(mapleSkillInfo);
            }
            System.gc();
            for (MapleItemName mapleItemName : getMapleItemNames()) {
                session.saveOrUpdate(mapleItemName);
            }
            System.gc();
            transaction.commit();
            session.close();
            System.gc();
            MapleEquipStats.deleteall();
            for (MapleEquipStats mapleEquipStats : getEquipStatses()) {
                MapleEquipStats.savetodb(mapleEquipStats);
            }
            MapleItemIcon.writeToDb();
            log.info("所有导出工作已完成。");
        } catch (Exception e) {
            log.error("错误：", e);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="生成类信息块代码">
    public static Collection<MapleItemName> getMapleItemNames() {
        Map<Integer, MapleItemName> list = new HashMap<Integer, MapleItemName>();
        String[] files = new String[]{"Cash.img", "Consume.img", "Eqp.img", "Etc.img", "Ins.img", "Pet.img"};
        for (String string : files) {
            MapleData strdata = stringDataWZ.getData(string);
            for (MapleData o_data : strdata.getChildren()) {
                loopData(list, o_data);
            }
        }
        return list.values();
    }

    public static void loopData(Map<Integer, MapleItemName> list, MapleData data) {
        if (data.getChildren().size() > 0) {
            for (MapleData t_data : data.getChildren()) {
                loopData(list, t_data);
            }
        } else {
            if (data.getName().equals("name")) {
                MapleItemName name = new MapleItemName();
                name.setId(Integer.parseInt(data.getParent().getName()));
                name.setName(MapleDataTool.getString("name", (MapleData) data.getParent(), null));
                if (/*!list.containsKey(name.getId()) &&*/name.getName() != null) {
                    list.put(name.getId(), name);
                }
            }
        }
    }

    public static Collection<MapleMapInfo> getMapleMapInfos() {
        MapleMapFactory iinfo = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")));
        ArrayList<MapleMapInfo> infos = new ArrayList<MapleMapInfo>();
        Pattern pattern = Pattern.compile("([0-9]*).img");
        for (MapleDataDirectoryEntry mapleDataDirectoryEntry : mapdata.getRoot().getSubdirectories()) {
            for (MapleDataFileEntry mapleDataFileEntry : mapleDataDirectoryEntry.getFiles()) {
                Matcher matcher = pattern.matcher(mapleDataFileEntry.getName());
                if (matcher.matches()) {
                    System.out.println("load Map:" + matcher.group(1));
                    MapleMapInfo info = iinfo.getMapInfo(Integer.parseInt(matcher.group(1)));
                    if (info != null) {
                        infos.add(info);
                    }
                }
            }
        }
        return infos;
    }

    public static Collection<MapleReactorStats> getReactorStatse() {
        Pattern pattern = Pattern.compile("([0-9]*).img");
        for (MapleDataFileEntry mapleDataFileEntry : rdata.getRoot().getFiles()) {
            Matcher matcher = pattern.matcher(mapleDataFileEntry.getName());
            if (matcher.matches()) {
                System.out.println("load Reactor:" + matcher.group(1));
                getReactor(Integer.parseInt(matcher.group(1)));
            }
        }
        return reactorStats.values();
    }

    public static Collection<MapleMonsterStats> getMonsterStatse() {
        Pattern pattern = Pattern.compile("([0-9]*).img");
        for (MapleDataFileEntry mapleDataFileEntry : mdata.getRoot().getFiles()) {
            Matcher matcher = pattern.matcher(mapleDataFileEntry.getName());
            if (matcher.matches()) {
                getMonsterFormWz(Integer.parseInt(matcher.group(1)));
            }
        }
        return monsterStats.values();
    }

    public static Collection<MapleNPCStats> getNpcStatse() {
        Map<Integer, MapleNPCStats> list = new HashMap<Integer, MapleNPCStats>();
        Pattern pattern = Pattern.compile("([0-9]*).img");
        for (MapleDataFileEntry mapleDataFileEntry : ndata.getRoot().getFiles()) {
            Matcher matcher = pattern.matcher(mapleDataFileEntry.getName());
            if (matcher.matches()) {
                list.put(Integer.parseInt(matcher.group(1)), getNPCFromWz(Integer.parseInt(matcher.group(1))));
            }
        }
        return list.values();
    }

    public static ArrayList<MapleSkillDatabase> getSkillInfo() {
        ArrayList<MapleSkillDatabase> infos = new ArrayList<MapleSkillDatabase>();
        Pattern pattern = Pattern.compile("([0-9]*).img");
        for (MapleDataFileEntry mapleDataFileEntry : datasource.getRoot().getFiles()) {
            Matcher matcher = pattern.matcher(mapleDataFileEntry.getName());
            if (matcher.matches()) {
                System.out.println("处理文件：" + matcher.group(1));
                readWzSkillInfo(datasource.getData(mapleDataFileEntry.getName()), Integer.parseInt(matcher.group(1)), infos);
            }
        }
        return infos;
    }

    public static ArrayList<MapleSkillDatabase> readWzSkillInfo(MapleData data, int jobid, ArrayList<MapleSkillDatabase> infos) {
        MapleData skills = data.getChildByPath("skill");
        for (MapleData mapleData : skills.getChildren()) {
            MapleSkillInfo info = new MapleSkillInfo(Integer.parseInt(mapleData.getName()), getSkillName(Integer.parseInt(mapleData.getName())), jobid);

            System.out.println("解析数据：" + info);
            /* 
             * for (MapleData data1 : mapleData.getChildren()) {
             if (data1.getType().equals(MapleDataType.PROPERTY)) {
             MapleSkillData sdata = new MapleSkillData();//建立子节点对象
             info.addNote(sdata);//添加到父节点
             sdata.setName(data1.getName());//设置子节点节点名
             readValues(sdata, data1);//读取字节所有字串符和int类型数值
             System.out.println("子节点：" + data1.getName());
             }
             }
             * /
             */
            readValues(info, mapleData);


            infos.add(SkillInfoManager.getDatabaseObj(info));
        }
        return infos;
    }

    public static String getSkillName(int id) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        MapleData skillroot = stringData__.getChildByPath(strId);
        if (skillroot != null) {
            return MapleDataTool.getString(skillroot.getChildByPath("name"), "");
        }
        return null;
    }

    public static void readValues(MapleSQLData info, MapleData data) {
        int animationTime = 0;
        for (MapleData cData : data.getChildren()) {
            if (cData.getType().equals(MapleDataType.INT) || cData.getType().equals(MapleDataType.STRING)) {
                System.out.println(cData.getName() + ":" + cData.getData().toString());
                info.addValues(cData.getName(), cData.getData().toString());
            } else if (cData.getType().equals(MapleDataType.VECTOR)) {
                Point point = (Point) cData.getData();
                info.addValues(cData.getName(), point.x + "/" + point.y);
            } else if (cData.getType().equals(MapleDataType.PROPERTY)) {
                MapleSQLData sdata = new MapleSQLData();//建立子节点对象
                info.addNote(sdata);//添加到父节点
                sdata.setName(cData.getName());//设置子节点节点名
                readValues(sdata, cData);//读取字节所有字串符和int类型数值
            }


            //特殊统加处理
            if (data.getName().equals("effect")) {
                animationTime += MapleDataTool.getIntConvert("delay", cData, 0);
            }
        }

        if (data.getName().equals("effect")) {
            info.addValues("animationTime", String.valueOf(animationTime));
            System.out.println("animationTime:" + animationTime);
        }
    }

    private static Collection<MapleAndroidInfo> getAndroidInfos() {
        ArrayList<MapleAndroidInfo> infos = new ArrayList<MapleAndroidInfo>();
        MapleDataDirectoryEntry android = (MapleDataDirectoryEntry) dataProvider.getRoot().getEntry("Android");
        for (MapleDataFileEntry fileEntry : android.getFiles()) {
            final MapleData iz = dataProvider.getData("Android/" + fileEntry.getName());
            MapleAndroidInfo ainfo = new MapleAndroidInfo();
            ainfo.setId(Integer.parseInt(fileEntry.getName().substring(0, 4)));
            for (MapleData ds : iz.getChildByPath("costume/hair")) {
                MapleAndroidInfoHair hair = new MapleAndroidInfoHair();
                hair.setValue(MapleDataTool.getInt(ds, 30000));
                ainfo.getHairs().add(hair);
            }
            for (MapleData ds : iz.getChildByPath("costume/face")) {
                MapleAndroidInfoFace face = new MapleAndroidInfoFace();
                face.setValue(MapleDataTool.getInt(ds, 20000));
                ainfo.getFaces().add(face);
            }
            infos.add(ainfo);
        }
        return infos;
    }

    private static Collection<MapleEquipStats> getEquipStatses() {
        Map<Integer, MapleEquipStats> list = new HashMap<Integer, MapleEquipStats>();
        for (MapleData data : getItemsData()) {
            MapleData infoData = data.getChildByPath("info");
            if (infoData != null) {
                String strid = data.getName();
                if (strid.endsWith(".img")) {
                    strid = strid.substring(0, strid.length() - 4);
                }
                int itemid;
                try {
                    itemid = Integer.parseInt(strid);
                } catch (Exception ex) {
                    continue;
                }
                MapleEquipStats stats = new MapleEquipStats();
                stats.setItemid(itemid);

                for (MapleData _data : infoData.getChildren()) {
                    if (_data.getName().startsWith("inc")) {
                        stats.add(_data.getName().substring(3), Integer.parseInt(_data.getData().toString()));
                    }
                }

                stats.add("tuc", MapleDataTool.getInt("tuc", infoData, 0));
                stats.add("reqLevel", MapleDataTool.getInt("reqLevel", infoData, 0));
                stats.add("reqJob", MapleDataTool.getInt("reqJob", infoData, 0));
                stats.add("reqSTR", MapleDataTool.getInt("reqSTR", infoData, 0));
                stats.add("reqDEX", MapleDataTool.getInt("reqDEX", infoData, 0));
                stats.add("reqINT", MapleDataTool.getInt("reqINT", infoData, 0));
                stats.add("reqLUK", MapleDataTool.getInt("reqLUK", infoData, 0));
                stats.add("cash", MapleDataTool.getInt("cash", infoData, 0));
                stats.add("cursed", MapleDataTool.getInt("cursed", infoData, 0));
                stats.add("success", MapleDataTool.getInt("success", infoData, 0));
                stats.add("durability", MapleDataTool.getInt("durability", infoData, -1)); //耐久度
                if (infoData.getChildByPath("level") != null) {
                    stats.add("skilllevel", 1);
                } else {
                    stats.add("skilllevel", 0);
                }
                System.out.println(itemid);
                list.put(itemid, stats);
            }
        }
        System.out.println("共有：" + list.size());
        return list.values();
    }

    private static Collection<MapleItemMeso> getitemMesos() {
        Map<Integer, MapleItemMeso> list = new HashMap<Integer, MapleItemMeso>();
        for (MapleData data : getItemsData()) {
            String strid = data.getName();
            if (strid.endsWith(".img")) {
                strid = strid.substring(0, strid.length() - 4);
            }
            int itemid;
            try {
                itemid = Integer.parseInt(strid);
            } catch (Exception ex) {
                continue;
            }
            MapleData cdata = data.getChildByPath("info/meso");
            int meso = -1;
            if (cdata != null) {
                meso = Integer.parseInt(cdata.getData().toString());
                System.out.println(meso);
            }
            MapleItemMeso mmeso = new MapleItemMeso();
            mmeso.setItemid(itemid);
            mmeso.setMeso(meso);
            if (meso != -1) {
                list.put(itemid, mmeso);
            }
        }
        return list.values();
    }

    private static Collection<MapleItemSlotMax> getItemSlotMaxs() {
        Map<Integer, MapleItemSlotMax> list = new HashMap<Integer, MapleItemSlotMax>();
        for (MapleData data : getItemsData()) {
            MapleData cdata = data.getChildByPath("info/slotMax");
            if (cdata != null) {
                String strid = data.getName();
                if (strid.endsWith(".img")) {
                    strid = strid.substring(0, strid.length() - 4);
                }
                int itemid = Integer.parseInt(strid);
                System.out.println("slotMax:" + cdata.getData().toString());
                int value = Integer.parseInt(cdata.getData().toString());
                MapleItemSlotMax sm = new MapleItemSlotMax();
                sm.setItemid(itemid);
                sm.setSlotmax(value);
                list.put(itemid, sm);
            }
        }

        return list.values();
    }

    private static Collection<MapleItemPrice> getItemPrices() {
        Map<Integer, MapleItemPrice> list = new HashMap<Integer, MapleItemPrice>();
        for (MapleData data : getItemsData()) {
            MapleData cdata = data.getChildByPath("info/unitPrice");
            if (cdata == null) {
                cdata = data.getChildByPath("info/price");
            }
            if (cdata != null) {
                String strid = data.getName();
                if (strid.endsWith(".img")) {
                    strid = strid.substring(0, strid.length() - 4);
                }
                int itemid = Integer.parseInt(strid);
                System.out.println("Price:" + cdata.getData().toString());
                double value = Double.parseDouble(cdata.getData().toString());
                MapleItemPrice sm = new MapleItemPrice();
                sm.setItemid(itemid);
                sm.setPrice(value);
                list.put(itemid, sm);
            }
        }

        return list.values();
    }

    private static Collection<MapleItemWholePrice> getItemWholePrices() {
        Map<Integer, MapleItemWholePrice> list = new HashMap<Integer, MapleItemWholePrice>();
        for (MapleData data : getItemsData()) {
            MapleData cdata = data.getChildByPath("info/price");
            if (cdata != null) {
                String strid = data.getName();
                if (strid.endsWith(".img")) {
                    strid = strid.substring(0, strid.length() - 4);
                }
                int itemid = Integer.parseInt(strid);
                System.out.println("WholePrice:" + cdata.getData().toString());
                int value = Integer.parseInt(cdata.getData().toString());
                MapleItemWholePrice sm = new MapleItemWholePrice();
                sm.setItemid(itemid);
                sm.setWholePrice(value);
                list.put(itemid, sm);
            }
        }

        return list.values();
    }

    private static Collection<MapleItemType> getMapleItemTypes() {
        Map<Integer, MapleItemType> list = new HashMap<Integer, MapleItemType>();
        for (MapleData data : getItemsData()) {
            MapleData cdata = data.getChildByPath("info/islot");
            if (cdata != null) {
                String strid = data.getName();
                if (strid.endsWith(".img")) {
                    strid = strid.substring(0, strid.length() - 4);
                }
                if (strid.contains("_")) {
                    strid = strid.replace("_", "");
                }
                int itemid = Integer.parseInt(strid);
                System.out.println("info/islot:" + cdata.getData().toString());
                MapleItemType sm = new MapleItemType();
                sm.setItemid(itemid);
                sm.setType(cdata.getData().toString());
                list.put(itemid, sm);
            }
        }

        return list.values();
    }

    private static Collection<MapleExpCardListInfo> getExpCardListInfos() {
        ArrayList<MapleExpCardListInfo> list = new ArrayList<MapleExpCardListInfo>();
        for (MapleData mapleData : getItemsData()) {
            MapleData cdata = mapleData.getChildByPath("info/time");
            if (cdata != null) {
                int itemid = Integer.parseInt(mapleData.getName());
                MapleExpCardListInfo ecli = new MapleExpCardListInfo();
                ecli.setItemid(itemid);
                System.out.println("经验卡处理：" + itemid);
                for (MapleData childdata : cdata.getChildren()) {
                    //MON:03-07
                    String[] time = MapleDataTool.getString(childdata).split(":");
                    MapleExpCardInfo eci = new MapleExpCardInfo();
                    eci.setId(MapleDayInt.getDayInt(time[0]));
                    if (eci.getId() == 0) {
                        continue;//无法处理
                    }
                    String[] sp = time[1].split("-");
                    eci.setStarthour(Integer.parseInt(sp[0]));
                    eci.setStophour(Integer.parseInt(sp[1]));
                    ecli.addInstance(eci);

                }
                list.add(ecli);
            }
        }

        return list;
    }

    public static class MapleDayInt {

        public static int getDayInt(String day) {

            if (day.equals("SUN")) {
                return 1;
            } else if (day.equals("MON")) {
                return 2;
            } else if (day.equals("TUE")) {
                return 3;
            } else if (day.equals("WED")) {
                return 4;
            } else if (day.equals("THU")) {
                return 5;
            } else if (day.equals("FRI")) {
                return 6;
            } else if (day.equals("SAT")) {
                return 7;
            }
            System.out.println("不能处理：" + day);
            return 0;
        }
    }

    private static Collection<MapleScriptedItemNpc> getScriptedItemNpc() {
        ArrayList<MapleScriptedItemNpc> list = new ArrayList<MapleScriptedItemNpc>();
        for (MapleData mapleData : getItemsData()) {
            int npcId = MapleDataTool.getInt("spec/npc", mapleData, 0);
            if (npcId > 0) {
                int id = Integer.parseInt(mapleData.getName());
                list.add(
                        new MapleScriptedItemNpc(id, npcId));
            }
        }
        return list;
    }

    private static Collection<MapleData> getItemsData() {
        if (ItemsData != null) {
            return ItemsData;
        }
        ArrayList<MapleData> list = new ArrayList<MapleData>();


        MapleDataDirectoryEntry root = itemData.getRoot(); //整个Item.wz文件夹的根目录
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) { //Item.wz文件夹里的目录 Cash Consume文件夹等
            for (MapleDataFileEntry iFile : topDir.getFiles()) { //各总img文件 例如0501.img
                MapleData cdata = itemData.getData(topDir.getName() + "/" + iFile.getName());

                System.out.println(topDir.getName() + "/" + iFile.getName());

                for (MapleData mapleData : cdata.getChildren()) {
                    int id = 0;
                    try {
                        id = Integer.parseInt(mapleData.getName());
                        list.add(cdata.getChildByPath(mapleData.getName()));
                    } catch (Exception e) {
                        System.out.println("特殊处理：" + cdata.getName());
                        list.add(cdata);
                        break;
                    }
                }
                System.out.println("Handlerd:" + iFile.getName());
            }
        }


        //遍历Character.wz文件夹开始
        root = equipData.getRoot(); //整个Character.wz文件夹的根目录
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) { //Character.wz文件夹里的目录Dragon Android Mechanic文件夹等
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                try {
                    list.add(equipData.getData(topDir.getName() + "/" + iFile.getName()));
                    System.out.println("Handlerd:" + iFile.getName());
                } catch (Exception e) {
                    System.out.println("无法处理文件名：" + iFile.getName());
                }

            }
        }
        ItemsData = list;
        return list;
    }

    private static Collection<MapleItemInventryType> getItemsType() {
        Map<Integer, MapleItemInventryType> maplist = new HashMap<Integer, MapleItemInventryType>();


        MapleDataDirectoryEntry root = itemData.getRoot(); //整个Item.wz文件夹的根目录
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) { //Item.wz文件夹里的目录 Cash Consume文件夹等
            MapleInventoryType current = MapleInventoryType.getByWZName(topDir.getName());
            for (MapleDataFileEntry iFile : topDir.getFiles()) { //各总img文件 例如0501.img
                MapleData cdata = itemData.getData(topDir.getName() + "/" + iFile.getName());

                System.out.println(topDir.getName() + "/" + iFile.getName());

                for (MapleData mapleData : cdata.getChildren()) {
                    int id;
                    try {
                        id = Integer.parseInt(mapleData.getName());
                    } catch (Exception e) {
                        id = Integer.parseInt(iFile.getName().substring(0, iFile.getName().length() - 4));
                    }
                    System.out.println("Handlerd:" + iFile.getName());
                    MapleItemInventryType t = new MapleItemInventryType();
                    t.setId(id);
                    t.setType(current);
                    maplist.put(id, t);
                }
            }
        }


        //遍历Character.wz文件夹开始
        root = equipData.getRoot(); //整个Character.wz文件夹的根目录
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) { //Character.wz文件夹里的目录Dragon Android Mechanic文件夹等
            System.out.println("文件夹：" + topDir.getName());
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                int id;
                try {
                    id = Integer.parseInt(iFile.getName().substring(0, iFile.getName().length() - 4));
                    MapleItemInventryType t = new MapleItemInventryType();
                    t.setId(id);
                    t.setType(MapleInventoryType.EQUIP);
                    maplist.put(id, t);
                    System.out.println("Handlerd:" + iFile.getName());
                } catch (Exception e) {
                    System.out.println("无法处理文件名：" + iFile.getName());
                }

            }
        }
        return maplist.values();
    }

    private static List<ItemOption> getOptions() {
        List<ItemOption> list = new ArrayList<ItemOption>();
        MapleDataProvider dataRoot = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Item.wz"));
        MapleData OptionData = dataRoot.getData("ItemOption.img");
        for (MapleData Option : OptionData.getChildren()) {
            int oid = Integer.valueOf(Option.getName());
            ItemOption O = ItemOption.loadFromData(oid, Option);
            list.add(O);
        }
        return list;
    }

    private static List<CashPackageList> getCashpackage() {
        List<CashPackageList> lists = new ArrayList<CashPackageList>();
        dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "Etc.wz"));
        int SN;
        MapleData currentData = dataProvider.getData("CashPackage.img");
        for (MapleData b : currentData.getChildren()) { //getChildren 子项名
            int itemid = Integer.parseInt(b.getName());  //9100000
            CashPackageList list = new CashPackageList(itemid);
            for (MapleData c : b.getChildren()) { //SN
                for (MapleData d : c.getChildren()) { // 0 1 2 3
                    //很麻烦的取值
                    SN = MapleDataTool.getIntConvert("" + Integer.parseInt(d.getName()), c);
                    list.getSns().add(new CashPackageInfo(SN));
                }
            }
            lists.add(list);
        }
        return lists;
    }

    private static List<CashItemInfo> getCommodity() {
        List<CashItemInfo> list = new ArrayList<CashItemInfo>();
        MapleData a = dataProvider.getData("Commodity.img");
        int size = a.getChildren().size();
        log.info("Commodity.img总项数 ：" + size);

        int i = 0;
        int SN = 0;
        int ItemId = 0;
        int Count = 0;
        int Price = 0;
        int Period = 0;
        int Gender = 0;
        int Priority = 0;
        int Bonus = 0;
        int PbCash = 0;
        int PbPoint = 0;
        int PbGift = 0;
        int Class2 = 0;
        int MaplePoint = 0;
        int Meso = 0;
        int OnSale = 0;
        for (MapleData b : a.getChildren()) { //getChildren 子项名 0 1 10 100 1000 1001
            i += 1;
            for (MapleData c : b.getChildren()) {
                //SN, itemId, count, price, period, gender, priority, bonus, pbCash, pbPoint, pbGift, class2, maplePoint, meso，onSale
                //没有写class 而且的class2的原因是因为会关键字冲突 没办法
                if (c.getName().equals("SN")) {
                    SN = MapleDataTool.getInt(c);
                } else if (c.getName().equals("ItemId")) {
                    ItemId = MapleDataTool.getInt(c);
                } else if (c.getName().equals("Count")) {
                    Count = MapleDataTool.getInt(c);
                } else if (c.getName().equals("Price")) {
                    Price = MapleDataTool.getInt(c);
                } else if (c.getName().equals("Period")) {
                    Period = MapleDataTool.getInt(c);
                } else if (c.getName().equals("Gender")) {
                    Gender = MapleDataTool.getInt(c);
                } else if (c.getName().equals("Priority")) {
                    Priority = MapleDataTool.getInt(c);
                } else if (c.getName().equals("Bonus")) {
                    Bonus = MapleDataTool.getInt(c);
                } else if (c.getName().equals("PbCash")) {
                    PbCash = MapleDataTool.getInt(c);
                } else if (c.getName().equals("PbPoint")) {
                    PbPoint = MapleDataTool.getInt(c);
                } else if (c.getName().equals("PbGift")) {
                    PbGift = MapleDataTool.getInt(c);
                } else if (c.getName().equals("Class")) {
                    Class2 = MapleDataTool.getInt(c);
                } else if (c.getName().equals("MaplePoint")) {
                    MaplePoint = MapleDataTool.getInt(c);
                } else if (c.getName().equals("Meso")) {
                    Meso = MapleDataTool.getInt(c);
                } else if (c.getName().equals("OnSale")) {
                    OnSale = MapleDataTool.getInt(c);
                }
            }
            CashItemInfo cs = new CashItemInfo(SN, ItemId, Count, Price, Period, Gender, OnSale == 1);
            list.add(cs);
        }
        return list;
    }

    public static MapleMonster getMonsterFormWz(int mid) {
        MapleMonsterStats stats = monsterStats.get(Integer.valueOf(mid));
        if (stats == null) {
            MapleData monsterData = mdata.getData(StringUtil.getLeftPaddedStr(Integer.toString(mid) + ".img", '0', 11));
            if (monsterData == null) {
                return null;
            }
            MapleData monsterInfoData = monsterData.getChildByPath("info");
            if (monsterInfoData.getChildByPath("level") == null) {
                return null;
            }

            stats = new MapleMonsterStats(mid);
            stats.setHp(MapleDataTool.getIntConvert("maxHP", monsterInfoData, 100));
            stats.setMp(MapleDataTool.getIntConvert("maxMP", monsterInfoData, 0));
            stats.setExp(MapleDataTool.getIntConvert("exp", monsterInfoData, 0));
            stats.setLevel(MapleDataTool.getIntConvert("level", monsterInfoData));
            stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", monsterInfoData, 0));
            stats.setBoss(MapleDataTool.getIntConvert("boss", monsterInfoData, 0) > 0);
            stats.setFfaLoot(MapleDataTool.getIntConvert("publicReward", monsterInfoData, 0) > 0);
            stats.setUndead(MapleDataTool.getIntConvert("undead", monsterInfoData, 0) > 0);
            stats.setName(MapleDataTool.getString(mid + "/name", mobStringData, "MISSINGNO"));
            stats.setBuffToGive(MapleDataTool.getIntConvert("buff", monsterInfoData, -1));
            stats.setExplosive(MapleDataTool.getIntConvert("explosiveReward", monsterInfoData, 0) > 0);
            MapleData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
            int firstAttack = 0;
            if (firstAttackData != null) {
                if (firstAttackData.getType() == MapleDataType.FLOAT) {
                    firstAttack = Math.round(MapleDataTool.getFloat(firstAttackData));
                } else {
                    firstAttack = MapleDataTool.getInt(firstAttackData);
                }
            }
            stats.setFirstAttack(firstAttack > 0);
            stats.setDropPeriod(MapleDataTool.getIntConvert("dropItemPeriod", monsterInfoData, 0));
            if (stats.isBoss() || mid == 8810018 || mid == 8810026) {
                MapleData hpTagColor = monsterInfoData.getChildByPath("hpTagColor");
                MapleData hpTagBgColor = monsterInfoData.getChildByPath("hpTagBgcolor");
                if (hpTagBgColor == null || hpTagColor == null) {
                    log.trace("Monster " + stats.getName() + " (" + mid + ") flagged as boss without boss HP bars.");
                    stats.setTagColor(0);
                    stats.setTagBgColor(0);
                } else {
                    stats.setTagColor(MapleDataTool.getIntConvert("hpTagColor", monsterInfoData));
                    stats.setTagBgColor(MapleDataTool.getIntConvert("hpTagBgcolor", monsterInfoData));
                }
            }

            for (MapleData idata : monsterData) {
                if (!idata.getName().equals("info")) {
                    int delay = 0;
                    for (MapleData pic : idata.getChildren()) {
                        delay += MapleDataTool.getIntConvert("delay", pic, 0);
                    }
                    stats.setAnimationTime(idata.getName(), delay);
                }
            }

            MapleData reviveInfo = monsterInfoData.getChildByPath("revive");
            if (reviveInfo != null) {
                List<Integer> revives = new LinkedList<Integer>();
                for (MapleData data_ : reviveInfo) {
                    revives.add(MapleDataTool.getInt(data_));
                }
                stats.setRevives(revives);
            }

            MapleLifeFactory.decodeElementalString(stats, MapleDataTool.getString("elemAttr", monsterInfoData, ""));

            MapleData monsterSkillData = monsterInfoData.getChildByPath("skill");
            if (monsterSkillData != null) {
                int i = 0;
                List<MapleMonsterSkill> skills = new ArrayList<MapleMonsterSkill>();
                while (monsterSkillData.getChildByPath(Integer.toString(i)) != null) {
                    skills.add(new MapleMonsterSkill(Integer.valueOf(MapleDataTool.getInt(i + "/skill", monsterSkillData, 0)), Integer.valueOf(MapleDataTool.getInt(i + "/level", monsterSkillData, 0))));
                    i++;
                }
                stats.setSkills(skills);
            }
            MapleData banishData = monsterInfoData.getChildByPath("ban");
            if (banishData != null) {
                stats.setBanishInfo(new MapleLifeFactory.BanishInfo(MapleDataTool.getString("banMsg", banishData), MapleDataTool.getInt("banMap/0/field", banishData, -1), MapleDataTool.getString("banMap/0/portal", banishData, "sp")));
            }
            monsterStats.put(Integer.valueOf(mid), stats);
        }
        MapleMonster ret = new MapleMonster(mid, stats);
        return ret;
    }

    public static MapleNPCStats getNPCFromWz(int nid) {
        return new MapleNPCStats(nid, MapleDataTool.getString(nid + "/name", npcStringData, "MISSINGNO"));
    }

    public static MapleReactorStats getReactor(int rid) {
        MapleReactorStats stats = reactorStats.get(Integer.valueOf(rid));
        if (stats == null) {
            int infoId = rid;
            MapleData reactorData = rdata.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
            MapleData link = reactorData.getChildByPath("info/link");
            if (link != null) {
                infoId = MapleDataTool.getIntConvert("info/link", reactorData);
                stats = reactorStats.get(Integer.valueOf(infoId));
            }
            MapleData activateOnTouch = reactorData.getChildByPath("info/activateByTouch");
            boolean loadArea = false;
            if (activateOnTouch != null) {
                loadArea = MapleDataTool.getInt("info/activateByTouch", reactorData, 0) != 0;
            }
            if (stats == null) {
                reactorData = rdata.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
                MapleData reactorInfoData = reactorData.getChildByPath("0/event/0");
                stats = new MapleReactorStats(rid);
                if (reactorInfoData != null) {
                    boolean areaSet = false;
                    int i = 0;
                    while (reactorInfoData != null && reactorInfoData.getChildByPath("type") != null) {
                        Pair<Integer, Integer> reactItem = null;
                        int type = MapleDataTool.getIntConvert("type", reactorInfoData);
                        if (type == 100) { //reactor waits for item
                            if (reactorInfoData.getChildByPath("0") == null || reactorInfoData.getChildByPath("1") == null) {
                                break;
                            }
                            reactItem = new Pair<Integer, Integer>(MapleDataTool.getIntConvert("0", reactorInfoData), MapleDataTool.getIntConvert("1", reactorInfoData));
                            if (!areaSet || loadArea) { //only set area of effect for item-triggered reactors once
                                stats.setTL(MapleDataTool.getPoint("lt", reactorInfoData));
                                stats.setBR(MapleDataTool.getPoint("rb", reactorInfoData));
                                areaSet = true;
                            }
                        }
                        byte nextState = (byte) MapleDataTool.getIntConvert("state", reactorInfoData);
                        stats.addState((byte) i, type, reactItem, nextState);
                        i++;
                        reactorInfoData = reactorData.getChildByPath(i + "/event/0");
                    }
                } else { //sit there and look pretty; likely a reactor such as Zakum/Papulatus doors that shows if player can enter
                    stats.addState((byte) 0, 999, null, (byte) 0);
                }

                reactorStats.put(Integer.valueOf(infoId), stats);
                if (rid != infoId) {
                    reactorStats.put(Integer.valueOf(rid), stats);
                }
            } else { // stats exist at infoId but not rid; add to map
                reactorStats.put(Integer.valueOf(rid), stats);
            }
        }
        return stats;
    }
    // </editor-fold>
}
