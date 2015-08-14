/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.life;

/**
 *
 * @author Admin
 */
public class MapleMonsterSkill {

	private long id;
	private int skill;
	private int level;

	public MapleMonsterSkill() {
	}

	public MapleMonsterSkill(int skill, int level) {
		this.skill = skill;
		this.level = level;
	}

	public long getId() {
		return id;
	}

	public int getSkill() {
		return skill;
	}

	public int getLevel() {
		return level;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setSkill(int skill) {
		this.skill = skill;
	}

	public void setLevel(int level) {
		this.level = level;
	}
}
