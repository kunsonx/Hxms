/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools;

import java.io.Serializable;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;

/**
 *
 * @author Administrator
 */
public class InitPlayerInterceptor extends EmptyInterceptor {

    private int id;
    private MapleClient client;

    public InitPlayerInterceptor(int id, MapleClient client) {
        this.id = id;
        this.client = client;
    }

    @Override
    public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
        if (entityName.equals("MainPlayer")
                && entityMode.equals(EntityMode.POJO)
                && id.equals(this.id)) {
            return new MapleCharacter(client, this.id);
        }
        return null;
    }
}
