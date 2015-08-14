/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.life;

import java.awt.Point;
import java.io.Serializable;
import java.util.List;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.ORM.MapleAndroidInfo;
import net.sf.odinms.server.movement.AbsoluteLifeMovement;
import net.sf.odinms.server.movement.LifeMovement;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.Randomizer;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author HXMS
 */
public class MapleAndroid implements Serializable {

	private static final long serialVersionUID = 9179541993413738569L;
	private int stance = 0, itemid, skin, hair, face;
	private String name;
	private Point pos = new Point(0, 0);
	private long uniqueid;

	// uniqueid, skin, hair, face, name
	private MapleAndroid() {
	}

	private MapleAndroid(final int itemid, final long uniqueid) {
		this.itemid = itemid;
		this.uniqueid = uniqueid;
	}

	public static MapleAndroid loadFromDb(final int itemid, final long uid) {
		Session session = DatabaseConnection.getSession();
		MapleAndroid android = (MapleAndroid) session.get(MapleAndroid.class,
				uid);
		// session.createQuery("from MapleAndroid as an where an.uniqueid =:uniqueid").setParameter("uniqueid",
		// uid).uniqueResult();
		session.close();
		if (android != null) {
			android.setItemid(itemid);
		}

		return android;
	}

	// "Android"
	public static MapleAndroid create(final int itemid, final long uniqueid) {
		int zxc = itemid - 1661999;
		if (zxc > 4) {
			zxc = 4;
		} else {
			zxc = itemid - 1661999;
		}
		MapleAndroidInfo aInfo = MapleItemInformationProvider.getInstance()
				.getAndroidInfo(zxc);
		if (aInfo == null) {
			return null;
		}
		return create(
				itemid,
				uniqueid,
				0,
				aInfo.getHairs()
						.get(Randomizer.getInstance().nextInt(
								aInfo.getHairs().size())).getValue(),
				aInfo.getFaces()
						.get(Randomizer.getInstance().nextInt(
								aInfo.getFaces().size())).getValue());
	}

	public static MapleAndroid create(int itemid, long uniqueid, int skin,
			int hair, int face) {
		final MapleAndroid and = new MapleAndroid(itemid, uniqueid);
		and.setSkin(skin);
		and.setHair(hair);
		and.setFace(face);
		and.setName("智能机器人");
		Session session = DatabaseConnection.getSession();
		Transaction transaction = session.beginTransaction();
		session.saveOrUpdate(and);
		transaction.commit();
		session.close();
		return and;
	}

	public long getUniqueId() {
		return uniqueid;
	}

	public final void setHair(final int closeness) {
		this.hair = closeness;
	}

	public final int getHair() {
		return hair;
	}

	public final void setFace(final int closeness) {
		this.face = closeness;
	}

	public final int getFace() {
		return face;
	}

	public String getName() {
		return name;
	}

	public void setName(String n) {
		this.name = n;
	}

	public final void setSkin(final int s) {
		this.skin = s;
	}

	public final int getSkin() {
		return skin;
	}

	public final Point getPos() {
		return pos;
	}

	public final void setPos(final Point pos) {
		this.pos = pos;
	}

	public final int getStance() {
		return stance;
	}

	public final void setStance(final int stance) {
		this.stance = stance;
	}

	public final int getItemId() {
		return itemid;
	}

	public long getUniqueid() {
		return uniqueid;
	}

	public void setUniqueid(int uniqueid) {
		this.uniqueid = uniqueid;
	}

	public int getItemid() {
		return itemid;
	}

	public void setItemid(int itemid) {
		this.itemid = itemid;
	}

	public void setUniqueid(long uniqueid) {
		this.uniqueid = uniqueid;
	}

	public final void updatePosition(final List<LifeMovementFragment> movement) {
		for (final LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					setPos(((LifeMovement) move).getPosition());
				}
				setStance(((LifeMovement) move).getNewstate());
			}
		}
	}
}