/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.tools;

/**
 * 服务器公告类型。[0 - 弹窗] [1 - 蓝色条] [2 - 红喇叭] [3 - 高质量喇叭] [5 - 系统公告] [8-9 - 道具喇叭] [0xf
 * - 蛋糕喇叭] [0x0a - 缤纷喇叭] [0x10 - 馅饼喇叭] [0x11 - 心脏高级喇叭] [0x12 - 白骨高级喇叭] [0x13 -
 * 红色抽奖公告(迷你蛋)] [0x14 - 绿色抽奖公告] [0x16 - 服务器公告]
 */
public enum PhoneType {

	弹窗(0), 蓝色条(1), 红喇叭(2), 高质量喇叭(3), 系统公告_篮字(4), 系统公告_红字(5), 道具喇叭(8), 缤纷喇叭(0xA), 蛋糕喇叭(
			0x13), 馅饼喇叭(0x14), 心脏高级喇叭(0x15), 白骨高级喇叭(0x16), 红色抽奖公告(0x1C), 绿色抽奖公告(
			0x1D), ;
	private final int i;

	PhoneType(int i) {
		this.i = i;
	}

	public boolean writeChannel() {
		switch (this) {
		case 高质量喇叭:
		case 蛋糕喇叭:
		case 馅饼喇叭:
		case 心脏高级喇叭:
		case 白骨高级喇叭:
		case 绿色抽奖公告:
			return true;
		default:
			return false;
		}
	}

	public int getValue() {
		return i;
	}

	public static PhoneType get(int value) {
		for (PhoneType phoneType : values()) {
			if (phoneType.i == value) {
				return phoneType;
			}
		}
		return null;
	}
}
