/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools.test;

import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.SkillMacro;

/**
 *
 * @author Administrator
 */
public class MapleTest {

	private long id;
	private List<SkillMacro> macros = new ArrayList<SkillMacro>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<SkillMacro> getMacros() {
		return macros;
	}

	public void setMacros(List<SkillMacro> macros) {
		this.macros = macros;
	}

	public void addMacro(SkillMacro skillMacro) {
		this.macros.add(skillMacro);
	}
}
