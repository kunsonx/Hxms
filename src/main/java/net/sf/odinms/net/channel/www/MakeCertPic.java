/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 *
 * @author Admin
 */
public class MakeCertPic {
	// 验证码图片中可以出现的字符集，可以根据需要修改

	private Random rand = new Random();
	private Font font = new Font("Atlantic Inline", Font.PLAIN, 18);
	private char mapTable[] = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
			'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

	/*
	 * 功能：生成彩色验证码图片 参数wedth为生成图片的宽度，参数height为生成图片的高度，参数os为页面的输出流
	 */

	public String getCertPic(int width, int height, OutputStream os) {
		if (width <= 0) {
			width = 60;
		}
		if (height <= 0) {
			height = 20;
		}
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		// 获取图形上下文
		Graphics2D g = (Graphics2D) image.getGraphics();
		// 设定背景颜色
		g.setColor(new Color(0xDCDCDC));
		g.fillRect(0, 0, width, height);
		// 画边框
		g.setColor(getRandColor());
		g.drawRect(0, 0, width - 1, height - 1);
		// 随机产生的验证码
		String strEnsure = "";
		// 4代表4为验证码，如果要产生更多位的验证码，则加大数值
		for (int i = 0; i < 4; ++i) {
			strEnsure += mapTable[(int) (mapTable.length * Math.random())];
		}
		// 将认证码显示到图像中，如果要生成更多位的验证码，增加drawString语句
		g.setColor(Color.black);
		g.setFont(font);

		String str = strEnsure.substring(0, 1);
		g.drawString(str, 8, 17);
		str = strEnsure.substring(1, 2);
		g.drawString(str, 20, 15);
		str = strEnsure.substring(2, 3);
		g.drawString(str, 35, 18);
		str = strEnsure.substring(3, 4);
		g.drawString(str, 45, 15);
		// 随机产生15个干扰点
		for (int i = 0; i < 10; i++) {
			g.setColor(getRandColor());
			int x = rand.nextInt(width);
			int y = rand.nextInt(height);
			g.drawOval(x, y, 1, 1);
		}
		for (int i = 0; i < 5; i++) {
			g.setColor(getRandColor());
			int x = rand.nextInt(width);
			int y = rand.nextInt(height);
			int xl = rand.nextInt(12);
			int yl = rand.nextInt(12);
			g.drawLine(x, y, x + xl, y + yl);
		}
		// 释放图形上下文
		g.dispose();
		try {
			// 输出图形到页面
			ImageIO.write(image, "JPEG", os);
		} catch (IOException e) {
			return "";
		}
		return strEnsure;
	}

	public Color getRandColor() {
		switch (rand.nextInt(9)) {
		case 0:
			return Color.BLUE;
		case 1:
			return Color.CYAN;
		case 2:
			return Color.DARK_GRAY;
		case 3:
			return Color.GRAY;
		case 4:
			return Color.LIGHT_GRAY;
		case 5:
			return Color.MAGENTA;
		case 6:
			return Color.PINK;
		case 7:
			return Color.RED;
		case 8:
			return Color.YELLOW;
		default:
			return Color.BLACK;
		}
	}
}