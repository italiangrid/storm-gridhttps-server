/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.grid.storm.gridhttps.server.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XML {

	// private static final Logger log = LoggerFactory.getLogger(XML.class);

	private DocumentBuilderFactory domFactory;
	private DocumentBuilder builder;
	private File xmlFile;
	private Document document;
	private Transformer transformer;
	private XPath xpath;

	private XML() throws ParserConfigurationException, TransformerConfigurationException {
		domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true); // never forget this!
		builder = domFactory.newDocumentBuilder();
		transformer = TransformerFactory.newInstance().newTransformer();
		xpath = XPathFactory.newInstance().newXPath();
	}

	public XML(String filename) throws IOException, SAXException, ParserConfigurationException, TransformerConfigurationException {
		this(new File(filename));
	}

	public XML(File xmlFile) throws IOException, SAXException, ParserConfigurationException, TransformerConfigurationException {
		this();
		this.xmlFile = xmlFile;
		document = builder.parse(xmlFile);
	}

	public Document getDocument() {
		return document;
	}

	public File getFile() {
		return xmlFile;
	}

	public void addNamespace() {

	}

	public void saveToFile(File outputFile) throws IOException, TransformerException {
		Result result = new StreamResult(outputFile);
		Source source = new DOMSource(document);
		transformer.transform(source, result);
	}

	public void save() throws IOException, TransformerException {
		saveToFile(getFile());
	}

	public NodeList getNodes(String expression, NamespaceContext namespaceContext) throws XPathExpressionException {
		xpath.setNamespaceContext(namespaceContext);
		return (NodeList) xpath.evaluate(expression, getDocument(), XPathConstants.NODESET);
	}
	
	public Node getNode(String expression, NamespaceContext namespaceContext) throws XPathExpressionException {
		xpath.setNamespaceContext(namespaceContext);
		return (Node) xpath.evaluate(expression, getDocument(), XPathConstants.NODE);
	}
	
}