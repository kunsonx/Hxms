/*
 角色动作信息
 */
package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.sf.odinms.client.GameConstants;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.AnimatedMapleMapObject;
import net.sf.odinms.server.movement.*;
import net.sf.odinms.tools.WriteToFile;
import net.sf.odinms.tools.data.input.LittleEndianAccessor;
import net.sf.odinms.tools.data.output.LittleEndianWriter;

public abstract class MovementParse extends AbstractMaplePacketHandler {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(MovementParse.class);

	public static List<LifeMovementFragment> parseMovement(
			final LittleEndianAccessor lea) {
		final List<LifeMovementFragment> res = new ArrayList<LifeMovementFragment>();
		int numCommands = lea.readByte();
		numCommands = numCommands >= 0 ? numCommands : numCommands + 256;
		for (int i = 0; i < numCommands; i++) {
			final int command = lea.readByte();
			switch (command) {
			case 17: // Float
			case 0: // normal move
			case 8: // assassinate
			case 7:
			case 16: {
				final short xpos = lea.readShort();
				final short ypos = lea.readShort();
				final short xwobble = lea.readShort();
				final short ywobble = lea.readShort();
				final short unk = lea.readShort();
				final short xoffset = lea.readShort();
				final short yoffset = lea.readShort();
				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final AbsoluteLifeMovement alm = new AbsoluteLifeMovement(
						command, new Point(xpos, ypos), duration, newstate);
				alm.setUnk(unk);
				alm.setPixelsPerSecond(new Point(xwobble, ywobble));
				alm.setOffset(new Point(xoffset, yoffset));
				res.add(alm);
				break;
			}
			case -1:
			case 1:
			case 2:
			case 21: { // Float
				final short xmod = lea.readShort();
				final short ymod = lea.readShort();
				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final RelativeLifeMovement rlm = new RelativeLifeMovement(
						command, new Point(xmod, ymod), duration, newstate);
				res.add(rlm);
				break;
			}
			case 4: // tele... -.-
			case 6: // assaulter
			{
				final short xpos = lea.readShort();
				final short ypos = lea.readShort();
				final short xwobble = lea.readShort();
				final short ywobble = lea.readShort();
				final byte newstate = lea.readByte();
				final TeleportMovement tm = new TeleportMovement(command,
						new Point(xpos, ypos), newstate);
				tm.setPixelsPerSecond(new Point(xwobble, ywobble));
				res.add(tm);
				break;
			}
			case 12: // chair ???
			case -112: {// change equip ???
				res.add(new ChangeEquipSpecialAwesome(command, lea.readByte()));
				// i--;
				break;
			}
			case 3:
			case 5:
				// case 7: //same structure
			case 9:
			case 10:
			case 11:
			case 13: // Shot-jump-back thing
			case 14:

			case 19:
			case 20: {
				final short xpos = lea.readShort();
				final short ypos = lea.readShort();
				final short unk = lea.readShort();
				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final ChairMovement cm = new ChairMovement(command, new Point(
						xpos, ypos), duration, newstate);
				cm.setUnk(unk);
				res.add(cm);
				break;
			}
			case 18:
			case 23: // ?
			case 24: // ?
			case 25: // ?
			case 26: // ?
			case 27: // ? <- has no offsets
			case 22:
			case 40:
			case 41:
			case 42: {
				final byte newstate = lea.readByte();
				final short unk = lea.readShort();
				final AranMovement am = new AranMovement(command, new Point(0,
						0), unk, newstate);
				res.add(am);
				break;
			}
			case 15:// 轻羽鞋
			{ // Jump Down
				final short xpos = lea.readShort();
				final short ypos = lea.readShort();
				final short xwobble = lea.readShort();
				final short ywobble = lea.readShort();
				final short unk = lea.readShort();
				final short fh = lea.readShort();
				final short xoffset = lea.readShort();
				final short yoffset = lea.readShort();
				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final JumpDownMovement jdm = new JumpDownMovement(command,
						new Point(xpos, ypos), duration, newstate);
				jdm.setUnk(unk);
				jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
				jdm.setOffset(new Point(xoffset, yoffset));
				jdm.setFH(fh);

				res.add(jdm);
				break;
			}/*
			 * { //?... 00 00 7A 03 0F 02 64 01 00 00 0F 00 04 5A 00 final short
			 * unk = lea.readShort(); //always 0? final short xpos =
			 * lea.readShort(); //not xpos final short ypos = lea.readShort();
			 * //not ypos final short xwobble = lea.readShort(); final short
			 * ywobble = lea.readShort(); final short fh = lea.readShort();
			 * final byte newstate = lea.readByte(); final short duration =
			 * lea.readShort(); final UnknownMovement um = new
			 * UnknownMovement(command, new Point(xpos, ypos), duration,
			 * newstate); um.setUnk(unk); um.setPixelsPerSecond(new
			 * Point(xwobble, ywobble)); um.setFH(fh); res.add(um); break; }
			 */
			/*
			 * case 20: { //fj final short xpos = lea.readShort(); final short
			 * ypos = lea.readShort(); final short xwobble = lea.readShort();
			 * final short ywobble = lea.readShort(); final byte newstate =
			 * lea.readByte(); final short duration = lea.readShort(); final
			 * FlashMovement um = new FlashMovement(command, new Point(xpos,
			 * ypos), duration, newstate); um.setPixelsPerSecond(new
			 * Point(xwobble, ywobble)); res.add(um); break; }
			 */
			case 28:
			case 29:
			case 30:
			case 31:
			case 32:
			case 33:
			case 34:
			case 35:
			case 36:
			case 37:
			case 38:
			case 39: {
				res.add(new RefreshLifeMovement(command, lea.readByte(), lea
						.readShort()));
				break;
			}
			case -111: // rush ?
			{
				final byte unk = lea.readByte();
				UnknownMovementAction um = new UnknownMovementAction(
						new UnknownMovementSerialize() {
							@Override
							public void Serialize(LittleEndianWriter lew) {
								lew.write(command);
								lew.write(unk);
							}
						});
				res.add(um);
				break;
			}
			default:
				WriteToFile re = new WriteToFile("未知的移动类型.txt");

				re.WriteFile(GameConstants.getFormatter().format(new Date())
						+ " Remaining : " + (numCommands - res.size())
						+ " New type of movement ID : " + command
						+ ", packet : " + lea.toString());
				re.CloseFile();
				return res;
			}
		}
		return res;
	}

	protected static void updatePosition(List<LifeMovementFragment> movement,
			AnimatedMapleMapObject target, int yoffset) {
		for (LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					Point position = ((LifeMovement) move).getPosition();
					position.y += yoffset;
					target.setPosition(position);
				}
				target.setStance(((LifeMovement) move).getNewstate());
			}
		}
	}
}
