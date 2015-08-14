/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

import net.sf.odinms.client.MapleCharacter;

/**
 *
 * @author hxms
 */
public interface MapleInteractionRoom {

    MapleCharacter getChr();

    MapleInteractionRoom getPartner();

    void cancel();
}
