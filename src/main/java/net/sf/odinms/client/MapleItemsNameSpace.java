/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

import java.util.Collection;

/**
 *
 * @author HXMS
 */
public interface MapleItemsNameSpace {

	MapleItemsNameSpaceType GetSpaceType();

	Collection<IItem> AllItems();
}
