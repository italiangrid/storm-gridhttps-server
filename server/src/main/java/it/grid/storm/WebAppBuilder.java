package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class WebAppBuilder {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	private Map<String, String> attributes = new HashMap<String, String>();

	public WebAppBuilder() {

	}

	public void addWebApp(Map<String, String> attributes) throws Exception {

		this.attributes.clear();
		this.attributes.putAll(attributes);
		if (this.attributes.get("template") == null)
			throw new Exception("Template file not defined!");
		if (this.attributes.get("name") == null)
			throw new Exception("webapp name not defined!");
		if (this.attributes.get("rootd") == null)
			throw new Exception("root directory not defined!");
		if (this.attributes.get("outputd") == null)
			throw new Exception("output directory not defined!");

		this.unzipTemplate(this.attributes.get("template"),
				this.attributes.get("outputd"));
		this.setRootDir(this.attributes.get("outputd")
				+ "/WEB-INF/classes/applicationContext.xml",
				this.attributes.get("rootd"));

	}

	private void unzipTemplate(String zipfile, String outputDirectory)
			throws IOException {
		log.info("Decompressing " + zipfile + " on directory "
				+ outputDirectory);
		(new Zip()).unzip(zipfile, outputDirectory);
		log.info("Decompressing OK");
	}

	private void setRootDir(String xmlfilesrc, String rootdir) throws Exception {
		try {

			SAXBuilder builder = new SAXBuilder();
			File xmlFile = new File(xmlfilesrc);
			Document doc = (Document) builder.build(xmlFile);

			Element rootNode = doc.getRootElement();
			List<?> beans = rootNode.getChildren();
			Element current = null;
			Iterator<?> i = beans.iterator();
			while (i.hasNext()) {
				current = (Element) i.next();
				if (current.getAttributeValue("id").equals(
						"milton.fs.resource.factory"))
					break;
			}

			List<?> properties = current.getChildren();
			Element property = null;
			i = properties.iterator();
			while (i.hasNext()) {
				property = (Element) i.next();
				if (property.getAttributeValue("name").equals("root"))
					break;
			}
			property.setAttribute("value", rootdir);

			XMLOutputter xmlOutput = new XMLOutputter();

			// display nice nice
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter(xmlfilesrc));

			log.info("root directory on applicationContext.xml updated!");
		} catch (IOException io) {
			io.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

}