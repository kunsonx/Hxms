/*
 * 为显示封包准备的读写文件，可以方便的将封包以文本的形式写在电脑硬盘里
 */
package net.sf.odinms.tools;

import java.util.*;
import java.io.*;

/**
 *
 * @author icelemon
 */
public class WriteToFile {

	File fc;
	FileWriter fw;
	BufferedWriter bw;
	PrintWriter pw;

	public WriteToFile(String file) {
		try {
			fc = new File(file);
			if (!fc.exists()) {
				fc.createNewFile();
			}
			fw = new FileWriter(fc, true);
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void WriteFile(String s1) {
		try {
			fw.write(s1);
			fw.write("\r\n");
			CloseFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void CloseFile() {
		try {
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
