package it.grid.storm.webdav.factory.html;

import java.io.OutputStream;
import java.util.Map;

import io.milton.http.XmlWriter;
import io.milton.http.XmlWriter.Element;

public class HtmlPage {
	private XmlWriter writer;
	public final String HTML_CONSTANT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
	
	public HtmlPage(OutputStream out) {
		writer = new XmlWriter(out);
	}
	
	public void open(String tag) {
		writer.open(tag);
	}
	
	public void open(String tag, Map<String, String> attributes) {
		Element e = writer.begin(tag);
		for (String name : attributes.keySet()) {
			e.writeAtt(name, attributes.get(name));
		}
		e.open();
	}
	
	public void writeText(String text) {
		writer.writeText(text);
	}
	
	public void close(String tag) {
		writer.close(tag);
	}
	
	protected void flush() {
		writer.flush();
	}
	
}