/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

import javax.script.*;
import org.apache.log4j.Logger;

/**
 *
 * @author HXMS
 */
public class ScriptEngineEval {

	private interface ScriptMain {

		public int get();
	}

	private final static Logger log = Logger.getLogger(ScriptEngineEval.class);
	private final static ScriptEngineManager sem = new ScriptEngineManager();
	private final static Compilable compEngine = (Compilable) sem
			.getEngineByName("javascript");
	private static String basescript = "var x = %d;" + "function u(i){"
			+ "return Math.ceil(i);" + "} " + "" + "function d(i){"
			+ "return Math.floor(i);" + "}" + "" + "function get()" + "{"
			+ "return %s;" + "}" + "get();";

	public static int Eval(int x, String exp, int defvalue, int sourceid) {
		if (exp == null || exp.length() == 0) {
			return defvalue;
		}
		try {
			CompiledScript script = compEngine.compile(String.format(
					basescript, x, exp.replace("y", "0")));// .replace("=", "")
			return ((Double) Double.parseDouble(script.eval().toString()))
					.intValue();
		} catch (ScriptException ex) {
			log.info(sourceid + " 计算脚本错误：\n" + exp, ex);
		}
		return defvalue;
	}
}
