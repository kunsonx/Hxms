package net.sf.odinms.server.maps;

import net.sf.odinms.net.IntValueHolder;

public enum SummonMovementType implements IntValueHolder {

    STATIONARY(0), FOLLOW(1), UNKNOWN(2), CIRCLE_FOLLOW(4);
    private final int val;

    private SummonMovementType(int val) {
        this.val = val;
    }

    public boolean ISFOLLOW() {
        return val == 4 || val == 1;
    }

    @Override
    public int getValue() {
        return val;
    }
}
