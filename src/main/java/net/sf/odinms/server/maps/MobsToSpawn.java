/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import net.sf.odinms.tools.Pair;

/**
 *
 * @author Admin
 */
public class MobsToSpawn extends Pair<Integer, Integer> {

    private long id;

    private MobsToSpawn() {
        super(0, 0);
    }

    public MobsToSpawn(Integer key, Integer value) {
        super(key, value);
    }

    public Integer getLeft_() {
        return super.getLeft();
    }

    public Integer getRight_() {
        return super.getRight();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setLeft_(Integer left) {
        this.left = left;
    }

    public void setRight_(Integer right) {
        this.right = right;
    }
}
