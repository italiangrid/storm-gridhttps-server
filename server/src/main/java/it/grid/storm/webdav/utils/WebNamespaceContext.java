package it.grid.storm.webdav.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class WebNamespaceContext implements NamespaceContext {
		@SuppressWarnings("unused")
		final private Map<String, String> prefixMap;

		public WebNamespaceContext(Map<String, String> prefixMap) {
			if (prefixMap != null) {
				this.prefixMap = Collections.unmodifiableMap(new HashMap<String, String>(prefixMap));
			} else {
				this.prefixMap = Collections.emptyMap();
			}
		}

		public String getPrefix(String namespaceURI) {
			return null;
		}

		public Iterator<?> getPrefixes(String namespaceURI) {
			return null;
		}

		public String getNamespaceURI(String prefix) {
			if (prefix == null)
				throw new NullPointerException("Invalid Namespace Prefix");
//			else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX))
//				return "http://java.sun.com/xml/ns/j2ee";
			else if ("j2ee".equals(prefix))
				return "http://java.sun.com/xml/ns/j2ee";
			else if ("xsi".equals(prefix))
				return "http://www.w3.org/2001/XMLSchema-instance";
			else
				return XMLConstants.NULL_NS_URI;
		}
	}