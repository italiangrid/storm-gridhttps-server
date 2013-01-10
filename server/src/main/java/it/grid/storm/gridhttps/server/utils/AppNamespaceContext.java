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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class AppNamespaceContext implements NamespaceContext {
		@SuppressWarnings("unused")
		final private Map<String, String> prefixMap;

		public AppNamespaceContext(Map<String, String> prefixMap) {
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
//				return "http://www.springframework.org/schema/beans";
			else if ("spring".equals(prefix))
				return "http://www.springframework.org/schema/beans";
			else if ("xsi".equals(prefix))
				return "http://www.w3.org/2001/XMLSchema-instance";
			else
				return XMLConstants.NULL_NS_URI;
		}
	}