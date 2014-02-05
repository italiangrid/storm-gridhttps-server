package it.grid.storm.gridhttps.webapp.webdav;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.common.LogUtils;
import io.milton.http.Response.Status;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindPropertyBuilder;
import io.milton.http.webdav.PropFindResponse;
import io.milton.http.webdav.PropertiesRequest;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.property.PropertySource;
import io.milton.property.PropertySource.PropertyMetaData;
import io.milton.resource.CollectionResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import it.grid.storm.gridhttps.webapp.common.StormResource;
import it.grid.storm.gridhttps.webapp.common.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.common.factory.StormDirectoryResource;
import it.grid.storm.gridhttps.webapp.common.factory.StormFactory;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

public class StormPropFindPropertyBuilder implements PropFindPropertyBuilder {

	private static final Logger log = LoggerFactory.getLogger(StormPropFindPropertyBuilder.class);
	private List<PropertySource> propertySources = new ArrayList<PropertySource>();
	private ArrayList<String> standardProperties = new ArrayList<String>();
	private ArrayList<String> srmProperties = new ArrayList<String>();
	
	public StormPropFindPropertyBuilder() {
		this.propertySources.clear();
		this.standardProperties.clear();
		this.standardProperties.add("getcontentlength");
		this.standardProperties.add("getcontenttype");
		this.standardProperties.add("displayname");
		this.standardProperties.add("resourcetype");
		this.standardProperties.add("quota-used-bytes");
		this.standardProperties.add("quota-available-bytes");
		this.standardProperties.add("getetag");
		this.standardProperties.add("name");
		this.standardProperties.add("iscollection");
		this.standardProperties.add("isreadonly");
		this.standardProperties.add("supported-report-set");
		this.srmProperties.clear();
		this.srmProperties.add("getcreated");
		this.srmProperties.add("creationdate");
		this.srmProperties.add("getlastmodified");
		this.srmProperties.add("checksumType");
		this.srmProperties.add("checksumValue");
		this.srmProperties.add("status");
	}

	public void setPropertySources(List<PropertySource> propertySources) {
		this.propertySources = propertySources;
		log.debug("num property sources: {}" , propertySources.size());
	}

	@Override
	public List<PropFindResponse> buildProperties(PropFindableResource pfr, int depth, PropertiesRequest parseResult, String url)
			throws URISyntaxException, NotAuthorizedException, BadRequestException {
		LogUtils.trace(log, "buildProperties: ", pfr.getClass(), "url:", url);
		url = fixUrlForWindows(url);
		List<PropFindResponse> propFindResponses = new ArrayList<PropFindResponse>();
		appendResponses(propFindResponses, pfr, depth, parseResult, url);
		return propFindResponses;
	}

	private void appendResponses(List<PropFindResponse> responses, PropFindableResource resource, int requestedDepth,
			PropertiesRequest parseResult, String encodedCollectionUrl) throws URISyntaxException, NotAuthorizedException,
			BadRequestException {
		String collectionHref = suffixSlash(resource, encodedCollectionUrl);
		URI parentUri = new URI(collectionHref);
		collectionHref = parentUri.toASCIIString();
		processResource(responses, resource, parseResult, collectionHref, requestedDepth, 0, collectionHref);
	}

	@Override
	public ValueAndType getProperty(QName field, Resource resource) throws NotAuthorizedException, BadRequestException {
		for (PropertySource source : propertySources) {
			PropertyMetaData meta = source.getPropertyMetaData(field, resource);
			if (meta != null && !meta.isUnknown()) {
				Object val = source.getProperty(field, resource);
				return new ValueAndType(val, meta.getValueType());
			}
		}
		LogUtils.trace(log, "getProperty: property not found", field, "resource", resource.getClass(), "property sources", propertySources);
		return null;
	}

	@Override
	public void processResource(List<PropFindResponse> responses, PropFindableResource resource, PropertiesRequest parseResult,
			String href, int requestedDepth, int currentDepth, String collectionHref) throws NotAuthorizedException, BadRequestException {

		if (!(resource instanceof StormResource)) {
			log.error("Unable to process non-StoRM resources!");
			return;
		}
		if (resource instanceof StormDirectoryResource) {
			if (!href.endsWith("/")) {
				href = href + "/";
			}
		}
		Set<QName> requestedFields = parseResult.isAllProp() ? findAllProps(resource) : parseResult.getNames();
		RecursionLevel recursion = requestedDepth <= 1 ? new RecursionLevel(Recursion.NONE) : new RecursionLevel(Recursion.FULL);
		SurlInfo surlInfo = StormResourceHelper.getInstance().doLsDetailed((StormResource) resource, recursion).getInfos().iterator().next();
		process(responses, (StormResource) resource, surlInfo, href, collectionHref, requestedFields, requestedDepth, 0,
				((StormResource) resource).getFactory());
	}

	private void process(List<PropFindResponse> responses, StormResource resource, SurlInfo surlInfo, String href, String collectionHref,
			Set<QName> requestedFields, int requestedDepth, int currentDepth, StormFactory factory) throws NotAuthorizedException,
			BadRequestException {

		final LinkedHashMap<QName, ValueAndType> knownProperties = new LinkedHashMap<QName, ValueAndType>();
		final ArrayList<NameAndError> unknownProperties = new ArrayList<NameAndError>();

		log.debug("propfind processing '{}'" , surlInfo.getStfn());
		Iterator<QName> it = requestedFields.iterator();
		while (it.hasNext()) {
			QName field = it.next();
			if (field.getLocalPart().equals("href")) {
				knownProperties.put(field, new ValueAndType(href, String.class));
				log.debug("property '{}' added!" , field);
				break;
			}
			boolean found = false;
			for (PropertySource source : propertySources) {
				PropertyMetaData meta = source.getPropertyMetaData(field, resource);
				if (meta != null && !meta.isUnknown()) {
					if (this.standardProperties.contains(field.getLocalPart())) {
						Object val = source.getProperty(field, resource);
						if (val == null) {
							// null, but we still need type information to write
							// it so use meta
							knownProperties.put(field, new ValueAndType(val, meta.getValueType()));
						} else {
							// non-null, so use more robust class info
							knownProperties.put(field, new ValueAndType(val, val.getClass()));
						}
						found = true;
						break;
					}
					if (this.srmProperties.contains(field.getLocalPart())) {
						ValueAndType val = getPropertyValue(field, surlInfo, resource);
						if (val == null) {
							// null, but we still need type information to write
							// it so use meta
							knownProperties.put(field, new ValueAndType(null, meta.getValueType()));
						} else {
							// non-null, so use more robust class info
							knownProperties.put(field, val);
						}
						found = true;
						break;
					}
				}
			}
			if (!found) {
				if (log.isDebugEnabled()) {
					log.debug("property not found in any property source: {}" , field.toString());
				}
				unknownProperties.add(new NameAndError(field, null));
			}
		}
		if (log.isDebugEnabled()) {
			if (unknownProperties.size() > 0) {
				log.debug("some properties could not be resolved. Listing property sources:");
				for (PropertySource ps : propertySources) {
					log.debug(" - {}" , ps.getClass().getCanonicalName());
				}
			}
		}
		Map<Status, List<NameAndError>> errorProperties = new EnumMap<Status, List<NameAndError>>(Status.class);
		errorProperties.put(Status.SC_NOT_FOUND, unknownProperties);
		PropFindResponse r = new PropFindResponse(href, knownProperties, errorProperties);
		responses.add(r);

		if (requestedDepth > currentDepth && (resource instanceof StormDirectoryResource)) {
			for (SurlInfo info : StormResourceHelper.getInstance().filterLs(surlInfo.getSubpathInfo())) {
				StormResource child = factory.resolveResource(info, resource.getStorageArea());
				if (child instanceof PropFindableResource) {
					String childName = child.getName();
					if (childName == null) {
						log.warn("null name for resource of type: {} in folder: {} WILL NOT be returned in PROPFIND response!!" , child.getClass() , href);
					} else {
						String childHref = href + child.getName();
						process(responses, child, info, childHref, href, requestedFields, requestedDepth, currentDepth + 1,
								resource.getFactory());
					}
				}
			}
		}
	}

	private ValueAndType getPropertyValue(QName field, SurlInfo info, StormResource resource) {
		String name = field.getLocalPart();
		if (name.equals("getcreated") || name.equals("creationdate") || name.equals("getlastmodified")) {
			if (info.getModificationTime() != null)
				return new ValueAndType(info.getModificationTime(), Date.class);
		}
		if (name.equals("checksumType")) {
			String s = "";
			if (info.getCheckSumType() != null)
				s = info.getCheckSumType().getValue();
			return new ValueAndType(s, String.class);
		}
		if (name.equals("checksumValue")) {
			String s = "";
			if (info.getCheckSumValue() != null)
				s = info.getCheckSumValue().getValue();
			return new ValueAndType(s, String.class);
		}
		if (name.equals("status")) {
			String s = "";
			if (info.getStatus() != null)
				s = info.getStatus().toString();
			return new ValueAndType(s, String.class);
		}
		return null;
	}

	@Override
	public Set<QName> findAllProps(PropFindableResource resource) throws NotAuthorizedException, BadRequestException {
		Set<QName> names = new LinkedHashSet<QName>();
		for (PropertySource source : this.propertySources) {
			List<QName> allprops = source.getAllPropertyNames(resource);
			if (allprops != null) {
				names.addAll(allprops);
			}
		}
		return names;
	}

	private String suffixSlash(PropFindableResource resource, String s) {
		if (resource instanceof CollectionResource && !s.endsWith("/")) {
			s = s + "/";
		}
		return s;
	}

	/**
	 * Requested URL *should* never contain an ampersand because its a reserved
	 * character. However windows 7 does send unencoded ampersands in requests,
	 * but expects them to be encoded in responses.
	 * 
	 * @param url
	 * @return
	 */
	public static String fixUrlForWindows(String url) {
		return url.replace("&", "%26");
	}

	public List<PropertySource> getPropertySources() {

		return this.getPropertySources();
	}

}