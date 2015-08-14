/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.provider;

/**
 *
 * @author Admin
 */
public class MapleSkillInfo extends MapleSQLData {

    private int id;
    private int jobid;

    public MapleSkillInfo(int id, String name, int jobid) {
        this.id = id;
        setName(name);
        this.jobid = jobid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setJobid(int jobid) {
        this.jobid = jobid;
    }

    public int getJobid() {
        return jobid;
    }

    @Override
    public String toString() {
        return String.format("技能：【%s】.ID：【%d】", getName(), id);
    }



    public void doCreate(String[] path, String value, int index, MapleSQLData data) {
        String str = path[index];
        if (path.length > index + 1) {//还有子目录
            MapleSQLData cdata = data.findNode(str);
            doCreate(path, value, index + 1, cdata);
        } else if (value == null) {//设置空节点
            MapleSQLData cdata = new MapleSQLData();
            cdata.setName(str);
            data.addNote(cdata);
        } else {
            data.addValues(str, value);
        }
    }
}
