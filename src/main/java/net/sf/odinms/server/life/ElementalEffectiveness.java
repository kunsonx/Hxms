package net.sf.odinms.server.life;

public enum ElementalEffectiveness {

	NORMAL, IMMUNE, STRONG, WEAK;

	public static ElementalEffectiveness getByNumber(int num) {
		switch (num) {
		case 1:
			return IMMUNE; // 完全免疫
		case 2:
			return STRONG; // 高抗
		case 3:
			return WEAK; // 弱
		case 4:
			return NORMAL; // 普通[不抗也不弱]
		default:
			throw new IllegalArgumentException("不知道的怪物抗性程度: " + num);
		}
	}

	public int getNumber() {
		switch (this) {
		case IMMUNE:
			return 1; // 完全免疫
		case STRONG:
			return 2; // 高抗
		case WEAK:
			return 3; // 弱
		case NORMAL:
			return 4; // 普通[不抗也不弱]
		default:
			throw new IllegalArgumentException("不知道的怪物抗性程度: " + this);
		}
	}
}