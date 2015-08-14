package net.sf.odinms.client;

import net.sf.odinms.net.LongValueHolder;

public enum MapleDisease implements LongValueHolder {

	// 这里是怪给人物的buff
	SLOW(0x1), // 缓慢-1
	人变花蘑菇(0x02), // -2
	UNK(0x200), SEDUCE(0x80), // 诱惑-7
	FISHABLE(0x100), // 钓鱼 - 无效
	CURSE(0x200), // 诅咒
	CONFUSE(0x80000), // 诱惑
	STUN(0x2000000000000L), // 眩晕
	POISON(0x4000000000000L), // 中毒
	SEAL(0x8000000000000L), // 封印
	DARKNESS(0x10000000000000L), // 黑暗
	WEAKEN(0x4000000000000000L);// 虚弱[不能跳]
	private long i;

	private MapleDisease(long i) {
		this.i = i;
	}

	@Override
	public long getValue() {
		return i;
	}
}