package net.sf.odinms.server.movement;

import java.awt.Point;
import net.sf.odinms.tools.data.output.LittleEndianWriter;

public class AbsoluteLifeMovement extends AbstractLifeMovement {

	private Point pixelsPerSecond, offset;
	private int unk;
	private int unk2;
	private int unk3, unk4;

	public AbsoluteLifeMovement(int type, Point position, int duration,
			int newstate) {
		super(type, position, duration, newstate);
	}

	public Point getPixelsPerSecond() {
		return pixelsPerSecond;
	}

	public void setPixelsPerSecond(Point wobble) {
		this.pixelsPerSecond = wobble;
	}

	public Point getOffset() {
		return offset;
	}

	public void setOffset(Point wobble) {
		this.offset = wobble;
	}

	public int getUnk() {
		return unk;
	}

	public void setUnk(int unk) {
		this.unk = unk;
	}

	public int getUnk2() {
		return unk2;
	}

	public void setUnk2(int unk2) {
		this.unk2 = unk2;
	}

	public int getUnk3() {
		return unk3;
	}

	public void setUnk3(int unk3) {
		this.unk3 = unk3;
	}

	public int getUnk4() {
		return unk4;
	}

	public void setUnk4(int unk4) {
		this.unk4 = unk4;
	}

	@Override
	public void serialize(LittleEndianWriter lew) {
		lew.write(getType());
		lew.writePos(getPosition());
		lew.writePos(pixelsPerSecond);
		lew.writeShort(unk);
		lew.writePos(offset);
		/*
		 * if (getType() == 0x0E) { lew.writeShort(unk2); }
		 */
		lew.write(getNewstate());
		lew.writeShort(getDuration());
	}
}
