/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client.skills;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Admin
 */
public class MapleSkillDatabase {

	private int id;
	private int jobid;
	private String name;
	private List<MapleSkillDatabaseInfo> values = new ArrayList<MapleSkillDatabaseInfo>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getJobid() {
		return jobid;
	}

	public void setJobid(int jobid) {
		this.jobid = jobid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<MapleSkillDatabaseInfo> getValues() {
		return values;
	}

	public void setValues(List<MapleSkillDatabaseInfo> values) {
		this.values = values;
	}

}
