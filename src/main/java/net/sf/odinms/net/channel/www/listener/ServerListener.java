/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import net.sf.odinms.net.channel.www.info.WebRankingInfo;

/**
 *
 * @author Administrator
 */
public class ServerListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		sce.getServletContext().setAttribute(WebRankingInfo.KEY,
				new WebRankingInfo());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}
