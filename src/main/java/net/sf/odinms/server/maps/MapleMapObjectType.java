package net.sf.odinms.server.maps;

public enum MapleMapObjectType {

    NPC,
    MONSTER,
    ITEM,
    PLAYER,
    DOOR,
    SUMMON,
    SHOP,
    MIST,
    REACTOR,
    HIRED_MERCHANT,
    LOVE,
    Android,
    机械传送门;

    public boolean isPlayer() {
        return this == PLAYER;
    }
}