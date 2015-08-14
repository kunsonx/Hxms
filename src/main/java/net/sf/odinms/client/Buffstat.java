/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

/**
 *
 * @author Admin
 */
public interface Buffstat {

    public int getValue(boolean foreign, boolean give);

    public int getPosition();

    public int getMask();
}
