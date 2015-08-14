package net.sf.odinms.client;

import java.sql.Timestamp;
import java.util.List;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.constants.InventoryConstants;
import net.sf.odinms.server.life.MapleAndroid;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;

public class Equip extends Item implements IEquip {

    private byte upgradeSlots;
    private byte level;
    private short str, dex, _int, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, vicious;
    private boolean ring;
    private int partnerUniqueId;
    private int partnerId;
    private String partnerName;
    //道具经验值 等级 技能 耐久度 大乱斗攻击力
    private int itemExp, itemLevel, itemSkill, durability = -1, pvpWatk;
    //潜能
    private int Potential_1, Potential_2, Potential_3;
    private byte Identify, Starlevel, Identified;
    private Timestamp unlockTimestamp = null;
    private MapleAndroid android;

    public Equip(int id, short position) {
        super(id, position, (short) 1);
        this.ring = false;
    }

    public Equip(int id, short position, boolean ring) {
        super(id, position, (short) 1);
        this.ring = false;
    }

    public Equip(int itemid, short position, boolean ring, int partnerUniqueId, int partnerId, String partnerName) {
        super(itemid, position, (short) 1);
        this.ring = ring;
        this.partnerUniqueId = partnerUniqueId;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
    }

    @Override
    public IItem copy() {
        Equip ret = new Equip(getItemId(), getPosition(), ring);
        ret.str = str;
        ret.dex = dex;
        ret._int = _int;
        ret.luk = luk;
        ret.hp = hp;
        ret.mp = mp;
        ret.matk = matk;
        ret.mdef = mdef;
        ret.watk = watk;
        ret.wdef = wdef;
        ret.acc = acc;
        ret.avoid = avoid;
        ret.hands = hands;
        ret.speed = speed;
        ret.jump = jump;
        ret.flag = flag;
        ret.upgradeSlots = upgradeSlots;
        ret.level = level;
        ret.vicious = vicious;
        ret.setOwner(getOwner());
        ret.setQuantity(getQuantity());
        ret.Identified = Identified;
        ret.Identify = Identify;
        ret.Potential_1 = Potential_1;
        ret.Potential_2 = Potential_2;
        ret.Potential_3 = Potential_3;
        ret.Starlevel = Starlevel;
        ret.itemExp = itemExp;
        ret.itemLevel = itemLevel;
        ret.itemSkill = itemSkill;
        ret.pvpWatk = pvpWatk;
        ret.durability = durability;
        return ret;
    }

    @Override
    public byte getType() {
        return IItem.EQUIP;
    }

    @Override
    public byte getUpgradeSlots() {
        return upgradeSlots;
    }

    @Override
    public short getStr() {
        return str;
    }

    @Override
    public short getDex() {
        return dex;
    }

    @Override
    public short getInt() {
        return _int;
    }

    @Override
    public short getLuk() {
        return luk;
    }

    @Override
    public short getHp() {
        return hp;
    }

    @Override
    public short getMp() {
        return mp;
    }

    @Override
    public short getWatk() {
        return watk;
    }

    @Override
    public short getMatk() {
        return matk;
    }

    @Override
    public short getWdef() {
        return wdef;
    }

    @Override
    public short getMdef() {
        return mdef;
    }

    @Override
    public short getAcc() {
        return acc;
    }

    @Override
    public short getAvoid() {
        return avoid;
    }

    @Override
    public short getHands() {
        return hands;
    }

    @Override
    public short getSpeed() {
        return speed;
    }

    @Override
    public short getJump() {
        return jump;
    }

    public void setStr(short str) {
        this.str = str;
    }

    public void setDex(short dex) {
        this.dex = dex;
    }

    public void setInt(short _int) {
        this._int = _int;
    }

    public void setLuk(short luk) {
        this.luk = luk;
    }

    public void setHp(short hp) {
        this.hp = hp;
    }

    public void setMp(short mp) {
        this.mp = mp;
    }

    public void setWatk(short watk) {
        this.watk = watk;
    }

    public void setMatk(short matk) {
        this.matk = matk;
    }

    public void setWdef(short wdef) {
        this.wdef = wdef;
    }

    public void setMdef(short mdef) {
        this.mdef = mdef;
    }

    public void setAcc(short acc) {
        this.acc = acc;
    }

    public void setAvoid(short avoid) {
        this.avoid = avoid;
    }

    public void setHands(short hands) {
        this.hands = hands;
    }

    public void setSpeed(short speed) {
        this.speed = speed;
    }

    public void setJump(short jump) {
        this.jump = jump;
    }

    public boolean getLocked() {
        return HasFlag(InventoryConstants.Items.Flags.锁定);
    }

    public void setLocked(byte locked) {
        if (locked == 1) {
            AddFlag(InventoryConstants.Items.Flags.锁定);
        } else {
            CanceFlag(InventoryConstants.Items.Flags.锁定);
        }

    }

    @Override
    public void setUpgradeSlots(int i) {
        this.upgradeSlots = (byte) i;
    }

    @Override
    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    @Override
    public short getVicious() {
        return vicious;
    }

    @Override
    public void setVicious(int i) {
        this.vicious = (short) i;
    }

    public void setVicious(short vicious) {
        this.vicious = vicious;
    }

    @Override
    public int getSN() {
        return sn;
    }

    @Override
    public void setSN(int sn) {
        this.sn = sn;
    }

    @Override
    public long getUniqueid() {
        return uniqueid;
    }

    @Override
    public void setUniqueId(long id) {
        this.uniqueid = id;
    }

    @Override
    public void setQuantity(short quantity) {
        if (quantity < 0 || quantity > 1) {
            throw new RuntimeException("Setting the quantity to " + quantity + " on an equip (itemid: " + getItemId() + ")");
        }
        super.setQuantity(quantity);
    }

    @Override
    public Timestamp getExpiration() {
        return expiration;
    }

    @Override
    public void setExpiration(Timestamp expire) {
        this.expiration = expire;
    }

    @Override
    public long getPartnerUniqueId() {
        return partnerUniqueId;
    }

    @Override
    public int getPartnerId() {
        return partnerId;
    }

    @Override
    public String getPartnerName() {
        return partnerName;
    }

    //获得装备的潜能属性
    @Override
    public int getPotential_1() {
        return Potential_1;
    }

    @Override
    public int getPotential_2() {
        return Potential_2;
    }

    @Override
    public int getPotential_3() {
        return Potential_3;
    }

    @Override
    public byte getIdentify() {
        return Identify;
    }

    @Override
    public byte getStarlevel() {
        return Starlevel;
    }

    @Override
    public byte getIdentified() {
        return Identified;
    }

    //设置装备的潜能属性
    @Override
    public void setPotential_1(int i) {
        Potential_1 = i;
    }

    @Override
    public void setPotential_2(int i) {
        Potential_2 = i;
    }

    @Override
    public void setPotential_3(int i) {
        Potential_3 = i;
    }

    @Override
    public void setIdentify(byte i) {
        Identify = i;
    }

    @Override
    public void setStarlevel(byte i) {
        Starlevel = i;
    }

    @Override
    public void setIdentified(byte i) {
        Identified = i;
    }

    public int getPotential(int type) {
        if (type == 1) {
            return Potential_1;
        } else if (type == 2) {
            return Potential_2;
        } else if (type == 3) {
            return Potential_3;
        } else {
            return 0;
        }
    }

    public void setPotential(int type, int set) {
        if (type == 1) {
            this.Potential_1 = (short) set;
        } else if (type == 2) {
            this.Potential_2 = (short) set;
        } else if (type == 3) {
            this.Potential_3 = (short) set;
        }
    }

    public void setItemExp(int itemExp) {
        this.itemExp = itemExp;
    }

    @Override
    public int getItemExp() {
        return itemExp;
    }

    public void gainItemExp3(MapleClient c, int gain, boolean timeless) {
        int expneeded = timeless ? (10 * itemLevel + 70) : (5 * itemLevel + 65);
        float modifier = 364 / expneeded;
        float exp = (expneeded / (1000000 * modifier * modifier)) * gain;
        itemExp += exp;

        if (itemExp >= 364) {
            itemExp = (itemExp - 364);
            gainItemLevel(c, timeless);
        } else {
            c.getPlayer().forceUpdateItem(MapleInventoryType.EQUIPPED, this);
        }
    }

    public void gainItemExp2(MapleClient c, int gain, boolean timeless) {
        itemExp += gain;
        int expNeeded = 0;
        if (timeless) {
            expNeeded = ExpTable.getTimelessItemExpNeededForLevel(itemLevel + 1);
        } else {
            expNeeded = ExpTable.getReverseItemExpNeededForLevel(itemLevel + 1);
        }
        if (itemExp >= expNeeded) {
            gainItemLevel(c, timeless);
            c.getSession().write(MaplePacketCreator.showItemLevelup());
        }
    }

    public void gainItemLevel(MapleClient c, boolean timeless) {
        List<Pair<String, Integer>> stats = MapleItemInformationProvider.getInstance().getItemLevelupStats(getItemId(), itemLevel, timeless);
        for (Pair<String, Integer> stat : stats) {
            if (stat.getLeft().equals("incDEX")) {
                dex += stat.getRight();
            } else if (stat.getLeft().equals("incSTR")) {
                str += stat.getRight();
            } else if (stat.getLeft().equals("incINT")) {
                _int += stat.getRight();
            } else if (stat.getLeft().equals("incLUK")) {
                luk += stat.getRight();
            } else if (stat.getLeft().equals("incMHP")) {
                hp += stat.getRight();
            } else if (stat.getLeft().equals("incMMP")) {
                mp += stat.getRight();
            } else if (stat.getLeft().equals("incPAD")) {
                watk += stat.getRight();
            } else if (stat.getLeft().equals("incMAD")) {
                matk += stat.getRight();
            } else if (stat.getLeft().equals("incPDD")) {
                wdef += stat.getRight();
            } else if (stat.getLeft().equals("incMDD")) {
                mdef += stat.getRight();
            } else if (stat.getLeft().equals("incEVA")) {
                avoid += stat.getRight();
            } else if (stat.getLeft().equals("incACC")) {
                acc += stat.getRight();
            } else if (stat.getLeft().equals("incSpeed")) {
                speed += stat.getRight();
            } else if (stat.getLeft().equals("incJump")) {
                jump += stat.getRight();
            }
        }
        this.itemLevel++;
        c.getPlayer().getClient().getSession().write(MaplePacketCreator.showEquipmentLevelUp());
        //c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showForeignEffect(c.getPlayer().getId(), 17));
        //c.getPlayer().forceUpdateItem(MapleInventoryType.EQUIPPED, this);
    }

    @Override
    public int getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(int i) {
        itemLevel = i;
    }

    @Override
    public int getDurability() {
        return durability;
    }

    public void setDurability(int i) {
        durability = i;
    }

    @Override
    public int getItemSkill() {
        return itemSkill;
    }

    public void setItemSkill(int i) {
        itemSkill = i;
    }

    public void gainItemExp(MapleClient c, int gain, boolean timeless) {
        /*
         int expneeded = timeless ? (10 * itemLevel + 70) : (5 * itemLevel + 65);
         float modifier = 364 / expneeded;
         float exp = (expneeded / (1000000 * modifier * modifier)) * gain;
         */

        itemExp += gain;
        if (itemExp >= 1270000) {
            logger.debug("升级");
            itemExp = (itemExp - 1270000);
            gainItemLevel(c, timeless);
        } else {
            logger.debug("不升级");
        }
        //c.getSession().write(MaplePacketCreator.updateEquipSlot(this));//更新装备栏
        //c.getPlayer().equipChanged();
        //c.getPlayer().forceUpdateItem(MapleInventoryType.EQUIPPED, this);
    }

    /**
     * @return the pvpWatk
     */
    @Override
    public int getPvpWatk() {
        return pvpWatk;
    }

    /**
     * @param pvpWatk the pvpWatk to set
     */
    public void setPvpWatk(int pvpWatk) {
        this.pvpWatk = pvpWatk;
    }

    @Override
    public boolean isRing() {
        return false;
    }

    @Override
    public Timestamp getUnlockTime() {
        return unlockTimestamp;
    }

    @Override
    public void setUnlockTime(Timestamp time) {
        this.unlockTimestamp = time;
    }

    public MapleAndroid getAndroid() {
        if (getItemId() / 10000 != 166 || getUniqueid() <= 0) {
            return null;
        }
        if (android == null) {
            android = MapleAndroid.loadFromDb(getItemId(), getUniqueid());
        }
        return android;
    }

    public void setAndroid(MapleAndroid ring) {
        this.android = ring;
    }

    public void writePacket(MaplePacketLittleEndianWriter mplew) {
        int mask = 0;
        int[] values = {
            getUpgradeSlots(),//可升级次数

            getLevel(),//从下开始就是双字节 2  //已升级次数
            getStr(),// 力量
            getDex(),// 敏捷
            getInt(), // 智力
            getLuk(),// 运气

            getHp(),// hp
            getMp(),// mp
            getWatk(), // 物理攻击
            getMatk(), // 魔法攻击
            getWdef(),// 物理防御

            getMdef(),// 魔法防御
            getAcc(),// 命中率
            getAvoid(), // 回避率
            getHands(), // 手技
            getSpeed(),// 移动速度

            getJump(),//从下开始就是int类型   17 // 跳跃力
            getFlag(),//4
            getItemSkill(),//1 是否获得道具附加技能 0 不获得 1 获得
            getItemLevel(),//1 //道具等级
            getItemExp(),//4//道具经验 客户端自动折算成百分比

            getDurability(),//4//耐久度 最大值30000 客户端自动折算成百分比 没耐久度的装备发-1
            getVicious(),//4//金锤子
            getPvpWatk()//2//093新增 大乱斗攻击力
        };
        for (int i = 0; i < values.length; i++) {
            if (values[i] > 0) {
                mask += 1 << i;
            }
        }
        mplew.writeInt(mask);
        for (int i = 0; i < values.length; i++) {
            if (values[i] > 0) {
                switch (i) {
                    case 0://√
                    case 1://√
                    case 18://√
                    case 19://√
                        mplew.write(values[i]);
                        break;
                    case 2://√
                    case 3://√
                    case 4://√
                    case 5://√
                    case 6://√
                    case 7://√
                    case 8://√
                    case 9://√
                    case 10://√
                    case 11://√
                    case 12://√
                    case 13://√
                    case 14://√
                    case 15://√
                    case 16://√
                    case 23:
                        mplew.writeShort(values[i]);
                        break;
                    case 17://√
                    case 20:
                    case 21:
                    case 22:
                        mplew.writeInt(values[i]);
                        break;
                }
            }
        }
    }
}