/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.maps;

import java.awt.Rectangle;

/**
 *
 * @author Admin
 */
public class MyRectangle extends Rectangle {

    private long id;

    private MyRectangle() {
    }

    public MyRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int get_X() {
        return x;
    }

    public int get_Y() {
        return y;
    }

    public int get_Width() {
        return width;
    }

    public int get_Height() {
        return height;
    }

    public void set_Height(int height) {
        this.height = height;
    }

    public void set_Width(int width) {
        this.width = width;
    }

    public void set_X(int x) {
        this.x = x;
    }

    public void set_Y(int y) {
        this.y = y;
    }
}
