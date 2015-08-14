/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.odinms.client.messages.ScriptCommandHelp;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.RecvPacketOpcode;
import net.sf.odinms.net.SendPacketOpcode;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.servlet.WorldServerConfig;
import net.sf.odinms.net.world.WorldServer;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.server.maps.MapleMapInfo;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.Randomizer;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.dom4j.CDATA;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.logicalcobwebs.proxool.ProxoolException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Administrator
 */
public class TestMain {

	public static interface jiekou {

		public void say();
	}

	public static class renlei {

		String name = "人类";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class TimerTaskTest extends java.util.TimerTask {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			System.out.println("start");
			System.out.println(Thread.currentThread().getName());
		}
	}

	public static class tangzongkai extends renlei implements kaiche, chifan {

		@Override
		public String getName() {
			return super.getName() + ":汤忠凯";
		}

		@Override
		public String kaiche() {
			return "我有B2会开车";
		}

		@Override
		public String chifan() {
			return "我在吃饭";
		}
	}

	public static class yekunpeng extends renlei {

		@Override
		public String getName() {
			return super.getName() + "：叶鹍鹏";
		}
	}

	public static abstract class dongwu implements kaiche {

		void yaoren() {
		}

		;

		@Override
		public String kaiche() {
			return "我不是人也会开车";
		}
	}

	public static interface kaiche {

		String kaiche();
	}

	public static interface chifan {

		String chifan();
	}

	public static Collection<String> contacts = Arrays.asList("ceshi1",
			"ceshi2");
	private long id;
	private Map<Integer, String> student = new HashMap<Integer, String>();

	public Collection<String> getContacts() {
		return contacts;
	}

	public void setContacts(Collection<String> contacts) {
		this.contacts = contacts;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) throws SQLException,
			ParserConfigurationException, SAXException, IOException,
			ProxoolException, InterruptedException, ClassNotFoundException,
			ServletException, LifecycleException, URISyntaxException,
			URISyntaxException {
		try {
			// System.out.println(_exp.length);
			// TODO code application logic here
			// 1152921504606846976
			// 1152921504606846976 2022746
			// 129697285800000000
			// 1324628068328
			/**
			 * log.error("转换远程攻击错误：" + "damage：" + ret.allDamage + "\r\npacket："
			 * + lea.toString() + "\r\nplayerName：" + chr.getName() +
			 * "\r\nskillId：" + ret.skill + "\r\nskillName：" +
			 * SkillFactory.getSkillName(ret.skill) + "\r\nskillLevel:" +
			 * chr.getSkillLevel(SkillFactory.getSkill(ret.skill)) +
			 * "\r\n是否拥有精灵的祝福:" + chr.getCygnusBless() + "\r\n", ex);
			 */
			/*
			 * File file = new File("TestMain.class"); long time =
			 * file.lastModified(); MaplePacketLittleEndianWriter mplew = new
			 * MaplePacketLittleEndianWriter();// time =
			 * System.currentTimeMillis(); long ttime =
			 * DateUtil.getFileTimestamp(time); mplew.writeLong(ttime);
			 */
			// 01/28/2012 23:38:57
			// 01/28/2012 23:40:06
			// // DriverManager.getConnection("proxool.odinms");
			// System.out.println(CashItemFactory.getItemInSql(10000070).getItemId());
			/*
			 * String[] strings = {"1", "2", "3", "4", "5"}; Map<String,
			 * Integer> map = new HashMap<String, Integer>(); MapleMapCore core
			 * = new MapleMapCore(); Map<Integer, MapleNPC> maps = new
			 * CopyOnWriteMap<Integer, MapleNPC>(); for (int i = 0; i < 100;
			 * i++) { MapleNPC npc = new MapleNPC(i, null);
			 * System.out.println("OID:" + npc.getObjectId());
			 * core.addMapObject(npc); maps.put(npc.getObjectId(), npc); } for
			 * (int i = 0; i < 98; i++) { core.removeMapObject(i + 1); } for
			 * (MapleNPC mapleNPC : Collections.unmodifiableMap(maps).values())
			 * { if (Math.random() > 0.2) { maps.remove(mapleNPC.getObjectId());
			 * } }
			 * 
			 * objects_lock.lock(); try { objects_lock.lock();
			 * objects_lock.lock(); objects_lock.lock(); objects_lock.lock();
			 * System.out.println(32 / 10); map.put("1", 1); map.put("2", 2); }
			 * finally { objects_lock.unlock(); } strings =
			 * Arrays.copyOf(strings, 10); strings = Arrays.copyOf(strings, 5);
			 * int i = 32; i *= 2; i /= 2;
			 */
			/*
			 * 
			 * 
			 * ArrayList<Integer> list = new ArrayList<Integer>(); list.add(1);
			 * list.add(2); Integer[] arrays = list.toArray(new Integer[0]);
			 * int[] array = new int[arrays.length]; for (int i = 0; i <
			 * arrays.length; i++) { Integer integer = arrays[i]; array[i] =
			 * integer; } new Thread(new Runnable() {
			 * 
			 * @Override public void run() { try { Thread.sleep(1000);
			 * System.out.println(recvmutex.tryLock()); } catch
			 * (InterruptedException ex) {
			 * java.util.logging.Logger.getLogger(TestMain
			 * .class.getName()).log(Level.SEVERE, null, ex); } } }).start();
			 * recvmutex.lock();
			 */
			/*
			 * long current = System.nanoTime(); byte b = (byte) 0x86; int
			 * numCommands = b >= 0 ? b : b + 256; System.out.println(b + ":" +
			 * numCommands); Thread.sleep(2000); long time = System.nanoTime() -
			 * current; double time_ = time / 1000000000.0;
			 * System.out.println(Long.MAX_VALUE);
			 * 
			 * 
			 * 
			 * System.out.println((int)
			 * ((Runtime.getRuntime().availableProcessors() + 1) * 1.5));
			 */
			/*
			 * Calendar calendar = Calendar.getInstance(); calendar.setTime(new
			 * Date()); System.out.println(calendar.get(Calendar.DAY_OF_MONTH));
			 */
			/*
			 * double sum; //最终付款 int i = 0; //选择换购的编号 double money; //金额 double
			 * t = 0; //选择后应加的金额 String str = "";
			 * System.out.println("有以下换购项目：");
			 * System.out.println("1.满50元，加2元换购百事可乐饮料一瓶。");
			 * System.out.println("2.满100元，加3元换购500ml可乐一瓶。");
			 * System.out.println("3.满100元，加10元换购5公斤面粉。");
			 * System.out.println("4.满200元，加10元换购1个苏泊尔炒菜锅。");
			 * System.out.println("5.满200元，加20元换购欧莱雅爽肤水一瓶。");
			 * System.out.println("0.不换购。"); System.out.print("请输入您的金额:");
			 * Scanner input = new Scanner(System.in); money =
			 * input.nextDouble(); //输入的值赋给money System.out.print("请输入您的选择：");
			 * if (input.hasNextInt()) { i = input.nextInt(); if (i == 0) {
			 * System.out.println("下次再见。程序退出。"); return; } } else {
			 * System.out.println("输入不正确。程序退出。"); return; }
			 * 
			 * if (0 >= i && 6 > i) { System.out.println("选择不正确。程序退出。"); return;
			 * } if (!((i == 1 && money >= 50) || (i == 2 && money >= 100) || (i
			 * == 3 && money >= 100) || (i == 4 && money >= 200) || (i == 5 &&
			 * money >= 200))) { System.out.println("不满足换购条件。程序退出。"); return; }
			 * if (i != 0) { switch (i) { case 1: t = 2; str = "成功换购到百事可乐一瓶！";
			 * break; case 2: t = 3; str = "成功换购到500ml可乐一瓶！"; break; case 3: t =
			 * 10; str = "成功换购到5公斤面粉！"; break; case 4: t = 10; str =
			 * "成功换购1个苏泊尔炒菜锅"; break; case 5: t = 20; str = "成功换购欧莱雅爽肤水一瓶！";
			 * break; } } else { System.out.println("你没有选择换购！"); } sum = money +
			 * t; System.out.println("您最终要付：" + sum); System.out.println(str);
			 */
			/*
			 * MaplePacketLittleEndianWriter mplew = new
			 * MaplePacketLittleEndianWriter();
			 * mplew.writeLong(DateUtil.getFileTimestamp(-2209017600000L));
			 * System.out.println(mplew.toString());
			 */
			// System.out.println(SkillFactory.getSkill(80001130).hasMastery());
			/*
			 * MaplePacketLittleEndianWriter mplew = new
			 * MaplePacketLittleEndianWriter();
			 * mplew.writeLong(DateUtil.getFileTimestamp
			 * (MaplePacketCreator.FINAL_TIME));
			 * System.out.println(mplew.toString());
			 */
			// System.out.println(CashItemFactory.getSnFromId(5150004));
			/*
			 * ByteBuffer buffer = ByteBuffer.allocateDirect(testdata.length +
			 * 4); buffer.put(testkey); buffer.put(testdata);
			 * System.out.println("普通写入前 buffer" + HexTool.toString(buffer));
			 * buffer.flip(); buffer.position(5); System.out.println("写前字节：" +
			 * buffer.asReadOnlyBuffer().get()); System.out.println("写前字节：" +
			 * buffer.asReadOnlyBuffer().get());
			 * 
			 * buffer.put((byte) 9); buffer.put((byte) 9); buffer.rewind();
			 * byte[] j = new byte[buffer.limit()]; buffer.get(j);
			 * 
			 * System.out.println("普通写入后 buffer" + HexTool.toString(j));
			 */
			/*
			 * System.out.println(UUID.randomUUID().toString()); MapleData f =
			 * SkillInfoManager.getSkill(1101006).getChildByPath("common/lt");
			 * Object obj = f.getData(); Skill skill = (Skill)
			 * SkillFactory.getSkill(1101006); skill = (Skill)
			 * SkillFactory.getSkill(1); skill = (Skill)
			 * SkillFactory.getSkill(1000); skill = (Skill)
			 * SkillFactory.getSkill(1000);
			 * System.out.print(SkillFactory.getSkillName(1000)); Integer[] w =
			 * SkillFactory.getSkills(132); for (int integer :
			 * SkillFactory.getSkills(132)) { System.out.println(integer); }
			 */
			/*
			 * TimerManager.getInstance().start(); MapleMapFactory mapFactory =
			 * new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new
			 * File(System.getProperty("net.sf.odinms.wzpath") + "/Map.wz")),
			 * MapleDataProviderFactory.getDataProvider(new
			 * File(System.getProperty("net.sf.odinms.wzpath") +
			 * "/String.wz"))); mapFactory.getMap(922000009);
			 * CashItemFactory.getPackageItems(9100000);
			 * TimerManager.getInstance().stop(); Session session =
			 * DatabaseConnection.getSession(); Transaction transaction =
			 * session.beginTransaction(); MapleNodes node = (MapleNodes)
			 * session.get(MapleNodes.class, 1); session.saveOrUpdate(node);
			 * transaction.commit(); session.close();
			 */
			/*
			 * Date date = new Date(); date.setMonth(1); date.setDate(1);
			 * System.out.println( (date.getYear() % 100) * 100000000 +
			 * (date.getMonth() + 1) * 1000000 + date.getDate() * 10000 +
			 * (date.getHours()) * 100 + date.getMinutes());
			 * System.out.println(1 << 27); for (int i =
			 * GameConstants.MAX_STATUS; i > 0; i--) { System.out.println(i); }
			 */
			// int skillType = MapleDataTool.getInt("effect/animationTime",
			// info, -1);
			/*
			 * Tomcat tomcat = new Tomcat();
			 * tomcat.setBaseDir(System.getProperty("user.dir"));
			 * tomcat.setPort(8081); StandardContext sc = (StandardContext)
			 * tomcat.addWebapp("", "D:/097/WebServer/web"); tomcat.start();
			 * 
			 * 
			 * Thread.sleep(Long.MAX_VALUE);
			 */
			/*
			 * Session session = DatabaseConnection.getSession(); Transaction
			 * transaction = session.beginTransaction(); MapleCharAttribute
			 * attribute = new MapleCharAttribute(); attribute.setCid(1);
			 * attribute.setAttribute(new HashMap<String, String>());
			 * attribute.getAttribute().put("1", "2");
			 * 
			 * session.saveOrUpdate(attribute); transaction.commit();
			 * session.close();
			 */
			/*
			 * FileReader fr = new FileReader("d://test.js");
			 * ScriptEngineManager sem = new ScriptEngineManager();
			 * ScriptEngineFactory sef =
			 * sem.getEngineByName("javascript").getFactory(); ScriptEngine
			 * command = sef.getScriptEngine(); CompiledScript compiled =
			 * ((Compilable) command).compile(fr); compiled.eval(); Invocable
			 * invocable = (Invocable) command; for (Object string :
			 * ScriptCommandHelp.toArray(command.get("list"))) {
			 * System.out.println(string); }
			 */
			/*
			 * NativeArray sr = (NativeArray) invocable.invokeFunction("a",
			 * (Object) null); for (int i = 0; i < sr.getLength(); i++) {
			 * System.out.println(sr.get(i, sr)); }
			 */
			// NativeFunction function = (NativeFunction)
			// invocable.invokeFunction("a", (Object) null);
			/*
			 * MaplePacketLittleEndianWriter mplew = new
			 * MaplePacketLittleEndianWriter(); mplew.writeShort(1);
			 * System.out.println(0x54>>4);
			 */
			/*
			 * String[][] str = { {"","123"} };
			 */
			// System.out.println(str[0].length);
			/*
			 * for (int i = 0; i < 3; i++) { System.out.println(i); }
			 */
			/*
			 * for (int i = 0; i < 10; i++) {
			 * System.out.println(Randomizer.getInstance().nextInt(2)); }
			 */
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new File("D:\\097\\HxmsConfig.xml"));
			System.out.println(doc instanceof Serializable);
			NodeList list = doc.getDocumentElement().getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				System.out.println(list.item(i).getNodeName());
			}
			System.out.println(doc.getElementsByTagName("WorldServer").item(0)
					.getAttributes().getNamedItem("alt").getNodeValue());
			System.out.println(doc.getElementsByTagName("LoginServer").item(0)
					.getAttributes().getNamedItem("ServerName").getNodeValue());

			// 这是一个数字
			Number number = 1;
			int j = 2;
			number = j;
			number = 3.3;

		} catch (Exception ex) {
			Logger.getLogger(TestMain.class.getName()).log(Level.SEVERE, null,
					ex);
		}

	}

	public static void j() {
		List<String> list = new ArrayList<String>();
		list.add("132");
		k(list);
		System.out.println(list);
	}

	public static void k(List<String> kList) {
		kList = new ArrayList<String>();
	}

	public static void test(renlei j) {
		System.out.println(j.getName());
	}

	public String create(String str, String filePath, int width, int height)
			throws FileNotFoundException, IOException {

		String fileName = System.currentTimeMillis() + ".jpg";
		String path = filePath + "/" + fileName;
		File file = new File(path);

		BufferedImage bi = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = (Graphics2D) bi.getGraphics();
		g2.setBackground(Color.WHITE);
		g2.clearRect(0, 0, width, height);

		Font font = new Font("黑体", Font.BOLD, 25);
		g2.setFont(font);
		g2.setPaint(Color.RED);

		FontRenderContext context = g2.getFontRenderContext();
		Rectangle2D bounds = font.getStringBounds(str, context);
		double x = (width - bounds.getWidth()) / 2;
		double y = (height - bounds.getHeight()) / 2;
		double ascent = -bounds.getY();
		double baseY = y + ascent;

		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2.drawString(str, (int) x, (int) baseY);
		try {
			ImageIO.write(bi, "jpg", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file.getPath();
	}

	public static int Test() {
		try {
			int x = 2;
			switch (x) {
			default:
				System.out.println("default");
			case 0:
				System.out.println("zero");
				break;
			case 1:
				System.out.println("one");
			case 2:
				System.out.println("two");
			}

			return 0;
		} finally {
			System.out.print("代码块");
		}
	}
}
