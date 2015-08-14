/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.provider;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.wz.MapleDataType;

/**
 *
 * @author Admin
 */
public class MapleSQLDataValue implements MapleData {

	private long id;
	private String name;
	private String value;

	public MapleSQLDataValue(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public MapleSQLDataValue() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("值类型：【%s】【%s】", name, value);
	}

	@Override
	public MapleDataType getType() {
		return MapleDataType.STRING;
	}

	@Override
	public List<MapleData> getChildren() {
		return null;
	}

	@Override
	public MapleData getChildByPath(String path) {
		return null;
	}

	@Override
	public Object getData() {
		if (name.equals("lt") || name.equals("rb")) {
			String[] str = getValue().split("/");
			return new Point(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
		} else {
			return getValue();
		}
	}

	@Override
	public MapleDataEntity getParent() {
		return null;
	}

	@Override
	public Iterator<MapleData> iterator() {
		return null;
	}
}
