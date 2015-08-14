package net.sf.odinms.server;

/**
 *
 * @author 岚殇
 */
public class CashItemInfo {

    private int SN, itemId, count, price, period, gender;
    private int pbCash, pbPoint, pbGift, class2, maplePoint, meso; //盛大新增
    private boolean onSale;

    private CashItemInfo() {
    }

    public CashItemInfo(int SN, int itemId, int count, int price, int period, int gender, boolean onSale) {
        this.SN = SN;
        this.itemId = itemId;
        this.count = count;
        this.price = price;
        this.period = period;
        this.gender = gender;
        this.onSale = onSale;
    }

    public int getSN() {
        return SN;
    }

    public int getItemId() {
        return itemId;
    }

    public int getCount() {
        return count;
    }

    public int getPrice() {
        return price;
    }

    public int getPeriod() {
        return period;
    }

    public int getGender() {
        return gender;
    }

    public boolean onSale() {
        return onSale;
    }

    public int getpbCash() {
        return pbCash;
    }

    public int getpbPoint() {
        return pbPoint;
    }

    public int getpbGift() {
        return pbGift;
    }

    public int getClass2() {
        return class2;
    }

    public int getMaplePoint() {
        return maplePoint;
    }

    public int getMeso() {
        return meso;
    }

    public boolean isOnSale() {
        return onSale;
    }

    private void setSN(int SN) {
        this.SN = SN;
    }

    private void setClass2(int class2) {
        this.class2 = class2;
    }

    private void setCount(int count) {
        this.count = count;
    }

    private void setGender(int gender) {
        this.gender = gender;
    }

    private void setItemId(int itemId) {
        this.itemId = itemId;
    }

    private void setMaplePoint(int maplePoint) {
        this.maplePoint = maplePoint;
    }

    private void setMeso(int meso) {
        this.meso = meso;
    }

    private void setOnSale(boolean onSale) {
        this.onSale = onSale;
    }

    private void setPbCash(int pbCash) {
        this.pbCash = pbCash;
    }

    private void setPbGift(int pbGift) {
        this.pbGift = pbGift;
    }

    private void setPbPoint(int pbPoint) {
        this.pbPoint = pbPoint;
    }

    private void setPeriod(int period) {
        this.period = period;
    }

    private void setPrice(int price) {
        this.price = price;
    }
}