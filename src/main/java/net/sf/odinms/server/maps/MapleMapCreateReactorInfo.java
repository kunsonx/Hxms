/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.awt.Point;

/**
 *
 * @author Admin
 */
public class MapleMapCreateReactorInfo {

	private long d_id;
	private int id;
	private Point point;
	private byte FacingDirection;
	private int delay;
	private String name;

	public MapleMapCreateReactorInfo() {
	}

	public MapleMapCreateReactorInfo(int id, Point point, byte FacingDirection,
			int delay, String name) {
		this.id = id;
		this.point = point;
		this.FacingDirection = FacingDirection;
		this.delay = delay;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

	public byte getFacingDirection() {
		return FacingDirection;
	}

	public void setFacingDirection(byte FacingDirection) {
		this.FacingDirection = FacingDirection;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getD_id() {
		return d_id;
	}

	public void setD_id(long d_id) {
		this.d_id = d_id;
	}

	// myReactor.setState((byte) 0);

	public MapleReactor getReactor() {
		MapleReactor myReactor = new MapleReactor(
				MapleReactorFactory.getReactor(id), id);
		myReactor.setPosition(point);
		myReactor.setFacingDirection(FacingDirection);
		myReactor.setDelay(delay);
		myReactor.setState((byte) 0);
		myReactor.setName(name);
		return myReactor;
	}
}
