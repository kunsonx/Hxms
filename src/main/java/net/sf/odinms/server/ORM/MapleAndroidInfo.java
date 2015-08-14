/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Admin
 */
public class MapleAndroidInfo {

	private int id;
	private List<MapleAndroidInfoFace> faces = new ArrayList<MapleAndroidInfoFace>();
	private List<MapleAndroidInfoHair> hairs = new ArrayList<MapleAndroidInfoHair>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<MapleAndroidInfoFace> getFaces() {
		return faces;
	}

	public void setFaces(List<MapleAndroidInfoFace> faces) {
		this.faces = faces;
	}

	public List<MapleAndroidInfoHair> getHairs() {
		return hairs;
	}

	public void setHairs(List<MapleAndroidInfoHair> hairs) {
		this.hairs = hairs;
	}
}
