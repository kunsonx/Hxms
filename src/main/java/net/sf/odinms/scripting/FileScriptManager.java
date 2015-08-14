/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.scripting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */
public class FileScriptManager {

	public static final class FileData {

		private static Logger log = Logger.getLogger(FileData.class);
		private byte[] data;
		private String path;
		private long lasttime;

		public FileData(String path) {
			this.path = path;
		}

		public void refresh() {
			File f = new File(path);
			if (f.exists()) {
				if (lasttime != f.lastModified()) {
					lasttime = f.lastModified();
					readFile(f);
				}
			} else {
				data = null;
				lasttime = -1;
			}
		}

		public void readFile(File f) {
			try {
				InputStream is = new FileInputStream(f);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int b;
				do {
					b = is.read();
					if (b != -1) {
						baos.write(b);
					}
				} while (b != -1);
				is.close();
				data = baos.toByteArray();
				baos.close();
			} catch (Exception e) {
				log.error("文件读取异常：", e);
			}
		}

		public Reader getReader() {
			refresh();
			if (data != null) {
				return new InputStreamReader(new ByteArrayInputStream(data));
			} else {
				return null;
			}
		}
	}

	private static final Map<String, FileData> data = new ConcurrentHashMap<String, FileScriptManager.FileData>();

	public static FileData getData(String str) {
		FileData f = data.get(str);
		if (f == null) {
			f = new FileData(str);
			data.put(str, f);
		}
		return f;
	}
}
