/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.servlet;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.sf.odinms.client.SystemVerification;
import net.sf.odinms.net.MapleServerHandler;
import net.sf.odinms.net.RecvPacketOpcode;
import net.sf.odinms.net.SendPacketOpcode;
import org.apache.log4j.Logger;

/**
 * 标准服务接口
 *
 * @author hxms 架构
 */
public abstract class GeneralServer {

	// 许可序列
	private static final List<String> permit = Arrays.asList(
			"C64C91EAE8ED97D0",// 自己
			"42C7137436868944",// 乐章测试服
			"72895DB40EF44D30",// 飘雪
			"D826F01C24AC84A0",// 倾萌
			"9774F168AD7BECA0",// 蓝天冒险岛
			"5B21D30715A80410",// 东方冒险岛
			"FD9679F72AED77E0",// 脑子有病
			"3747F28BCD698A90", // 老油条
			"564EC267FB22FA50",// 完美君???
			"508BCAA72BE33850",// 波波冒险岛
			"D2A6141B6A467F40",// 木九十冒险岛x64
			"FB00D207FABF4420",// 奾狸儿冒险岛
			"F18208B58F282AD0"// 章鱼老客户
	/*
	 * "FA661035A9980D60"// MP3跳槽客户 , "91DB469CAE929070"//芒果冒险岛
	 */);
	// 许可序列
	private static final Map<String, Integer> permit_version = java.util.Collections
			.singletonMap("CE5A6B14D70BECA8", 110);// 章鱼冒险岛
	public static final List<String> temporary_permit = Arrays.asList();
	// 配置文件名
	protected static String _ConfigName = "HxmsConfig.xml";
	// 日志程序
	private static final Logger log = Logger.getLogger(GeneralServer.class);
	private static final SystemVerification systemVerification = new SystemVerification();
	protected GeneralServerConfig _Config;

	static {
		systemVerification.init();

		SendPacketOpcode.values();
		RecvPacketOpcode.values();
	}

	public GeneralServer(GeneralServerConfig _ServerConfig) {
		this._Config = _ServerConfig;
		getConfig().initConfig(_ConfigName, this);
	}

	public abstract GeneralServerType getServerType();

	public abstract GeneralServerConfig getConfig();

	private static boolean CheckTime() {
		try {
			Date date = systemVerification.getCurrentDate();
			if ((date.getYear() + 1900) == 2013 && date.getMonth() == 7) {
				return permit.contains(systemVerification.GetComputerCode());
			} else {
				log.error("时间效验错误。文件已超时。");
			}
		} catch (Exception e) {
			log.error("尝试验证文件失败！", e);
		}
		return false;
	}

	private static boolean CheckFileVersion() {
		try {
			String vcode = systemVerification.GetComputerCode();
			for (String string : permit_version.keySet()) {
				if (string.equals(vcode)
						&& permit_version.get(vcode) == MapleServerHandler.MAPLE_VERSION) {
					return true;
				}
			}
		} catch (Exception e) {
			log.error("尝试验证文件失败！", e);
		}
		return false;
	}

	private static boolean CheckTempTime() {
		try {
			Date date = systemVerification.getCurrentDate();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			if ((date.getYear() + 1900) == 2013 && date.getMonth() == 4
					&& calendar.get(Calendar.DAY_OF_MONTH) <= 13) {
				return temporary_permit.contains(systemVerification
						.GetComputerCode());
			}
		} catch (Exception e) {
			log.error("尝试验证文件失败！", e);
		}
		return false;
	}

	protected static boolean CheckFilePermit() {
		if (!(CheckTime() || CheckTempTime() || CheckFileVersion())) {
			log.error("机器码验证失败......！");
			System.exit(0);
			return true;
		}
		return false;
	}

	public static boolean is64() {
		return System.getProperties().get("sun.arch.data.model").equals("64");
	}

	public int getSimpleIoProcessorPoolThread() {
		switch (getServerType()) {
		case LOGIN:
			return is64() ? Runtime.getRuntime().availableProcessors() * 10
					: Runtime.getRuntime().availableProcessors() * 2;
		case CHANNEL:
			return is64() ? Runtime.getRuntime().availableProcessors() * 10
					: Runtime.getRuntime().availableProcessors() * 2;
		}
		return 0;
	}

	public int getMaxiNumPoolSize() {
		switch (getServerType()) {
		case LOGIN:
			return is64() ? Integer.MAX_VALUE : 60;
		case CHANNEL:
			return is64() ? Integer.MAX_VALUE : 100;
		}
		return 0;
	}

	public static int getMaxiNumPoolSize(GeneralServerType type) {
		switch (type) {
		case LOGIN:
			return is64() ? Integer.MAX_VALUE : 60;
		case CHANNEL:
			return is64() ? Integer.MAX_VALUE : 100;
		}
		return 0;
	}
}
