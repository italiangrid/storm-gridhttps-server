package it.grid.storm;

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

		log.info("WEBAPP-BUILDER: adding webapp " + this.attributes.get("name") + "...... ");
		
		this.unzipTemplate(this.attributes.get("template"), this.attributes.get("outputd"));
		
		log.info("WEBAPP-BUILDER: webapp working directory is " + this.attributes.get("outputd"));
		
		String applicationContextFile = this.attributes.get("outputd") + "/WEB-INF/classes/applicationContext.xml";
		this.setRootDir(applicationContextFile, this.attributes.get("rootd"));
		
		log.info("WEBAPP-BUILDER: webapp " + this.attributes.get("name") + " added!");

	}

	private void unzipTemplate(String zipfile, String outputDirectory)
			throws IOException {
		log.info("WEBAPP-BUILDER: Decompressing " + zipfile + " on directory "
				+ outputDirectory);
		(new Zip()).unzip(zipfile, outputDirectory);
		log.info("WEBAPP-BUILDER: Decompressing OK");
	}

	private void setRootDir(String xmlfilesrc, String rootdir) throws Exception {
		try {
			log.info("WEBAPP-BUILDER: updating root directory......");
			
			//System.out.println("xmlfilesrc: " + xmlfilesrc);
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

			log.info("WEBAPP-BUILDER: root directory updated!");
		} catch (IOException io) {
			io.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

}