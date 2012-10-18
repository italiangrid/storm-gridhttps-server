package it.grid.storm.webdav.webapp.factory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.webdav.WebDavProtocol;
import io.milton.property.PropertySource;
import io.milton.resource.Resource;

class StormPropertySource implements PropertySource {

//	private static final Logger log = LoggerFactory.getLogger(StormPropertySource.class);

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
		if (checkPropery(name, r)) {
			StormResource srmRes = (StormResource) r;
			if (name.getLocalPart().equals("checksumType")) {
				return srmRes.getChecksumType();
			} else if (name.getLocalPart().equals("checksumValue")) {
				return srmRes.getChecksumValue();
			} else if (name.getLocalPart().equals("status")) {
				return srmRes.getStatus();
			}
			return "";
		}
		return null;
	}

	public void setProperty(QName name, Object value, Resource r) {
		// setProperty for StormPropertyWriter not permit
	}

	public PropertyMetaData getPropertyMetaData(QName name, Resource r) throws NotAuthorizedException, BadRequestException {
		if (checkPropery(name, r))
			return new PropertyMetaData(PropertyAccessibility.READ_ONLY, String.class);
		else
			return new PropertyMetaData(PropertyAccessibility.UNKNOWN, null);
	}

	public void clearProperty(QName name, Resource r) throws PropertySetException, NotAuthorizedException {
		// throw new PropertySetException(Status.SC_BAD_REQUEST, "");
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
		if ((r instanceof StormResource) 
				&& (name.getNamespaceURI().contentEquals(this.propertyNamespace))
				&& (properties.contains(name.getLocalPart())))
			return true;
		return false;
	}

}