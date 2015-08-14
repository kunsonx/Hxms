/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

import java.rmi.RemoteException;
import java.sql.SQLException;
import org.apache.log4j.Logger;

/**
 * 异常输出打印
 *
 * @author hxms
 */
public class ServerExceptionHandler {

	private static Logger log = Logger.getLogger(ServerExceptionHandler.class);

	public static void HandlerRemoteException(RemoteException exception) {
		log.error("异常类型 RemoteException：", exception);
	}

	public static void HandlerSqlException(SQLException exception) {
		log.error("异常类型 SQLException：", exception);
	}

	public static void HandlerException(Exception exception) {
		log.error("异常类型 " + exception.getClass().getName() + "：", exception);
	}
}
