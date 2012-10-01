package it.grid.storm.webdav;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.common.FileUtils;
import io.milton.http.Request;
import io.milton.servlet.SpringMiltonFilter;
import io.milton.http.XmlWriter;
import io.milton.http.webdav.WebDavProtocol;

public class StormSpringMiltonFilter extends SpringMiltonFilter {

	private static final Logger log = LoggerFactory
			.getLogger(StormSpringMiltonFilter.class);

	public void init(FilterConfig fc) throws ServletException {
		super.init(fc);
	}

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain fc) throws IOException, ServletException {

		/*
		 * 
		 * - Con un propget con cadaver:
		 * 
		 * <?xml version="1.0" encoding="utf-8"?> <propfind xmlns="DAV:">
		 * <allprop/> </propfind>
		 * 
		 * - Con un propget di isreadonly con cadaver:
		 * 
		 * <?xml version="1.0" encoding="utf-8"?> <propfind xmlns="DAV:"> <prop>
		 * <isreadonly xmlns="http://webdav.org/cadaver/custom-properties/"/>
		 * </prop> </propfind>
		 * 
		 * - Con un ls con cadaver:
		 * 
		 * <?xml version="1.0" encoding="utf-8"?> <propfind xmlns="DAV:"> <prop>
		 * <getcontentlength xmlns="DAV:"/> <getlastmodified xmlns="DAV:"/>
		 * <executable xmlns="http://apache.org/dav/props/"/> <resourcetype
		 * xmlns="DAV:"/> <checked-in xmlns="DAV:"/> <checked-out xmlns="DAV:"/>
		 * </prop> </propfind>
		 * 
		 * - Con un PROPFIND con Transmit:
		 * 
		 * <?xml version="1.0" encoding="utf-8"?> <propfind xmlns="DAV:"> <prop>
		 * <resourcetype xmlns="DAV:"/> </prop> </propfind>
		 * 
		 * - Con un PROPFIND senza body con Chrome:
		 * 
		 * NIENTEEEEE!!
		 * 
		 * --> Quando non c'è niente devo mettere i seguenti parametri (tutti
		 * con xmlns="DAV:"): creationdate getlastmodified displayname
		 * resourcetype getcontenttype getcontentlength getetag
		 */

		FilteredRequest filteredRequest = new FilteredRequest(req);

		super.doFilter(filteredRequest, resp, fc);
	}

	public void destroy() {
		super.destroy();
	}

	private class FilteredRequest extends HttpServletRequestWrapper {
		
		public FilteredRequest(ServletRequest request) {
			super((HttpServletRequest) request);
		}

		public ServletInputStream getInputStream() throws IOException {
			ServletInputStream sin = super.getInputStream();
			if (super.getMethod().contentEquals(
					Request.Method.PROPFIND.toString())) {
				ByteArrayOutputStream out = FileUtils.readIn(sin);
				log.info("Body request before manipulation:\n"
						+ new String(out.toByteArray()));
				if (out.size() == 0) {
					log.warn("The " + Request.Method.PROPFIND.toString()
							+ " request body is empty");
					out = fillPropfindEmptyBody(out);
				}
				out = addCustomNodeToPropfindBody(out);
				log.info("Body request after manipulation:\n"
						+ new String(out.toByteArray()));
				return new ServletInputStreamWrapper(out.toByteArray());
			}
			return sin;
		}

		private ByteArrayOutputStream addCustomNodeToPropfindBody(ByteArrayOutputStream out) throws IOException{
			if (out != null) {
				// add something like <nameNode xmlns="DAV:"/>
				try {
					SAXBuilder builder = new SAXBuilder();
					Document document = (Document) builder.build(new ServletInputStreamWrapper(out.toByteArray()));
					Element rootNode = document.getRootElement();
					Element node=rootNode.getChild("prop",rootNode.getNamespace());
					if(node != null){
						node.addContent(new Element("customPropertyNode",rootNode.getNamespace()));
					}
					else{
						node=rootNode.getChild("allprop",rootNode.getNamespace());
						if(node==null) throw new IOException("Both prop and allprop nodes missing");
						// if there is allprop node, it is not necessary to add a custom required property
					}
					XMLOutputter outputter = new XMLOutputter();
					ByteArrayOutputStream outModified = new ByteArrayOutputStream();
					outputter.output(document, outModified);
					return outModified;
				} catch (JDOMException e) {
					throw new IOException(e.getMessage());
				}
			}
			else{
				throw new IOException("ByteArrayOutputStream out is null after initialization");
			}
		}
		
		private ByteArrayOutputStream fillPropfindEmptyBody(ByteArrayOutputStream out) throws IOException {
			if (out != null) {
				XmlWriter xml = new XmlWriter(out);
				xml.writeXMLHeader();
				xml.begin("propfind").writeAtt("xmlns",WebDavProtocol.DAV_URI).open();
				xml.open("prop");
				xml.begin("creationdate").writeAtt("xmlns",WebDavProtocol.DAV_URI).close();
				xml.begin("getlastmodified").writeAtt("xmlns",WebDavProtocol.DAV_URI).close();
				xml.begin("displayname").writeAtt("xmlns",WebDavProtocol.DAV_URI).close();
				xml.begin("resourcetype").writeAtt("xmlns",WebDavProtocol.DAV_URI).close();
				xml.begin("getcontenttype").writeAtt("xmlns",WebDavProtocol.DAV_URI).close();
				xml.begin("getcontentlength").writeAtt("xmlns",WebDavProtocol.DAV_URI).close();
				xml.begin("getetag").writeAtt("xmlns",WebDavProtocol.DAV_URI).close();
				xml.close("prop");
				xml.close("propfind");
				xml.flush();
				return out;
			}
			else{
				throw new IOException("ByteArrayOutputStream out is null after initialization");
			}
		}

		private class ServletInputStreamWrapper extends ServletInputStream {

			private byte[] data;
			private int idx = 0;

			ServletInputStreamWrapper(byte[] data) {
				if (data == null)
					data = new byte[0];
				this.data = data;
			}

			@Override
			public int read() throws IOException {
				if (idx == data.length)
					return -1;
				return data[idx++];
			}

		}

	}

}