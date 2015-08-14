/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.util.List;

/**
 *
 * @author Admin
 */
public class MaplePlatform {

	private long id;
	private String name;
	private int y2;
	private int start;
	private int speed;
	private int x1;
	private int y1;
	private int x2;
	private int r;
	private List<Integer> SN;

	private MaplePlatform() {
	}

	public MaplePlatform(String name, int start, int speed, int x1, int y1,
			int x2, int y2, int r, List<Integer> SN) {
		this.name = name;
		this.start = start;
		this.speed = speed;
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.r = r;
		this.SN = SN;
	}

	public List<Integer> getSN() {
		return SN;
	}

	public void setSN(List<Integer> SN) {
		this.SN = SN;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getX1() {
		return x1;
	}

	public void setX1(int x1) {
		this.x1 = x1;
	}

	public int getY1() {
		return y1;
	}

	public void setY1(int y1) {
		this.y1 = y1;
	}

	public int getX2() {
		return x2;
	}

	public void setX2(int x2) {
		this.x2 = x2;
	}

	public int getY2() {
		return y2;
	}

	public void setY2(int y2) {
		this.y2 = y2;
	}

	public int getR() {
		return r;
	}

	public void setR(int r) {
		this.r = r;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
