/*
 * 处理机械师的传送门
 */
package net.sf.odinms.server.maps;

import java.awt.Point;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author 岚殇
 */
public class MapleDoor2 extends AbstractMapleMapObject {

    private MapleCharacter owner;
    private MapleMap target; //门所在地图
    private Point targetPosition; //门所在坐标
    private boolean isFirst = false;

    public MapleDoor2(MapleCharacter owner, Point targetPosition, boolean isFirst) {
        super();
        this.owner = owner;
        this.target = owner.getMap();
        this.targetPosition = targetPosition;
        setPosition(this.targetPosition);
        this.isFirst = isFirst; //是否是第一个召唤出来的门
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.机械传送门;
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public MapleMap getTarget() {
        return target;
    }

    public Point getTargetPosition() {
        return targetPosition;
    }

    public boolean isFirst() {
        return isFirst;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (target.getId() == client.getPlayer().getMapId() || owner == client.getPlayer()) {
            client.getSession().write(MaplePacketCreator.传送门的效果(owner.getId(), targetPosition, isFirst));
            client.getSession().write(MaplePacketCreator.传送门的传送点(targetPosition));
            client.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(makeDestroyData(client));
    }

    public MaplePacket makeDestroyData(MapleClient client) {
        return MaplePacketCreator.取消传送门(owner.getId(), isFirst);
    }
}
