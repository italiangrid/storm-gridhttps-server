package it.grid.storm.webdav.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XML {
	
	private SAXBuilder builder;
	private File xmlFile;
	private Document document;
	
	
	public XML(String filename) throws JDOMException, IOException {
		builder = new SAXBuilder();
		xmlFile = new File(filename);
		document = (Document) builder.build(xmlFile);
	}

	public XML(InputStream input) throws JDOMException, IOException {
		builder = new SAXBuilder();
		document = (Document) builder.build(input);
	}
	
	public Element getRootElement() {
		return document.getRootElement();
	}
	
	public Element getNodeFromKeyValue(String key, String value) throws Exception {
		return this.getNodeFromKeyValue(this.getRootElement(), key, value);
	}
	
	public Element getNodeFromKeyValue(Element node, String key, String value) throws Exception {
		assert node!=null;
		List<?> children = node.getChildren();
		Iterator<?> i = children.iterator();
		Element current = null;
		while (i.hasNext()) {
			current = (Element) i.next();
			if (current.getAttributeValue(key).equals(value))
				return current;
		}
		throw new Exception("node with '"+key+"' = '"+value+"' not found!");
	}
	
	public Element getNode(String nodeName) throws Exception {
		return this.getNode(this.getRootElement(), nodeName);
	}
	
	public Element getNode(Element node, String nodeName) throws Exception {
		assert node!=null;
		List<?> children = node.getChildren();
		Iterator<?> i = children.iterator();
		Element current = null;
		while (i.hasNext()) {
			current = (Element) i.next();
			if (current.getName().equals(nodeName))
				return current;
		}
		throw new Exception("node '"+nodeName+"' not found!");
	}
	
	public String getAttribute(Element node, String attributeName) {
		assert node!=null;
		assert node.getAttribute(attributeName) != null;
		return node.getAttributeValue(attributeName);
	}
	
	public Element setAttribute(Element node, String attributeName, String attributeValue) {
		assert node!=null;
		return node.setAttribute(attributeName, attributeValue);
	}
	
	public Element addNode(Element nodeFather, String nodeName) throws Exception {
		if (nodeFather.getChild(nodeName) != null)
			throw new Exception("node '"+nodeName+"' already exists!");
		Element element = new Element(nodeName);
		nodeFather.addContent(element);
		return nodeFather.getChild(nodeName);
	}
	
	public Element setNodeValue(Element node, String nodeValue) throws Exception {
		node.setText(nodeValue);
		return node;
	}
	
	
	public void close() throws IOException {
		/* output new file */
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(this.document, new FileWriter(this.xmlFile));
	}
	
}