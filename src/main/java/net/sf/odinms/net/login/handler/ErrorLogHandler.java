/*
 错误信息
 */
package net.sf.odinms.net.login.handler;

import java.util.Date;
import net.sf.odinms.client.GameConstants;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.tools.WriteToFile;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ErrorLogHandler implements MaplePacketHandler {

	@Override
	public boolean validateState(MapleClient c) {
		return !c.isLoggedIn();
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		String error = slea.readMapleAsciiString();
		WriteToFile wt = new WriteToFile("客户端错误信息.txt");

		wt.WriteFile("记录时间：" + GameConstants.getFormatter().format(new Date())
				+ "\r\n" + error.toUpperCase());
		wt.CloseFile();
	}
}