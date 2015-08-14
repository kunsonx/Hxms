/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.servlet;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author hxms 架构
 */
public class GeneralServerTools {

	// 日志程序
	private static Logger log = Logger.getLogger(GeneralServerTools.class);
	// DOM程序
	private static DocumentBuilderFactory dbf = DocumentBuilderFactory
			.newInstance();
	// 成员变量
	private DocumentBuilder _DocumentBuilder;

	public GeneralServerTools() {
		try {
			_DocumentBuilder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			log.error("构造配置文件读取器失败！", ex);
		}
	}

	public File getFile(String file) {
		File _file = new File(file);
		return _file.exists() ? _file : null;
	}

	public Document getXmlDocument(File file) {
		try {
			return _DocumentBuilder.parse(file);
		} catch (Exception ex) {
			log.error("解析配置文件 XML 失败：" + ex.getMessage());
		}
		return null;
	}

	public Document getDocument(String fileName) {
		File _file = getFile(fileName);
		if (_file == null) {
			log.error(String.format("无法找到配置文件：%s", fileName));
			return null;
		}
		return getXmlDocument(_file);
	}

	public Node getSingleNode(Element element, String name) {
		NodeList list = element.getElementsByTagName(name);
		if (list.getLength() != 0) {
			return list.item(0);
		}
		return null;
	}

	public String getSingleNodeValue(Element element, String name,
			String defValue) {
		Node _node = getSingleNode(element, name);
		if (_node != null) {
			return _node.getTextContent();
		}
		return defValue;
	}
}
