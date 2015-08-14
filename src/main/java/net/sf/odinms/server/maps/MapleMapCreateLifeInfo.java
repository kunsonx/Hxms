/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.awt.Point;
import net.sf.odinms.server.life.AbstractLoadedMapleLife;
import net.sf.odinms.server.life.MapleLifeFactory;
import org.apache.log4j.Logger;

/**
 *
 * @author Admin
 */
public class MapleMapCreateLifeInfo implements java.io.Serializable {

	private static final Logger log = Logger
			.getLogger(MapleMapCreateLifeInfo.class);
	private long d_id;
	private int id;
	private int mapid;
	private String type;
	private int cy, f = 0, fh, rx0, rx1, hide;
	private Point position;
	private int mobtime;

	public MapleMapCreateLifeInfo() {
	}

	public MapleMapCreateLifeInfo(int id, int mapid, String type, int cy,
			int f, int fh, int rx0, int rx1, int hide, Point position) {
		this.id = id;
		this.mapid = mapid;
		this.type = type;
		this.cy = cy;
		this.f = f;
		this.fh = fh;
		this.rx0 = rx0;
		this.rx1 = rx1;
		this.hide = hide;
		this.position = position;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getMapid() {
		return mapid;
	}

	public void setMapid(int mapid) {
		this.mapid = mapid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getCy() {
		return cy;
	}

	public void setCy(int cy) {
		this.cy = cy;
	}

	public int getF() {
		return f;
	}

	public void setF(int f) {
		this.f = f;
	}

	public int getFh() {
		return fh;
	}

	public void setFh(int fh) {
		this.fh = fh;
	}

	public int getRx0() {
		return rx0;
	}

	public void setRx0(int rx0) {
		this.rx0 = rx0;
	}

	public int getRx1() {
		return rx1;
	}

	public void setRx1(int rx1) {
		this.rx1 = rx1;
	}

	public int getHide() {
		return hide;
	}

	public void setHide(int hide) {
		this.hide = hide;
	}

	public Point getPosition() {
		return position;
	}

	public void setPosition(Point position) {
		this.position = position;
	}

	public int getMobtime() {
		return mobtime;
	}

	public void setMobtime(int mobtime) {
		this.mobtime = mobtime;
	}

	public long getD_id() {
		return d_id;
	}

	public void setD_id(long d_id) {
		this.d_id = d_id;
	}

	public AbstractLoadedMapleLife getLife() {
		AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(getId(),
				getType());
		myLife.setCy(getCy());
		myLife.setF(getF());
		myLife.setFh(getFh());
		myLife.setRx0(getRx0());
		myLife.setRx1(getRx1());
		myLife.setPosition(getPosition());
		if (getHide() == 1) {
			myLife.setHide(true);
		} else if (getHide() > 1) {
			log.warn("Hide > 1 (" + getHide() + ")");
		}
		return myLife;
	}
}
