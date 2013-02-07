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
package it.grid.storm.gridhttps.webapp.webdav.factory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.webdav.WebDavProtocol;
import io.milton.property.PropertySource;
import io.milton.resource.Resource;
import it.grid.storm.gridhttps.webapp.data.StormFileResource;
import it.grid.storm.gridhttps.webapp.data.StormResource;

class StormPropertySource implements PropertySource {

	private static final Logger log = LoggerFactory.getLogger(StormPropertySource.class);

	private ArrayList<String> properties = new ArrayList<String>();
	private String propertyNamespace = WebDavProtocol.DAV_URI;

	public StormPropertySource() {
		super();
		properties.clear();
		properties.add("checksumType");
		properties.add("checksumValue");
		properties.add("status");
	}

	public String getProperty(QName name, Resource r) throws NotAuthorizedException {
		String value = "";
		String property = name.getLocalPart();
		if (checkPropery(name, r)) {
			if (r instanceof StormFileResource) {
				StormFileResource srmFile = (StormFileResource) r;
				if (property.equals("checksumType") && (srmFile.getCheckSumType() != null)) {
					value = srmFile.getCheckSumType().getValue();
				} else if (property.equals("checksumValue") && (srmFile.getCheckSumValue() != null)) {
					value = srmFile.getCheckSumValue().getValue();
				} else if (property.equals("status") && (srmFile.getStatus() != null)) {
					value =  srmFile.getStatus().toString();
				}
			}
		}
		return value;
	}

	public void setProperty(QName name, Object value, Resource r) {
		log.warn("setting property " + name.getLocalPart() + " is not permit");
	}

	public PropertyMetaData getPropertyMetaData(QName name, Resource r) throws NotAuthorizedException, BadRequestException {
		if (checkPropery(name, r))
			return new PropertyMetaData(PropertyAccessibility.READ_ONLY, String.class);
		else
			return new PropertyMetaData(PropertyAccessibility.UNKNOWN, null);
	}

	public void clearProperty(QName name, Resource r) throws PropertySetException, NotAuthorizedException {
		log.warn("clear property " + name.getLocalPart() + " is not permit");
	}
	
	public List<QName> getAllPropertyNames(Resource r) throws NotAuthorizedException, BadRequestException {
		List<QName> list = new ArrayList<QName>();
		if (r instanceof StormResource) {
			list.add(new QName(this.propertyNamespace, "checksumType"));
			list.add(new QName(this.propertyNamespace, "checksumValue"));
			list.add(new QName(this.propertyNamespace, "status"));
		}
		return list;
	}

	private boolean checkPropery(QName name, Resource r) {
		if ((r instanceof StormResource) && (name.getNamespaceURI().contentEquals(this.propertyNamespace))
				&& (properties.contains(name.getLocalPart())))
			return true;
		return false;
	}
}