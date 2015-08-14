package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

public class BeansGame1Handler extends AbstractMaplePacketHandler {

	private static final Logger log = Logger.getLogger(BeansGame1Handler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// log.debug("豆豆出现包" +slea.toString());
		MapleCharacter chr = c.getPlayer();
		List<Beans> beansInfo = new ArrayList<Beans>();
		int type = slea.readByte();
		int 力度 = 0;
		int 豆豆序号 = 0;
		if (type == 1) { // 点开始的时候 确认打豆豆的力度
			// 01 E8 03
			力度 = slea.readShort();
			chr.setBeansRange(力度);
			// log.debug("打豆豆力度1："+力度);
			// c.getSession().write(MaplePacketCreator.enableActions());
		} else if (type == 2) { // 没存在的必要
			// 02 1B 00 00 00
			/*
			 * 豆豆序号 = slea.readInt(); //点暂停时显示的下个豆豆的序号 同时也与要扣除的豆数的值相同
			 * chr.gainBeans(豆豆序号 - chr.getBeansNum()); chr.setBeansNum(豆豆序号);
			 * log.debug("扣除的豆豆数量："+豆豆序号);
			 * log.debug("type == 2 "+slea.toString());
			 */
		} else if (type == 0x0B || type == 0) {
			// 0B[11] - 点start/stop的时候获得start/stop时豆豆的力度和序号
			// 0 - 刚打开界面的时候设置的力度
			// 0B E8 03 00 00 00 00
			// 00 88 13 1B 00 00 00
			力度 = slea.readShort();
			豆豆序号 = slea.readInt() + 1;// 这里获得的Int是最后一个豆豆的序号
			chr.setBeansRange(力度);
			chr.setBeansNum(豆豆序号);
			if (豆豆序号 == 1)
				chr.setCanSetBeansNum(false);
			/*
			 * log.debug("混合类型："+type); log.debug("豆豆序号1："+豆豆序号);
			 * log.debug("打豆豆力度2："+力度);
			 */
		} else if (type == 6) { // 点暂停或者满5个豆豆后客户端发送的豆豆信息 最多5个豆豆
			/*
			 * 06 00 01 1B 00 00 00 E8 03 00 4D
			 * 
			 * 06 01 05 01 00 00 00 E8 03 02 52 02 00 00 00 E8 03 02 52 03 00 00
			 * 00 E8 03 02 52 04 00 00 00 E8 03 02 52 05 00 00 00 E8 03 02 52
			 */

			slea.skip(1);
			int 循环次数 = slea.readByte();
			if (循环次数 == 0)
				return;
			else if (循环次数 != 1)
				slea.skip((循环次数 - 1) * 8);
			// int 临时豆豆序号 = slea.readInt();
			// 豆豆序号 = (临时豆豆序号 == 1 ? 0 : 临时豆豆序号) + (chr.getBeansNum() == 临时豆豆序号
			// ? 1 : 0);
			if (chr.isCanSetBeansNum()) {
				chr.setBeansNum(chr.getBeansNum() + 循环次数);
				// log.debug("豆豆序号2："+chr.getBeansNum());
			}
			chr.gainBeans(-循环次数);
			chr.setCanSetBeansNum(true);
			// log.debug("扣除的豆豆数量："+循环次数);
			// log.debug("type == 6 "+slea.toString());
		} else {
			log.debug("未处理的类型【" + type + "】\n包" + slea.toString());
		}
		if (type == 0x0B || type == 6) {
			for (int i = 0; i < 5; i++) {
				beansInfo.add(new Beans(chr.getBeansRange() + rand(-100, 100),
						getBeanType(), chr.getBeansNum() + i));
			}
			c.getSession().write(MaplePacketCreator.showBeans(beansInfo));
		}
		// c.getSession().write(MaplePacketCreator.enableActions());
	}

	private static int getBeanType() {
		int random = rand(1, 100);
		int beanType = 0;
		// 3 - 红, 2 - 蓝, 1 - 绿, 0 - 普通
		switch (random) {
		case 2:
			beanType = 1; // 绿
			break;
		case 49:
			beanType = 2; // 蓝
			break;
		case 99:
			beanType = 3; // 红
			break;
		}
		return beanType;
	}

	private static int rand(int lbound, int ubound) {
		return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
	}

	public class Beans {
		private int number;
		private int type;
		private int pos;

		public Beans(int pos, int type, int number) {
			this.pos = pos;
			this.number = number;
			this.type = type;
		}

		public int getType() {
			return type;
		}

		public int getNumber() {
			return number;
		}

		public int getPos() {
			return pos;
		}
	}
}
