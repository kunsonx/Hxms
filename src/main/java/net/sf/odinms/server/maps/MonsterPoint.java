/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

/**
 *
 * @author Admin
 */
public class MonsterPoint {

	private long id;
	private int x;
	private int y;
	private int cy;
	private int fh;
	private int team;

	public MonsterPoint(int x, int y, int fh, int cy, int team) {
		this.x = x;
		this.y = y;
		this.fh = fh;
		this.cy = cy;
		this.team = team;
	}

	public int getTeam() {
		return team;
	}

	public void setTeam(int team) {
		this.team = team;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getFh() {
		return fh;
	}

	public void setFh(int fh) {
		this.fh = fh;
	}

	public int getCy() {
		return cy;
	}

	public void setCy(int cy) {
		this.cy = cy;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
