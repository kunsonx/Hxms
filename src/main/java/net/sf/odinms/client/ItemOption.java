/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;

/**
 *
 * @author Administrator
 */
public class ItemOption {

	private int id, optionType, reqLevel;
	private String string;
	private List<ItemOptionEffect> effects = new ArrayList<ItemOptionEffect>();

	private ItemOption() {
	}

	private ItemOption(final int id) {
		this.id = id;
	}

	public static ItemOption loadFromData(int id, MapleData data) {
		ItemOption ret = new ItemOption(id);
		MapleData info = data.getChildByPath("info");
		ret.optionType = MapleDataTool.getInt("optionType", info, 0);
		ret.reqLevel = MapleDataTool.getInt("reqLevel", info, 0);
		ret.string = MapleDataTool.getString("string", info, "");
		MapleData levels = data.getChildByPath("level");
		for (MapleData level : levels.getChildren()) {
			ret.effects.add(ItemOptionEffect.loadFromData(id, level));
		}

		return ret;
	}

	public ItemOptionEffect getEffect(int level) {
		return effects.get(level - 1);
	}

	private void setId(int id) {
		this.id = id;
	}

	private void setOptionType(int optionType) {
		this.optionType = optionType;
	}

	private void setReqLevel(int reqLevel) {
		this.reqLevel = reqLevel;
	}

	private void setString(String string) {
		this.string = string;
	}

	public List<ItemOptionEffect> getEffects() {
		return effects;
	}

	public void setEffects(List<ItemOptionEffect> effects) {
		this.effects = effects;
	}

	public int getId() {
		return id;
	}

	public int getOptionType() {
		return optionType;
	}

	public int getReqLevel() {
		return reqLevel;
	}

	public String getString() {
		return string;
	}
}