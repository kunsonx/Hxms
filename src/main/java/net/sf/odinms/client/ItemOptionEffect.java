/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;

/**
 *
 * @author Administrator
 */
public class ItemOptionEffect {

    private long id;
    private int incSTR, incDEX, incINT, incLUK, incMHP, incMMP, incACC, incEVA, incSpeed, incJump, incPAD, incMAD, incPDD, incMDD, prop, time;
    private double incSTRr, incDEXr, incINTr, incLUKr, incMHPr, incMMPr, incACCr, incEVAr, incPADr, incMADr, incPDDr, incMDDr, incCr, incDAMr;
    private int RecoveryHP, RecoveryMP, HP, MP, attackType, level, ignoreTargetDEF, ignoreDAM, DAMreflect, mpconReduce, mpRestore, incMesoProp;
    private int incRewardProp, incAllskill, RecoveryUP;
    private String face;
    private boolean boss;

    public static ItemOptionEffect loadFromData(int id, MapleData source) {
        ItemOptionEffect ret = new ItemOptionEffect();
        ret.incSTR = MapleDataTool.getInt("incSTR", source, 0);//1
        ret.incDEX = MapleDataTool.getInt("incDEX", source, 0);//2
        ret.incINT = MapleDataTool.getInt("incINT", source, 0);//3
        ret.incLUK = MapleDataTool.getInt("incLUK", source, 0);//4
        ret.incMHP = MapleDataTool.getInt("incMHP", source, 0);//5
        ret.incMMP = MapleDataTool.getInt("incMMP", source, 0);//6
        ret.incACC = MapleDataTool.getInt("incACC", source, 0);//7
        ret.incEVA = MapleDataTool.getInt("incEVA", source, 0);//8
        ret.incSpeed = MapleDataTool.getInt("incSpeed", source, 0);//9
        ret.incJump = MapleDataTool.getInt("incJump", source, 0);//10
        ret.incPAD = MapleDataTool.getInt("incPAD", source, 0);//11
        ret.incMAD = MapleDataTool.getInt("incMAD", source, 0);//12
        ret.incPDD = MapleDataTool.getInt("incPDD", source, 0);//13
        ret.incMDD = MapleDataTool.getInt("incMDD", source, 0);//14

        ret.incSTRr = MapleDataTool.getInt("incSTRr", source, 0) / 100.0;//41
        ret.incDEXr = MapleDataTool.getInt("incDEXr", source, 0) / 100.0;//42
        ret.incINTr = MapleDataTool.getInt("incINTr", source, 0) / 100.0;//43
        ret.incLUKr = MapleDataTool.getInt("incLUKr", source, 0) / 100.0;//44
        ret.incMHPr = MapleDataTool.getInt("incMHPr", source, 0) / 100.0;//45
        ret.incMMPr = MapleDataTool.getInt("incMMPr", source, 0) / 100.0;//46
        ret.incACCr = MapleDataTool.getInt("incACCr", source, 0) / 100.0;//47
        ret.incEVAr = MapleDataTool.getInt("incEVAr", source, 0) / 100.0;//48
        ret.incPADr = MapleDataTool.getInt("incPADr", source, 0) / 100.0;//51
        ret.incMADr = MapleDataTool.getInt("incMADr", source, 0) / 100.0;//52
        ret.incPDDr = MapleDataTool.getInt("incPDDr", source, 0) / 100.0;//53
        ret.incMDDr = MapleDataTool.getInt("incMDDr", source, 0) / 100.0;//54
        ret.incCr = MapleDataTool.getInt("incCr", source, 0) / 100.0;//55
        ret.incDAMr = MapleDataTool.getInt("incDAMr", source, 0) / 100.0;//70 伤害

        ret.RecoveryHP = MapleDataTool.getInt("RecoveryHP", source, 0);//每4秒恢复HP
        ret.RecoveryMP = MapleDataTool.getInt("RecoveryMP", source, 0);//每4秒恢复MP
        ret.HP = MapleDataTool.getInt("HP", source, 0);//攻击时有几率恢复HP
        ret.MP = MapleDataTool.getInt("MP", source, 0);//206
        ret.attackType = MapleDataTool.getInt("attackType", source, 0);//1000 中毒 1001 眩晕 1002 减速
        ret.level = MapleDataTool.getInt("level", source, 0);//221
        ret.ignoreTargetDEF = MapleDataTool.getInt("ignoreTargetDEF", source, 0);//291
        ret.ignoreDAM = MapleDataTool.getInt("ignoreDAM", source, 0);//351
        ret.DAMreflect = MapleDataTool.getInt("DAMreflect", source, 0);//376
        ret.mpconReduce = MapleDataTool.getInt("mpconReduce", source, 0);//30501 潜在能力未知
        ret.mpRestore = MapleDataTool.getInt("mpRestore", source, 0);//511
        ret.incMesoProp = MapleDataTool.getInt("incMesoProp", source, 0);//650
        ret.incRewardProp = MapleDataTool.getInt("incRewardProp", source, 0);//656
        ret.incAllskill = MapleDataTool.getInt("incAllskill", source, 0);//106
        ret.RecoveryUP = MapleDataTool.getInt("RecoveryUP", source, 0);
        ret.boss = MapleDataTool.getInt("boss", source, 0) == 1;
        ret.face = MapleDataTool.getString("face", source, "");//受到攻击时，有X%的几率在X秒内感受到愤怒
        ret.prop = MapleDataTool.getInt("prop", source, 0);//901
        ret.time = MapleDataTool.getInt("time", source, 0);//901

        return ret;
    }

    public int getincMHP() {
        return this.incMHP;
    }

    public int getincMMP() {
        return this.incMMP;
    }

    public double getincMHPr() {
        return this.incMHPr;
    }

    public double getincMMPr() {
        return this.incMMPr;
    }

    public int getincAllskill() {
        return this.incAllskill;
    }

    public int getDAMreflect() {
        return DAMreflect;
    }

    public int getHP() {
        return HP;
    }

    public int getMP() {
        return MP;
    }

    public int getRecoveryHP() {
        return RecoveryHP;
    }

    public int getRecoveryMP() {
        return RecoveryMP;
    }

    public int getRecoveryUP() {
        return RecoveryUP;
    }

    public int getAttackType() {
        return attackType;
    }

    public boolean isBoss() {
        return boss;
    }

    public String getFace() {
        return face;
    }

    public int getIgnoreDAM() {
        return ignoreDAM;
    }

    public int getIgnoreTargetDEF() {
        return ignoreTargetDEF;
    }

    public int getIncACC() {
        return incACC;
    }

    public double getIncACCr() {
        return incACCr;
    }

    public int getIncAllskill() {
        return incAllskill;
    }

    public double getIncCr() {
        return incCr;
    }

    public double getIncDAMr() {
        return incDAMr;
    }

    public int getIncDEX() {
        return incDEX;
    }

    public double getIncDEXr() {
        return incDEXr;
    }

    public int getIncEVA() {
        return incEVA;
    }

    public double getIncEVAr() {
        return incEVAr;
    }

    public int getIncINT() {
        return incINT;
    }

    public double getIncINTr() {
        return incINTr;
    }

    public int getIncJump() {
        return incJump;
    }

    public int getIncLUK() {
        return incLUK;
    }

    public double getIncLUKr() {
        return incLUKr;
    }

    public int getIncMAD() {
        return incMAD;
    }

    public double getIncMADr() {
        return incMADr;
    }

    public int getIncMDD() {
        return incMDD;
    }

    public double getIncMDDr() {
        return incMDDr;
    }

    public int getIncMHP() {
        return incMHP;
    }

    public double getIncMHPr() {
        return incMHPr;
    }

    public int getIncMMP() {
        return incMMP;
    }

    public double getIncMMPr() {
        return incMMPr;
    }

    public int getIncMesoProp() {
        return incMesoProp;
    }

    public int getIncPAD() {
        return incPAD;
    }

    public double getIncPADr() {
        return incPADr;
    }

    public int getIncPDD() {
        return incPDD;
    }

    public double getIncPDDr() {
        return incPDDr;
    }

    public int getIncRewardProp() {
        return incRewardProp;
    }

    public int getIncSTR() {
        return incSTR;
    }

    public double getIncSTRr() {
        return incSTRr;
    }

    public int getIncSpeed() {
        return incSpeed;
    }

    public int getLevel() {
        return level;
    }

    public int getMpRestore() {
        return mpRestore;
    }

    public int getMpconReduce() {
        return mpconReduce;
    }

    public int getProp() {
        return prop;
    }

    public int getTime() {
        return time;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setIncSTR(int incSTR) {
        this.incSTR = incSTR;
    }

    public void setIncDEX(int incDEX) {
        this.incDEX = incDEX;
    }

    public void setIncINT(int incINT) {
        this.incINT = incINT;
    }

    public void setIncLUK(int incLUK) {
        this.incLUK = incLUK;
    }

    public void setIncMHP(int incMHP) {
        this.incMHP = incMHP;
    }

    public void setIncMMP(int incMMP) {
        this.incMMP = incMMP;
    }

    public void setIncACC(int incACC) {
        this.incACC = incACC;
    }

    public void setIncEVA(int incEVA) {
        this.incEVA = incEVA;
    }

    public void setIncSpeed(int incSpeed) {
        this.incSpeed = incSpeed;
    }

    public void setIncJump(int incJump) {
        this.incJump = incJump;
    }

    public void setIncPAD(int incPAD) {
        this.incPAD = incPAD;
    }

    public void setIncMAD(int incMAD) {
        this.incMAD = incMAD;
    }

    public void setIncPDD(int incPDD) {
        this.incPDD = incPDD;
    }

    public void setIncMDD(int incMDD) {
        this.incMDD = incMDD;
    }

    public void setProp(int prop) {
        this.prop = prop;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setIncSTRr(double incSTRr) {
        this.incSTRr = incSTRr;
    }

    public void setIncDEXr(double incDEXr) {
        this.incDEXr = incDEXr;
    }

    public void setIncINTr(double incINTr) {
        this.incINTr = incINTr;
    }

    public void setIncLUKr(double incLUKr) {
        this.incLUKr = incLUKr;
    }

    public void setIncMHPr(double incMHPr) {
        this.incMHPr = incMHPr;
    }

    public void setIncMMPr(double incMMPr) {
        this.incMMPr = incMMPr;
    }

    public void setIncACCr(double incACCr) {
        this.incACCr = incACCr;
    }

    public void setIncEVAr(double incEVAr) {
        this.incEVAr = incEVAr;
    }

    public void setIncPADr(double incPADr) {
        this.incPADr = incPADr;
    }

    public void setIncMADr(double incMADr) {
        this.incMADr = incMADr;
    }

    public void setIncPDDr(double incPDDr) {
        this.incPDDr = incPDDr;
    }

    public void setIncMDDr(double incMDDr) {
        this.incMDDr = incMDDr;
    }

    public void setIncCr(double incCr) {
        this.incCr = incCr;
    }

    public void setIncDAMr(double incDAMr) {
        this.incDAMr = incDAMr;
    }

    public void setRecoveryHP(int RecoveryHP) {
        this.RecoveryHP = RecoveryHP;
    }

    public void setRecoveryMP(int RecoveryMP) {
        this.RecoveryMP = RecoveryMP;
    }

    public void setHP(int HP) {
        this.HP = HP;
    }

    public void setMP(int MP) {
        this.MP = MP;
    }

    public void setAttackType(int attackType) {
        this.attackType = attackType;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setIgnoreTargetDEF(int ignoreTargetDEF) {
        this.ignoreTargetDEF = ignoreTargetDEF;
    }

    public void setIgnoreDAM(int ignoreDAM) {
        this.ignoreDAM = ignoreDAM;
    }

    public void setDAMreflect(int DAMreflect) {
        this.DAMreflect = DAMreflect;
    }

    public void setMpconReduce(int mpconReduce) {
        this.mpconReduce = mpconReduce;
    }

    public void setMpRestore(int mpRestore) {
        this.mpRestore = mpRestore;
    }

    public void setIncMesoProp(int incMesoProp) {
        this.incMesoProp = incMesoProp;
    }

    public void setIncRewardProp(int incRewardProp) {
        this.incRewardProp = incRewardProp;
    }

    public void setIncAllskill(int incAllskill) {
        this.incAllskill = incAllskill;
    }

    public void setRecoveryUP(int RecoveryUP) {
        this.RecoveryUP = RecoveryUP;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }
}
