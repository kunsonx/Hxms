/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.sf.odinms.provider.wz.MapleDataType;

/**
 *
 * @author Admin
 */
public class MapleSQLData implements MapleData {
    
    private String name;
    private MapleData[] nodes = new MapleData[0];
    
    public void addNote(MapleSQLData data) {
        addNode(data);
    }
    
    public void addValues(String key, String value) {
        addNode(new MapleSQLDataValue(key, value));
    }
    
    public void addNode(MapleData data) {
        nodes = Arrays.copyOf(nodes, nodes.length + 1);
        nodes[nodes.length - 1] = data;
    }
    
    @Override
    public String toString() {
        return "节点：" + getName();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public MapleData[] getDatas() {
        return nodes;
    }
    
    public List<MapleSQLDataValue> getValues() {
        List<MapleSQLDataValue> values = new ArrayList<MapleSQLDataValue>();
        for (MapleData mapleData : getDatas()) {
            if (mapleData.getType().equals(MapleDataType.STRING)) {
                values.add((MapleSQLDataValue) mapleData);
            }
        }
        return values;
    }
    
    public List<MapleSQLData> getNodes() {
        List<MapleSQLData> values = new ArrayList<MapleSQLData>();
        for (MapleData mapleData : getDatas()) {
            if (mapleData.getType().equals(MapleDataType.PROPERTY)) {
                values.add((MapleSQLData) mapleData);
            }
        }
        return values;
    }
    
    public boolean hasNodes() {
        for (MapleData mapleData : getDatas()) {
            if (mapleData.getType().equals(MapleDataType.PROPERTY)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasValues() {
        for (MapleData mapleData : getDatas()) {
            if (mapleData.getType().equals(MapleDataType.STRING)) {
                return true;
            }
        }
        return false;
    }
    
    protected MapleSQLData findNode(String name) {
        MapleSQLData cdata = null;
        for (MapleData mapleSkillData : getDatas()) {
            if (mapleSkillData.getName().equals(name) && mapleSkillData.getType().equals(MapleDataType.PROPERTY)) {
                cdata = (MapleSQLData) mapleSkillData;
                break;
            }
        }
        if (cdata == null) {
            cdata = new MapleSQLData();
            cdata.setName(name);
            addNote(cdata);
        }
        return cdata;
    }
    
    @Override
    public MapleDataType getType() {
        return MapleDataType.PROPERTY;
    }
    
    @Override
    public List<MapleData> getChildren() {
        return Arrays.asList(getDatas());
    }
    
    @Override
    public MapleData getChildByPath(String path) {
        final String segments[] = path.split("/");
        MapleData myNode = this;
        for (int x = 0; x < segments.length; x++) {
            boolean foundChild = false;
            for (MapleData mapleData : myNode.getChildren()) {
                if (mapleData.getName().equals(segments[x])) {
                    myNode = mapleData;
                    foundChild = true;
                    break;
                }
            }
            if (!foundChild) {
                return null;
            }
        }
        return myNode;
    }
    
    @Override
    public Object getData() {
        return nodes;
    }
    
    @Override
    public MapleDataEntity getParent() {
        return null;
    }
    
    @Override
    public Iterator<MapleData> iterator() {
        return Arrays.asList(getDatas()).iterator();
    }
}
