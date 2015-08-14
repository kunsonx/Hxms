/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.www.WebUser;
import net.sf.odinms.net.channel.www.WebUserPlayer;
import net.sf.odinms.server.MapleStatEffect;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */
public class UserServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(UserServlet.class);

	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 *
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		try {
			/* TODO output your page here. You may use following sample code. */
			out.println("<!DOCTYPE html>");
			out.println("<html>");
			out.println("<head>");
			out.println("<title>Servlet UserServlet</title>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h1>Servlet UserServlet at "
					+ request.getContextPath() + "</h1>");
			out.println("</body>");
			out.println("</html>");
		} finally {
			out.close();
		}
	}

	// <editor-fold defaultstate="collapsed"
	// desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			response.setContentType("text/html;charset=UTF-8");
			WebUser user = (WebUser) request.getSession().getAttribute(
					WebUser.SESSION_KEY);
			if (user != null) {
				String op = request.getParameter("op");
				handlerAction(op, request.getParameter("name"), user, response);
				if (op != null && op.equals("zhuxiao")) {
					request.getSession().invalidate();
				}
			} else {
				writeData(response, "null");
			}
		} catch (Exception e) {
			log.info("USERSERVLET ERROR:", e);
		}
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Short description";
	}// </editor-fold>

	public void handlerAction(String op, String name, WebUser user,
			HttpServletResponse response) throws IOException {
		if (op != null && op.equals("playerlist")) {
			List<WebUserPlayer> list = user.getPlayers();
			JSONArray jsonObject = JSONArray.fromObject(list);
			writeData(response, jsonObject.toString());
		} else if (op != null && op.equals("clearSkill")) {
			int id = MapleCharacter.getIdByName(name);
			String ret;
			if (id == -1) {
				ret = "没有找到该角色";
			} else {
				MapleCharacter chr = ChannelServer
						.getCharacterFromAllServersAndWorld(id);
				if (chr != null) {
					chr.ClearAllSkills();
				} else {
					deleteSkillInfo(id);
				}
				ret = "操作成功完成";
			}
			writeData(response, ret);
		} else if (op != null && op.equals("dc")) {
			for (WebUserPlayer up : user.getPlayers()) {
				MapleCharacter chr = ChannelServer
						.getCharacterFromAllServersAndWorld(up.getId());
				if (chr != null) {
					chr.Dci();
				}
				// updateofflineStats(up.getId());

			}
			updateofflineStats(user.getId(), 0);

			writeData(response, "操作成功完成");
		} else if (op != null && op.equals("cshzx")) {
			int id = MapleCharacter.getIdByName(name);
			String ret;
			if (id == -1) {
				ret = "没有找到该角色";
			} else {
				MapleCharacter chr = ChannelServer
						.getCharacterFromAllServersAndWorld(id);
				if (chr != null) {
					int ged = chr.getGender();
					int face = ged == 0 ? 20000 : 21000;
					int hair = ged == 0 ? 30000 : 37290;
					chr.setFace(face);
					chr.setHair(hair);
					chr.updateSingleStat(MapleStat.FACE, face);
					chr.updateSingleStat(MapleStat.HAIR, hair);
					chr.equipChanged();
				} else {
					updateFaceAndHair(id);
				}
				ret = "操作成功完成";
			}
			writeData(response, ret);
		} else if (op != null && op.equals("warptodef")) {
			int id = MapleCharacter.getIdByName(name);
			String ret;
			if (id == -1) {
				ret = "没有找到该角色";
			} else {
				MapleCharacter chr = ChannelServer
						.getCharacterFromAllServersAndWorld(id);
				ret = "操作成功完成";
				if (chr != null) {
					if (chr.getEventInstance() != null) {
						ret = "您正在进行任务事件,不允许此操作.";
					} else {
						chr.changeMap(chr.getClient().getChannelServer()
								.getMapFactory().getMap(910000018));
					}
				} else {
					updateMap(id);
				}
			}
			writeData(response, ret);
		} else if (op != null && op.equals("changejobtodef")) {
			int id = MapleCharacter.getIdByName(name);
			String ret;
			if (id == -1) {
				ret = "没有找到该角色";
			} else {
				MapleCharacter chr = ChannelServer
						.getCharacterFromAllServersAndWorld(id);
				ret = "操作成功完成";
				if (chr != null) {
					chr.ClearAllSkills();
					chr.changeJob(MapleJob.BEGINNER);
				} else {
					updateJob(id);
					deleteSkillInfo(id);
				}
			}
			writeData(response, ret);
		}
	}

	public void deleteSkillInfo(int chrId) {
		try {
			ArrayList<Integer> list = new ArrayList<Integer>();
			Connection con = DatabaseConnection.getConnection();
			java.sql.PreparedStatement ps = con
					.prepareStatement("SELECT skillid FROM skills WHERE characterid = ?");
			ps.setInt(1, chrId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(1);
				if (!MapleStatEffect.isMonsterRiding(id)) {
					list.add(id);
				}
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("delete from skills where characterid = ? and skillid = ?");
			ps.setInt(1, chrId);
			for (Integer integer : list) {
				ps.setInt(2, integer);
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			log.error("DELETE FALID:", ex);
		}
	}

	public void updateofflineStats(int aid, int chrId) {
		try {
			Connection con = DatabaseConnection.getConnection();

			java.sql.PreparedStatement ps = con
					.prepareStatement("update accounts set loggedin = 0 where id = ?");
			ps.setInt(1, aid);
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("update characters set loggedin = 0 where accountid = ?");
			ps.setInt(1, aid);
			ps.executeUpdate();
			ps.close();

			con.close();
		} catch (SQLException ex) {
			log.error("DELETE FALID:", ex);
		}
	}

	public void updateofflineStats(int chrId) {
		try {
			Connection con = DatabaseConnection.getConnection();
			java.sql.PreparedStatement ps = con
					.prepareStatement("SELECT accountid FROM characters WHERE id = ?");
			ps.setInt(1, chrId);
			ResultSet rs = ps.executeQuery();
			rs.next();
			int aid = rs.getInt(1);
			rs.close();
			ps.close();

			ps = con.prepareStatement("update accounts set loggedin = 0 where id = ?");
			ps.setInt(1, aid);
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("update characters set loggedin = 0 where accountid = ?");
			ps.setInt(1, aid);
			ps.executeUpdate();
			ps.close();

			con.close();

		} catch (SQLException ex) {
			log.error("DELETE FALID:", ex);
		}
	}

	public void updateFaceAndHair(int chrId) {
		try {
			Connection con = DatabaseConnection.getConnection();
			java.sql.PreparedStatement ps = con
					.prepareStatement("update characters set face = (case WHEN gender = 0 then 20000 else 21000 end), hair = (case WHEN gender = 0 then 30000 else 37290 end) WHERE id = ?");
			ps.setInt(1, chrId);
			ps.executeUpdate();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			log.error("DELETE FALID:", ex);
		}
	}

	public void updateMap(int chrId) {
		try {
			Connection con = DatabaseConnection.getConnection();
			java.sql.PreparedStatement ps = con
					.prepareStatement("update characters set map = 910000020 WHERE id = ?");
			ps.setInt(1, chrId);
			ps.executeUpdate();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			log.error("DELETE FALID:", ex);
		}
	}

	public void updateJob(int chrId) {
		try {
			Connection con = DatabaseConnection.getConnection();
			java.sql.PreparedStatement ps = con
					.prepareStatement("update characters set job = 0 WHERE id = ?");
			ps.setInt(1, chrId);
			ps.executeUpdate();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			log.error("DELETE FALID:", ex);
		}
	}

	private void writeData(HttpServletResponse response, String ret)
			throws IOException {
		response.getWriter().print(ret);
		response.getWriter().close();
	}
}
