/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www;

import java.sql.Timestamp;
import net.sf.odinms.client.MapleCharacter;

/**
 *
 * @author Admin
 */
public class WebUserPlayer implements java.io.Serializable {

	private int id;
	private String name;
	private int vip, str, dex, luk, int_, level, gender, fame, reborns;
	private long meso;
	private Timestamp createdata;
	private WebPlayerItems items = null;

	public WebUserPlayer() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getVip() {
		return vip;
	}

	public void setVip(int vip) {
		this.vip = vip;
	}

	public int getStr() {
		return str;
	}

	public void setStr(int str) {
		this.str = str;
	}

	public int getDex() {
		return dex;
	}

	public void setDex(int dex) {
		this.dex = dex;
	}

	public int getLuk() {
		return luk;
	}

	public void setLuk(int luk) {
		this.luk = luk;
	}

	public int getInt_() {
		return int_;
	}

	public void setInt_(int int_) {
		this.int_ = int_;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public long getMeso() {
		return meso;
	}

	public void setMeso(long meso) {
		this.meso = meso;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public int getFame() {
		return fame;
	}

	public void setFame(int fame) {
		this.fame = fame;
	}

	public Timestamp getCreatedata() {
		return createdata;
	}

	public void setCreatedata(Timestamp createdata) {
		this.createdata = createdata;
	}

	public int getReborns() {
		return reborns;
	}

	public void setReborns(int reborns) {
		this.reborns = reborns;
	}

	public WebPlayerItems Items() {
		synchronized (this) {
			if (items == null) {
				items = WebPlayerItems.get(MapleCharacter.getIdByName(name),
						MapleCharacter.getAccountIdByName(name), this);
			}
		}
		return items;
	}

	public WebPlayerItems getItems() {
		return null;
	}

	public void setItems(WebPlayerItems items) {
		this.items = items;
	}
}
