package net.sf.odinms.client;

/**
 *
 * @author Matze
 */
public enum MapleInventoryType {

    UNDEFINED(-2),
    EQUIPPED(-1),
    EQUIP(1),
    USE(2),
    SETUP(3),
    ETC(4),
    CASH(5),;
    final byte type;

    private MapleInventoryType(int type) {
        this.type = (byte) type;
    }

    public byte getType() {
        return type;
    }

    public short getBitfieldEncoding() {
        return (short) (2 << type);
    }

    public static MapleInventoryType getByType(byte type) {
        for (MapleInventoryType l : MapleInventoryType.values()) {
            if (l.getType() == type) {
                return l;
            }
        }
        return null;
    }

    public String getName() {
        switch (this) {
            case CASH:
                return "商城栏";
            case EQUIP:
                return "装备栏";
            case EQUIPPED:
                return "已装备栏";
            case ETC:
                return "其他栏";
            case SETUP:
                return "设置栏";
            case USE:
                return "消耗栏";
            default:
                return "未知栏";
        }
    }

    public static MapleInventoryType getByWZName(String name) {
        if (name.equals("Install")) {
            return SETUP;
        } else if (name.equals("Consume")) {
            return USE;
        } else if (name.equals("Etc")) {
            return ETC;
        } else if (name.equals("Cash")) {
            return CASH;
        } else if (name.equals("Pet")) {
            return CASH;
        }
        return UNDEFINED;
    }
}
