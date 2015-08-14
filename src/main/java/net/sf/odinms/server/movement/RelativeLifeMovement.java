package net.sf.odinms.server.movement;

import java.awt.Point;

import net.sf.odinms.tools.data.output.LittleEndianWriter;

public class RelativeLifeMovement extends AbstractLifeMovement {

    private int unk = 0;

    public RelativeLifeMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    public int getUnk() {
        return unk;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }

    @Override
    public void serialize(LittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
 /*       if (getType() != 1 && getType() != 2) {
            lew.writeShort(unk);
        }*/
        lew.write(getNewstate());
        lew.writeShort(getDuration());
    }
}
