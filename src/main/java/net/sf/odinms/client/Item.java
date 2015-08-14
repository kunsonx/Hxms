package net.sf.odinms.client;

import java.sql.Timestamp;
import net.sf.odinms.server.constants.InventoryConstants.Items.Flags;
import org.apache.log4j.Logger;

public class Item implements IItem {

	protected static Logger logger = Logger.getLogger(Item.class);
	protected String dbid;
	protected int itemId;
	protected short position;
	protected short quantity;
	protected String owner = "";
	protected String sender = "";
	protected String message = "";
	protected StringBuffer log = new StringBuffer();
	protected int flag;
	protected Timestamp expiration;
	protected long uniqueid;
	protected int sn;

	public Item(int id, short position, short quantity) {
		super();
		this.itemId = id;
		this.position = position;
		this.quantity = quantity;
		this.flag = 0;
	}

	@Override
	public IItem copy() {
		Item ret = new Item(itemId, position, quantity);
		ret.owner = owner;
		ret.log = new StringBuffer();
		return ret;
	}

	@Override
	public void setPosition(short position) {
		this.position = position;
	}

	@Override
	public void setQuantity(short quantity) {
		this.quantity = quantity;
	}

	@Override
	public int getItemId() {
		return itemId;
	}

	@Override
	public short getPosition() {
		return position;
	}

	@Override
	public short getQuantity() {
		return quantity;
	}

	@Override
	public byte getType() {
		return IItem.ITEM;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	@Override
	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public int compareTo(IItem other) {
		if (Math.abs(position) < Math.abs(other.getPosition())) {
			return -1;
		} else if (Math.abs(position) == Math.abs(other.getPosition())) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public String toString() {
		return "Item: " + itemId + " quantity: " + quantity;
	}

	@Override
	public Timestamp getExpiration() {
		return expiration;
	}

	@Override
	public void setExpiration(Timestamp expire) {
		this.expiration = expire;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getSender() {
		return sender;
	}

	@Override
	public int getSN() {
		return sn;
	}

	@Override
	public void setSN(int sn) {
		this.sn = sn;
	}

	@Override
	public long getUniqueid() {
		return uniqueid;
	}

	@Override
	public void setUniqueId(long id) {
		this.uniqueid = id;
	}

	private int getSn() {
		return sn;
	}

	public void setMessage(String msg) {
		this.message = msg;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public boolean 友谊戒指() {
		switch (itemId) {
		case 1112800: // 四叶挚友戒指
		case 1112801: // 雏菊挚友戒指
		case 1112802: // 闪星挚友戒指
		case 1112803: // 海滩聊天戒指
		case 1112810: // 圣诞夜响叮当
		case 1112811: // 圣诞华丽派对
		case 1112817:// 蝴蝶
		case 1049000:// 友情
			return true;
		}
		return false;
	}

	@Override
	public boolean 恋人戒指() {
		switch (itemId) {
		case 1112000: // 闪光戒指
		case 1112001: // 恋人戒指
		case 1112002: // 纯爱恋人戒指
		case 1112003: // 丘比特戒指
		case 1112005: // 维纳斯戒指
		case 1112006: // 圣十字架戒指
		case 1112007: // 许愿情侣戒指
		case 1112012: // 红玫瑰戒指
		case 1112013: // 爱情红线戒指
		case 1112015: // 白金戒指
		case 1112820:// 龙凤
		case 1112812: // 我的麻吉好友
		case 1048000:// ?T1048000
		case 1048001:
		case 1048002:
		case 1112816:
		case 1112014:
			return true;
		}
		return false;
	}

	@Override
	public boolean 结婚戒指() {
		return itemId == 1112804;
	}

	@Override
	public StringBuffer getLog() {
		return log;
	}

	@Override
	public void setFlag(int i) {
		this.flag = i;
	}

	@Override
	public int getFlag() {
		return this.flag;
	}

	@Override
	public void log(String str, boolean fromdrop) {
		log.append(str).append("- FROMDROP:").append(fromdrop);
	}

	@Override
	public void AddFlag(Flags flag) {
		if (!HasFlag(flag)) {
			this.flag |= flag.GetValue();
		}
	}

	@Override
	public void CanceFlag(Flags flag) {
		if (HasFlag(flag)) {
			this.flag ^= flag.GetValue();
		}
	}

	@Override
	public boolean HasFlag(Flags flag) {
		return (this.flag & flag.GetValue()) == flag.GetValue();
	}

	public String getDbid() {
		return dbid;
	}

	private void setDbid(String dbid) {
		this.dbid = dbid;
	}

	private void setItemId(int itemId) {
		this.itemId = itemId;
	}

	private void setSn(int sn) {
		this.sn = sn;
	}

	private void setUniqueid(long uniqueid) {
		this.uniqueid = uniqueid;
	}
}