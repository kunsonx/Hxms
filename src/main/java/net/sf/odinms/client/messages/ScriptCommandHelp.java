/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client.messages;

import java.lang.reflect.Method;

/**
 * 脚本命令帮助器
 *
 * @author hxms
 */
public class ScriptCommandHelp {

	/**
	 * 把脚本数组转换成数组
	 *
	 * @param na
	 * @return
	 */
	public static Object[] toArray(Object na) throws Exception {
		if (na.getClass().getName().contains("NativeArray")) {
			Method method = na.getClass().getDeclaredMethod("getLength",
					(Class<?>[]) null);
			method.setAccessible(true);
			int length = Integer.parseInt(method.invoke(na, (Object[]) null)
					.toString());
			Object[] result = new Object[length];
			method = na
					.getClass()
					.getDeclaredMethod(
							"get",
							int.class,
							Class.forName("sun.org.mozilla.javascript.internal.Scriptable"));
			// NativeJavaObject unwrap
			for (int i = 0; i < result.length; i++) {

				result[i] = method.invoke(na, i, na);
				if (result[i].getClass().getName().contains("NativeJavaObject")) {
					Method m = result[i].getClass().getMethod("unwrap",
							(Class<?>[]) null);
					result[i] = m.invoke(result[i], (Object[]) null);
				}
			}
			return result;
		}
		return null;
	}
}
