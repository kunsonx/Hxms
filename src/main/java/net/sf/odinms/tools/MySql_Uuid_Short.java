/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools;

import java.io.Serializable;
import java.util.UUID;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 *
 * @author Administrator
 */
public class MySql_Uuid_Short implements IdentifierGenerator {

    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        UUID id = UUID.randomUUID();
        return Randomizer.getInstance().nextInt(2) == 0 ? id.getLeastSignificantBits() : id.getMostSignificantBits();
    }
}
