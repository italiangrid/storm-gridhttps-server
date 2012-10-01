package it.grid.storm.webdav.webapp.factory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.http.Response.Status;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.webdav.WebDavProtocol;
import io.milton.property.PropertySource;
import io.milton.resource.Resource;

class StormPropertySource implements PropertySource {

	private static final Logger log = LoggerFactory
			.getLogger(StormPropertySource.class);

	private String propertyName = "customPropertyName";
	private String propertyNamespace = WebDavProtocol.DAV_URI;
	private String propertyValue = "customPropertyValue";

	public String getProperty(QName name, Resource r)
			throws NotAuthorizedException {
		if (checkPropery(name, r))
			return this.propertyValue;
		return null;
	}

	public void setProperty(QName name, Object value, Resource r) {
		// setProperty for StormPropertyWriter not permit
	}

	public PropertyMetaData getPropertyMetaData(QName name, Resource r)
			throws NotAuthorizedException, BadRequestException {
		if (checkPropery(name, r))
			return new PropertyMetaData(PropertyAccessibility.WRITABLE,
					String.class);
		else
			return new PropertyMetaData(PropertyAccessibility.UNKNOWN, null);
	}

	public void clearProperty(QName name, Resource r)
			throws PropertySetException, NotAuthorizedException {
//		throw new PropertySetException(Status.SC_BAD_REQUEST, "");
	}

	public List<QName> getAllPropertyNames(Resource r)
			throws NotAuthorizedException, BadRequestException {
		List<QName> list = new ArrayList<QName>();
		list.add(new QName(this.propertyNamespace, this.propertyName));
		return list;
	}

	private boolean checkPropery(QName name, Resource r) {
		if ((name.getNamespaceURI().contentEquals(this.propertyNamespace))
				&& (name.getLocalPart().contentEquals(this.propertyName)))
			return true;
		return false;
	}

}