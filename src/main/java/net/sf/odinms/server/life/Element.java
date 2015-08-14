package net.sf.odinms.server.life;

public enum Element {

    PHYSICAL, FIRE, ICE, LIGHTING, POISON, HOLY, DARK;

    public static Element getFromChar(char c) {
        switch (Character.toUpperCase(c)) {
            case 'F':
                return FIRE;
            case 'I':
                return ICE;
            case 'L':
                return LIGHTING;
            case 'S':
                return POISON;
            case 'H':
                return HOLY;
            case 'P':
                return PHYSICAL;//普通
            case 'D':
                return DARK; //暗
        }
        throw new IllegalArgumentException("unknown elemnt char " + c);
    }

    public char getChar() {
        switch (this) {
            case FIRE:
                return 'F';
            case ICE:
                return 'I';
            case LIGHTING:
                return 'L';
            case POISON:
                return 'S';
            case HOLY:
                return 'H';
            case PHYSICAL:
                return 'P';//普通
            case DARK:
                return 'D'; //暗
        }
        throw new IllegalArgumentException("unknown elemnt char ");
    }
}