/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client.messages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import net.sf.odinms.client.MapleClient;
import org.apache.log4j.Logger;

/**
 * 脚本命令管理器
 *
 * @author hxms
 */
public final class ScriptCommand implements Command {

	private static final Logger log = Logger.getLogger(ScriptCommand.class);
	private static ScriptEngineFactory sef = new ScriptEngineManager()
			.getEngineByName("javascript").getFactory();
	private ScriptEngine engine;
	private CompiledScript compiledScript;
	private Invocable invocable;
	private String stratwith = "";
	private List<String> commands = new ArrayList<String>();
	private List<CommandDefinition> commandDefinitions = new ArrayList<CommandDefinition>();

	private ScriptCommand(ScriptEngine engine, CompiledScript compiledScript)
			throws ScriptException {
		this.engine = engine;
		this.compiledScript = compiledScript;
		this.invocable = (Invocable) engine;
		this.engine.put("command", this);
		compiledScript.eval();
		readKey();
	}

	public static ScriptCommand getScriptCommand(File file)
			throws FileNotFoundException, ScriptException, IOException {
		FileReader fr = new FileReader(file);
		ScriptEngine engine = sef.getScriptEngine();
		CompiledScript compiled = ((Compilable) engine).compile(fr);
		fr.close();
		return new ScriptCommand(engine, compiled);
	}

	/**
	 * 注册命令
	 *
	 * @param name
	 *            命令名
	 * @return
	 */
	public CommandDefinition registry(String name) {
		return registry(name, 0);
	}

	private void readKey() {
		if (stratwith.isEmpty()) {
			Object str = engine.get("commandChar");
			if (str != null) {
				stratwith = str.toString();
			}
		}
	}

	/**
	 * 注册命令
	 *
	 * @param name
	 *            命令名
	 * @param gmLevel
	 *            gm等级
	 * @return
	 */
	public CommandDefinition registry(String name, int gmLevel) {
		readKey();
		CommandDefinition result = new CommandDefinition(name, gmLevel);
		commandDefinitions.add(result);
		commands.add(stratwith + name);
		return result;
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return commandDefinitions.toArray(new CommandDefinition[0]);
	}

	@Override
	public void execute(MapleClient c, MessageCallback mc, String[] splittedLine)
			throws Exception, IllegalCommandSyntaxException {
		if (commands.contains(splittedLine[0])) {
			try {
				invocable.invokeFunction(splittedLine[0].substring(1), c, mc,
						splittedLine);
			} catch (ScriptException e) {
				log.error("命令脚本执行错误：", e);
			} catch (NoSuchMethodException e) {
				log.error("没有在脚本中搜索到方法名：" + splittedLine[0].substring(1), e);
			}
		}
	}
}
