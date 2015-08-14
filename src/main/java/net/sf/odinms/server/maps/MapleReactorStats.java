package net.sf.odinms.server.maps;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import net.sf.odinms.tools.Pair;

/**
 * @author Lerk
 */
public class MapleReactorStats {

    private int id;
    private Point tl;
    private Point br;
    private Map<Byte, StateData> stateInfo = new HashMap<Byte, StateData>();

    public MapleReactorStats() {
    }

    public MapleReactorStats(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Point getTl() {
        return tl;
    }

    public Point getBr() {
        return br;
    }

    public Map<Byte, StateData> getStateInfo() {
        return stateInfo;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTl(Point tl) {
        this.tl = tl;
    }

    public void setBr(Point br) {
        this.br = br;
    }

    public void setStateInfo(Map<Byte, StateData> stateInfo) {
        this.stateInfo = stateInfo;
    }

    public void setTL(Point tl) {
        this.tl = tl;
    }

    public void setBR(Point br) {
        this.br = br;
    }

    public Point getTL() {
        return tl;
    }

    public Point getBR() {
        return br;
    }

    public void addState(byte state, int type, Pair<Integer, Integer> reactItem, byte nextState) {
        stateInfo.put(state, new StateData(type, reactItem, nextState));
    }

    public byte getNextState(byte state) {
        StateData nextState = stateInfo.get(state);
        if (nextState != null) {
            return nextState.getNextState();
        } else {
            return -1;
        }
    }

    public int getType(byte state) {
        StateData nextState = stateInfo.get(state);
        if (nextState != null) {
            return nextState.getType();
        } else {
            return -1;
        }
    }

    public Pair<Integer, Integer> getReactItem(byte state) {
        StateData nextState = stateInfo.get(state);
        if (nextState != null) {
            return nextState.getReactItem();
        } else {
            return null;
        }
    }

    public static class StateData {

        private long id;
        private int type;
        private Pair<Integer, Integer> reactItem;
        private byte nextState;

        public StateData() {
        }

        private StateData(int type, Pair<Integer, Integer> reactItem, byte nextState) {
            this.type = type;
            this.reactItem = reactItem;
            this.nextState = nextState;
        }

        private int getType() {
            return type;
        }

        private byte getNextState() {
            return nextState;
        }

        private Pair<Integer, Integer> getReactItem() {
            return reactItem;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public void setType(int type) {
            this.type = type;
        }

        public void setReactItem(Pair<Integer, Integer> reactItem) {
            this.reactItem = reactItem;
        }

        public void setNextState(byte nextState) {
            this.nextState = nextState;
        }

        public void setReactItems(String s) {
            if (!s.isEmpty()) {
                String[] ss = s.split("/");
                reactItem = new Pair<Integer, Integer>(Integer.parseInt(ss[0]), Integer.parseInt(ss[1]));
            }
        }

        public String getReactItems() {
            if (reactItem != null) {
                return reactItem.getLeft() + "/" + reactItem.getRight();
            } else {
                return "";
            }
        }
    }
}
