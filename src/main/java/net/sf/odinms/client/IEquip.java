package net.sf.odinms.client;

import java.sql.Timestamp;

public interface IEquip extends IItem {

    public enum ScrollResult {

        SUCCESS, FAIL, CURSE
    }

    //潜能
    public int getPotential_1();

    public int getPotential_2();

    public int getPotential_3();

    public byte getIdentify();

    public byte getStarlevel();

    public byte getIdentified();

    public void setPotential_1(int i);

    public void setPotential_2(int i);

    public void setPotential_3(int i);

    public void setIdentify(byte i);

    public void setStarlevel(byte i);

    public void setIdentified(byte i);

    public void setUpgradeSlots(int i);

    public void setVicious(int i);

    public byte getUpgradeSlots();

    public byte getLevel();

    public short getStr();

    public short getDex();

    public short getInt();

    public short getLuk();

    public short getHp();

    public short getMp();

    public short getWatk();

    public short getMatk();

    public short getWdef();

    public short getMdef();

    public short getAcc();

    public short getAvoid();

    public short getHands();

    public short getSpeed();

    public short getJump();

    public long getPartnerUniqueId();

    public int getPartnerId();

    public String getPartnerName();

    public short getVicious();

    public int getItemExp();

    public int getItemLevel();

    public int getItemSkill();

    public int getDurability();

    public int getPvpWatk();

    public boolean isRing();

    public Timestamp getUnlockTime();

    public void setUnlockTime(Timestamp time);
}