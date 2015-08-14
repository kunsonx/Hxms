package net.sf.odinms.client;

import java.sql.Timestamp;
import net.sf.odinms.server.constants.InventoryConstants;

public interface IItem extends Comparable<IItem> {

	public final int PET = 3;
	public final int ITEM = 2;
	public final int EQUIP = 1;

	byte getType();

	short getPosition();

	void setPosition(short position);

	void setFlag(int i);

	int getFlag();

	int getItemId();

	short getQuantity();

	String getOwner();

	IItem copy();

	void setOwner(String owner);

	void setQuantity(short quantity);

	StringBuffer getLog();

	void log(String str, boolean fromdrop);

	Timestamp getExpiration();

	void setExpiration(Timestamp expire);

	int getSN();

	long getUniqueid();

	void setUniqueId(long id);

	void setSN(int sn);

	void AddFlag(InventoryConstants.Items.Flags flag);

	void CanceFlag(InventoryConstants.Items.Flags flag);

	boolean HasFlag(InventoryConstants.Items.Flags flag);

	boolean 友谊戒指();

	boolean 恋人戒指();

	boolean 结婚戒指();
}