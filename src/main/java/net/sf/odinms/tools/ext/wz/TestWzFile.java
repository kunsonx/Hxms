/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools.ext.wz;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.odinms.provider.MapleSQLData;
import net.sf.odinms.client.skills.MapleSkillDatabase;
import net.sf.odinms.provider.MapleSkillInfo;
import net.sf.odinms.client.skills.SkillInfoManager;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataFileEntry;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.provider.wz.MapleDataType;
import net.sf.odinms.tools.StringUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author Admin
 */
public class TestWzFile {

	private static MapleDataProvider datasource = MapleDataProviderFactory
			.getDataProvider(new File(System
					.getProperty("net.sf.odinms.wzpath") + "/Skill.wz"));
	private static MapleData stringData__ = MapleDataProviderFactory
			.getDataProvider(
					new File(System.getProperty("net.sf.odinms.wzpath")
							+ "/String.wz")).getData("Skill.img");

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {

		Session session = DatabaseConnection.getSession();
		// 获得回话

		Transaction transaction = session.beginTransaction();

		session.createQuery("delete MapleSkillDatabase").executeUpdate();
		for (MapleSkillDatabase mapleSkillInfo : getSkillInfo()) {
			session.saveOrUpdate(mapleSkillInfo);
		}
		transaction.commit();
		// MapleSkillInfo info =
		// SkillInfoManager.getSkillInfoFromData((MapleSkillDatabase)
		// session.createQuery("from MapleSkillDatabase where id =:id").setParameter("id",
		// 1000).uniqueResult());
		session.close();
	}

	public static ArrayList<MapleSkillDatabase> getSkillInfo() {
		ArrayList<MapleSkillDatabase> infos = new ArrayList<MapleSkillDatabase>();
		Pattern pattern = Pattern.compile("([0-9]*).img");
		for (MapleDataFileEntry mapleDataFileEntry : datasource.getRoot()
				.getFiles()) {
			Matcher matcher = pattern.matcher(mapleDataFileEntry.getName());
			if (matcher.matches()) {
				System.out.println("处理文件：" + matcher.group(1));
				readWzSkillInfo(
						datasource.getData(mapleDataFileEntry.getName()),
						Integer.parseInt(matcher.group(1)), infos);
			}
		}
		return infos;
	}

	public static ArrayList<MapleSkillDatabase> readWzSkillInfo(MapleData data,
			int jobid, ArrayList<MapleSkillDatabase> infos) {
		MapleData skills = data.getChildByPath("skill");
		for (MapleData mapleData : skills.getChildren()) {
			MapleSkillInfo info = new MapleSkillInfo(Integer.parseInt(mapleData
					.getName()), getSkillName(Integer.parseInt(mapleData
					.getName())), jobid);

			System.out.println("解析数据：" + info);
			/*
			 * for (MapleData data1 : mapleData.getChildren()) { if
			 * (data1.getType().equals(MapleDataType.PROPERTY)) { MapleSkillData
			 * sdata = new MapleSkillData();//建立子节点对象
			 * info.addNote(sdata);//添加到父节点
			 * sdata.setName(data1.getName());//设置子节点节点名 readValues(sdata,
			 * data1);//读取字节所有字串符和int类型数值 System.out.println("子节点：" +
			 * data1.getName()); } } /
			 */
			readValues(info, mapleData);

			infos.add(SkillInfoManager.getDatabaseObj(info));
		}
		return infos;
	}

	public static String getSkillName(int id) {
		String strId = Integer.toString(id);
		strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
		MapleData skillroot = stringData__.getChildByPath(strId);
		if (skillroot != null) {
			return MapleDataTool
					.getString(skillroot.getChildByPath("name"), "");
		}
		return null;
	}

	public static void readValues(MapleSQLData info, MapleData data) {
		int animationTime = 0;
		for (MapleData cData : data.getChildren()) {
			if (cData.getType().equals(MapleDataType.INT)
					|| cData.getType().equals(MapleDataType.STRING)) {
				System.out.println(cData.getName() + ":"
						+ cData.getData().toString());
				info.addValues(cData.getName(), cData.getData().toString());
			} else if (cData.getType().equals(MapleDataType.VECTOR)) {
				Point point = (Point) cData.getData();
				info.addValues(cData.getName(), point.x + "/" + point.y);
			} else if (cData.getType().equals(MapleDataType.PROPERTY)) {
				MapleSQLData sdata = new MapleSQLData();// 建立子节点对象
				info.addNote(sdata);// 添加到父节点
				sdata.setName(cData.getName());// 设置子节点节点名
				readValues(sdata, cData);// 读取字节所有字串符和int类型数值
			}

			// 特殊统加处理
			if (data.getName().equals("effect")) {
				animationTime += MapleDataTool.getIntConvert("delay", cData, 0);
			}
		}

		if (data.getName().equals("effect")) {
			info.addValues("animationTime", String.valueOf(animationTime));
			System.out.println("animationTime:" + animationTime);
		}
	}
}
