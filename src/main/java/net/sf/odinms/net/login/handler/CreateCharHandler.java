/*
 创建角色程序
 */
package net.sf.odinms.net.login.handler;

import net.sf.odinms.client.Equip;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleCharAttribute;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacterUtil;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleSkinColor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.constants.InventoryConstants;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CreateCharHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CreateCharHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //11 00 [08 00 B9 FE B9 FE D9 E2 D2 B6] [03 00 00 00] [00 00] [84 4E 00 00] [30 75 00 00] [F7 E6 0F 00] [E3 34 10 00] [FF 5C 10 00] [1F 01 16 00]  //088的创建职业封包,貌似是send的
        //11 00 [08 00 CA B2 C3 B4 CA B2 B4 F3] [00 00 00 00] [00 00] [B1 4F 00 00] [30 75 00 00] [3E 06 10 00] [00 00 00 00] [A6 5B 10 00] [15 2C 14 00]//089
        //25 00 [08 00 73 64 68 68 79 79 79 79] [01 00 00 00] [00 00] [00 06] [B1 4F 00 00] [30 75 00 00] [82 DE 0F 00] [A2 2C 10 00] [85 5B 10 00] [04 05 14 00]//97
        /*
         * 25 00 0A 00 CE D2 CA C7 BB F0 C5 DA CA D6 01 00 00 00 02 00 00 00 06
         * 谁告诉我这个是什么意思。 84 4E 00 00 4E 75 00 00 86 DE 0F 00 A2 2C 10 00 A5 5B 10
         * 00 15 2C 14 00
         *
         * 25 00 0A 00 CF D6 D4 DA B5 C4 B9 CA CA C2 05 00 00 00 00 00 00 0C 05
         * 4D 50 00 00 AD 82 00 00 50 06 10 00 87 5D 10 00 76 39 17 00
         *
         * 25 00 0A 00 CF D6 D4 DA B5 C4 B9 CA CA C2 05 00 00 00 00 00 01 0C 05
         * 33 54 00 00 77 86 00 00 65 0A 10 00 87 5D 10 00 76 39 17 00
         *
         * 08 00 B6 F1 C4 A7 C1 D4 CA D6 06 00 00 00 00 00 00 0D 07 18 4F 00 00
         * FB 82 00 00
         *
         * 34 72 0F 00
         *
         * 4F 06 10 00 86 5D 10 00 8B 2C 14 00 F9 C4 10 00
         */
        String name = slea.readMapleAsciiString();//角色名
        int gender = c.getGender(); //性别
        slea.skip(4);
        int job = slea.readInt(); //职业
        slea.skip(2);
        gender = slea.readByte();
        int skinColor = slea.readByte();        //肤色
        slea.skip(1);
        int face = slea.readInt(); //脸型
        int hair = slea.readInt(); //发型
        int face_a = 0;
        if (job == 6) {
            face_a = slea.readInt();
        }
        int hairColor = 0;        //发色
        int top = slea.readInt(); //上衣
        int bottom = 0;
        if (job != 0 && job != 5 && job != 6 && job != 11 && job != 12) {//
            bottom = slea.readInt(); //裙裤
        }
        int shoes = slea.readInt(); //鞋子
        int weapon = slea.readInt(); //武器
        int dun = (job == 6) ? slea.readInt() : 0;

        MapleCharacter newchar = MapleCharacter.getDefault(c);
        if (c.isGM()) {
            newchar.setGm(1);
        }
        newchar.setWorld(c.getWorld());
        newchar.setFace(face);//生成脸型
        newchar.setHair(hair + hairColor);//生成头发和发色
        newchar.setGender(gender);
        MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        if (job == 0) { //反抗者 089建立没问题的，什么装备都没啊
            newchar.setStr(13);
            newchar.setDex(4);
            newchar.setInt(4);
            newchar.setLuk(4);
            newchar.setJob(3000);
            newchar.setRemainingAp(0);
            //送新手指导书
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161054, (byte) 0, (short) 1));
        } else if (job == 1) { //冒险家  089建立没问题的，什么装备都没啊
            newchar.setStr(13);
            newchar.setDex(4);
            newchar.setInt(4);
            newchar.setLuk(4);
            newchar.setJob(0);
            newchar.setRemainingAp(0);
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1));
        } else if (job == 2) {//骑士团  089建立没问题的，什么装备都没啊
            newchar.setStr(11);
            newchar.setDex(6);
            newchar.setInt(4);
            newchar.setLuk(4);
            newchar.setJob(1000);
            newchar.setRemainingAp(0);
            //送新手指导书
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161047, (byte) 0, (short) 1));
        } else if (job == 3) {//战神
            newchar.setStr(11);
            newchar.setDex(6);
            newchar.setInt(4);
            newchar.setLuk(4);
            newchar.setJob(2000);
            newchar.setRemainingAp(0);

            //送新手指导书
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161048, (byte) 0, (short) 1));
        } else if (job == 4) { // 龙神  089建立没问题的，什么装备都没啊
            newchar.setStr(6);
            newchar.setDex(4);
            newchar.setInt(11);
            newchar.setLuk(4);
            newchar.setJob(2001);
            newchar.setRemainingAp(0);
            //送
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4160005, (byte) 0, (short) 1));
        } else if (job == 5) {// 双弩精灵 099建立没问题的。什么装备都没啊 1352003//
            IItem song = null;
            if (gender == 0) {//男同胞.
                song = new Equip(1000046, (short) 0);//精灵王假发-男款
                song.setUniqueId(MapleCharacter.getNextUniqueId());
                newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

                song = new Equip(1050221, (short) 0);//精灵王服饰-男款
                song.setUniqueId(MapleCharacter.getNextUniqueId());
                newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);
            } else {
                song = new Equip(1051271, (short) 0);//精灵王服饰-女款
                song.setUniqueId(MapleCharacter.getNextUniqueId());
                newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

                song = new Equip(1001069, (short) 0);//精灵王假发-女款
                song.setUniqueId(MapleCharacter.getNextUniqueId());
                newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);
            }
            song = new Equip(1082408, (short) 0);//精灵王的手套
            song.setUniqueId(MapleCharacter.getNextUniqueId());
            newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

            song = new Equip(1072628, (short) 0);//精灵王的靴子
            song.setUniqueId(MapleCharacter.getNextUniqueId());
            newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

            song = new Equip(1102344, (short) 0);//精灵王的披风
            song.setUniqueId(MapleCharacter.getNextUniqueId());
            newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

            newchar.setStr(11);
            newchar.setDex(15);
            newchar.setInt(4);
            newchar.setLuk(4);
            newchar.setJob(2002);
            newchar.setRemainingAp(0);
            //送
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161079, (byte) 0, (short) 1));
        } else if (job == 6) {// 恶魔猎手 099建立没问题的。什么装备都没啊
            IItem song = null;
            if (gender == 0) {//男同胞.
                song = new Equip(1000045, (short) 0);//军团长假发-男款
                song.setUniqueId(MapleCharacter.getNextUniqueId());
                newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

                song = new Equip(1050220, (short) 0);//军团长外套-男款
                song.setUniqueId(MapleCharacter.getNextUniqueId());
                newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);
            } else {
                song = new Equip(1001068, (short) 0);//军团长假发-女款
                song.setUniqueId(MapleCharacter.getNextUniqueId());
                newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

                song = new Equip(1051270, (short) 0);//军团长外套-女款
                song.setUniqueId(MapleCharacter.getNextUniqueId());
                newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);
            }
            song = new Equip(1102343, (short) 0);//军团长的披风
            song.setUniqueId(MapleCharacter.getNextUniqueId());
            newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

            song = new Equip(1082407, (short) 0);//军团长的手套
            song.setUniqueId(MapleCharacter.getNextUniqueId());
            newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);

            song = new Equip(1072627, (short) 0);//军团长的靴子
            song.setUniqueId(MapleCharacter.getNextUniqueId());
            newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);


            newchar.setStr(15);
            newchar.setDex(11);
            newchar.setInt(4);
            newchar.setLuk(4);
            newchar.setJob(3001);
            newchar.setMaxmp(10);
            newchar.setMp(10);
            newchar.setFace_Adorn(face_a);
            newchar.setRemainingAp(0);
        } else if (job == 7) {//夜光
            newchar.setStr(12);
            newchar.setDex(12);
            newchar.setInt(12);
            newchar.setLuk(12);
            newchar.setJob(2003);
        } else if (job == 8) {
            newchar.setStr(10);
            newchar.setDex(10);
            newchar.setInt(10);
            newchar.setLuk(10);
            newchar.setJob(0);
        } else if (job == 9) {
            newchar.setStr(10);
            newchar.setDex(10);
            newchar.setInt(10);
            newchar.setLuk(10);
            newchar.setJob(5000);
        } else if (job == 10) {
            newchar.setStr(10);
            newchar.setDex(10);
            newchar.setInt(10);
            newchar.setLuk(10);
            newchar.setJob(2004);
        } else if (job == 11) {//狂龙
            newchar.setStr(10);
            newchar.setDex(10);
            newchar.setInt(10);
            newchar.setLuk(10);
            newchar.setJob(6000);

            Equip d = (Equip) MapleItemInformationProvider.getInstance().getEquipById(1352504);
            d.setPosition((short) -10);
            equip.addFromDB(d.copy());
        } else if (job == 12) {
            newchar.setStr(10);
            newchar.setDex(10);
            newchar.setInt(10);
            newchar.setLuk(10);
            newchar.setJob(6001);

            Equip d = (Equip) MapleItemInformationProvider.getInstance().getEquipById(1352600);
            d.setPosition((short) -10);
            equip.addFromDB(d.copy());
        } else if (job == 14) {
            newchar.setStr(10);
            newchar.setDex(10);
            newchar.setInt(10);
            newchar.setLuk(10);
            newchar.setJob(3002);
            top = 0;//干净的脸
            Equip song = new Equip(1242001, (short) 0);//精灵王的靴子
            song.AddFlag(InventoryConstants.Items.Flags.签名是制作人);
            song.setOwner("请10级后更换");
            song.setUniqueId(MapleCharacter.getNextUniqueId());
            newchar.getInventory(MapleInventoryType.EQUIP).addItem(song);
        }
        newchar.setName(name);//生成角色名
        newchar.setSkinColor(MapleSkinColor.getById(skinColor));//生成皮肤颜色

        if (top != 0) {
            Equip eq_top = new Equip(top, (byte) -5);//生成上衣
            eq_top.setWdef((short) 3);
            eq_top.setUpgradeSlots((byte) 7);
            equip.addFromDB(eq_top.copy());
        }
        if (bottom != 0) {
            Equip eq_bottom = new Equip(bottom, job != 14 ? (byte) -6 : (byte) -5);//生成裙裤 部分职业生成连体衣
            eq_bottom.setWdef((short) 2);
            eq_bottom.setUpgradeSlots((byte) 7);
            equip.addFromDB(eq_bottom.copy());
        }
        if (dun != 0) {
            Equip d = (Equip) MapleItemInformationProvider.getInstance().getEquipById(1099004);
            d.setPosition((short) -10);
            d.setMp((short) 110);
            equip.addFromDB(d);
        }
        Equip eq_shoes = new Equip(shoes, (byte) -7);//生成鞋子
        eq_shoes.setWdef((short) 2); //rite? o_O
        eq_shoes.setUpgradeSlots((byte) 7);
        equip.addFromDB(eq_shoes.copy());
        Equip eq_weapon = new Equip(weapon, (byte) -11);//生成武器
        eq_weapon.setWatk((short) 15);
        eq_weapon.setUpgradeSlots((byte) 7);
        equip.addFromDB(eq_weapon.copy());
        if (MapleCharacterUtil.canCreateChar(name, c.getWorld())) { //检测角色名
            newchar.saveToDB(false);
            newchar.setAttribute(MapleCharAttribute.createCharAttribute(newchar.getId()));
            c.getSession().write(MaplePacketCreator.addNewCharEntry(newchar, true));
            //c.getSession().write(MaplePacketCreator.serverNotice(1, "成功创建一个角色!"));
        } else {
            log.warn(MapleClient.getLogMessage(c, "Trying to create a character with a name: {}", name));
        }
    }
}
