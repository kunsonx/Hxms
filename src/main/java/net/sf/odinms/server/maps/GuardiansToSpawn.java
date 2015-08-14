/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.awt.Point;
import net.sf.odinms.tools.Pair;

/**
 *
 * @author Admin
 */
public class GuardiansToSpawn extends Pair<Point, Integer> {

    public long id;

    private GuardiansToSpawn() {
        super(new Point(), 0);
    }

    public GuardiansToSpawn(Point left, Integer right) {
        super(left, right);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setLeft_X(int i) {
        this.left.x = i;
    }

    public int getLeft_X() {
        return left.x;
    }

    public void setLeft_Y(int i) {
        this.left.y = i;
    }

    public int getLeft_Y() {
        return left.y;
    }

    public Integer getRight_() {
        return super.getRight();
    }

    public void setRight_(Integer right) {
        this.right = right;
    }
}
