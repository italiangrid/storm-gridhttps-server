package it.grid.storm.webdav.webapp.factory;

import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.*;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.webdav.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StormResource implements Resource, MoveableResource, CopyableResource, DigestResource {

	private static final Logger log = LoggerFactory.getLogger(StormResource.class);
	File file;
	final StormResourceFactory factory;
	final String host;
	String ssoPrefix;

	protected abstract void doCopy(File dest);

	public StormResource(String host, StormResourceFactory factory, File file) {
		this.host = host;
		this.file = file;
		this.factory = factory;
	}

	public File getFile() {
		return file;
	}

	public String getUniqueId() {
		String s = file.lastModified() + "_" + file.length() + "_" + file.getAbsolutePath();
		return s.hashCode() + "";
	}

	public String getName() {
		return file.getName();
	}

	public Object authenticate(String user, String password) {
		return factory.getSecurityManager().authenticate(user, password);
	}

	public Object authenticate(DigestResponse digestRequest) {
		return factory.getSecurityManager().authenticate(digestRequest);
	}

	public boolean isDigestAllowed() {
		return factory.isDigestAllowed();
	}

	public boolean authorise(Request request, Method method, Auth auth) {
		boolean b = factory.getSecurityManager().authorise(request, method, auth, this);
		if (log.isTraceEnabled()) {
			log.trace("authorise: result=" + b);
		}
		return b;
	}

	public String getRealm() {
		return factory.getRealm(this.host);
	}

	public Date getModifiedDate() {
		return new Date(file.lastModified());
	}

	public Date getCreateDate() {
		return null;
	}

	public int compareTo(Resource o) {
		return this.getName().compareTo(o.getName());
	}

	public void moveTo(CollectionResource newParent, String newName) {
		log.info("Called function for MOVE FILE or DIRECTORY");
		if (newParent instanceof StormDirectoryResource) {
			StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
			File dest = new File(newFsParent.getFile(), newName);
			boolean ok = this.file.renameTo(dest);
			if (!ok) {
				throw new RuntimeException("Failed to move to: " + dest.getAbsolutePath());
			}
			this.file = dest;
		} else {
			throw new RuntimeException("Destination is an unknown type. Must be a StormDirectoryResource, is a: " + newParent.getClass());
		}
	}

	public void copyTo(CollectionResource newParent, String newName) {
		log.info("Called function for COPY FILE or DIRECTORY");
		if (newParent instanceof StormDirectoryResource) {
			StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
			File dest = new File(newFsParent.getFile(), newName);
			doCopy(dest);
		} else {
			throw new RuntimeException("Destination is an unknown type. Must be a StormDirectoryResource, is a: " + newParent.getClass());
		}
	}

	public String getSurl() {
		String rootDir = this.factory.getRoot().getPath();
		String frontendHostname = this.factory.getStormFrontendHostname();
		int frontendPort = this.factory.getStormFrontendPort();
		String path = this.file.getPath().replaceFirst(rootDir, "/" + this.factory.getContextPath());
		String surl = "srm://" + frontendHostname + ":" + frontendPort + path;
		return surl;
	}

	protected boolean isUserAuthorized(String operation) throws IllegalArgumentException, Exception {
		// String userDN = StormResourceHelper.getUserDN();
		// ArrayList<String> userFQANs = StormResourceHelper.getUserFQANs();
		String filename = this.getFile().toString();
		return StormAuthorizationUtils.isUserAuthorized(StormAuthorizationUtils.getVomsSecurityContext(MiltonServlet.request()), 
				operation, filename);
	}

	private SurlInfo doLsDetailed() {
		String userDN = StormResourceHelper.getUserDN();
		ArrayList<String> userFQANs = StormResourceHelper.getUserFQANs();
		String surl = this.getSurl();
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(surl);

		log.debug("userDN = " + userDN);
		log.debug("userFQANs = " + StringUtils.join(userFQANs.toArray(), ","));
		log.debug("surl = " + surl);

		LsOutputData output;
		try {
			log.info("lsDetailed " + surl);
			output = this.factory.getBackendApi().lsDetailed(userDN, userFQANs, surls);
			log.info("success: " + output.isSuccess());
			ArrayList<SurlInfo> infos = (ArrayList<SurlInfo>) output.getInfos();
			return infos.get(0);
		} catch (ApiException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getChecksumType() {
		if (this instanceof StormFileResource) {
			SurlInfo info = doLsDetailed();
			return info.getCheckSumType() == null ? "" : info.getCheckSumType().getValue();
		}
		return "";
	}

	public String getChecksumValue() {
		if (this instanceof StormFileResource) {
			SurlInfo info = doLsDetailed();
			return info.getCheckSumValue() == null ? "" : info.getCheckSumValue().getValue();
		}
		return "";
	}

	public String getStatus() {
		if (this instanceof StormFileResource) {
			SurlInfo info = doLsDetailed();
			return info.getStatus() == null ? "" : info.getStatus().getExplanation();
		}
		return "";
	}

}
