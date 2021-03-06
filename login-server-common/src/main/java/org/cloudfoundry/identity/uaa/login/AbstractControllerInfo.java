package org.cloudfoundry.identity.uaa.login;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.util.UaaStringUtils;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.ui.Model;
/**
 * Contains basic information used by the 
 * login-server controllers.
 * @author fhanik
 *
 */
public abstract class AbstractControllerInfo {
	private final Log logger = LogFactory.getLog(getClass());
	private Map<String,String> links = new HashMap<String, String>();
	private static String DEFAULT_BASE_UAA_URL = "https://uaa.cloudfoundry.com";
	protected static final String HOST = "Host";


	private Properties gitProperties = new Properties();

	private Properties buildProperties = new Properties();

	private String baseUrl;

	private String uaaHost;


	/**
	 * @param links the links to set
	 */
	public void setLinks(Map<String, String> links) {
		this.links = links;
	}

	public Map<String, String> getLinks() {
		return links;
	}
	
	protected void initProperties() {
		setUaaBaseUrl(DEFAULT_BASE_UAA_URL);
		try {
			gitProperties = PropertiesLoaderUtils.loadAllProperties("git.properties");
		}
		catch (IOException e) {
			// Ignore
		}
		try {
			buildProperties = PropertiesLoaderUtils.loadAllProperties("build.properties");
		}
		catch (IOException e) {
			// Ignore
		}
	}
	
	/**
	 * @param baseUrl the base uaa url
	 */
	public void setUaaBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		try {
			this.uaaHost = new URI(baseUrl).getHost();
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Could not extract host from URI: " + baseUrl);
		}
	}

	protected String getUaaBaseUrl() {
		return baseUrl;
	}
	
	protected String getUaaHost() {
		return uaaHost;
	}

	protected Map<String, ?> getBuildInfo() {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("commit_id", gitProperties.getProperty("git.commit.id.abbrev", "UNKNOWN"));
		model.put(
				"timestamp",
				gitProperties.getProperty("git.commit.time",
						new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())));
		model.put("app", UaaStringUtils.getMapFromProperties(buildProperties, "build."));
		return model;
	}
	
	protected Map<String, ?> getLinksInfo() {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("uaa", getUaaBaseUrl());
		model.put("login", getUaaBaseUrl().replaceAll("uaa", "login"));
		model.putAll(getLinks());
		return model;
	}

	protected HttpHeaders getRequestHeaders(HttpHeaders headers) {
		// Some of the headers coming back are poisonous apparently (content-length?)...
		HttpHeaders outgoingHeaders = new HttpHeaders();
		outgoingHeaders.putAll(headers);
		outgoingHeaders.remove(HOST);
		outgoingHeaders.remove(HOST.toLowerCase());
		outgoingHeaders.set(HOST, getUaaHost());
		logger.debug("Outgoing headers: " + outgoingHeaders);
		return outgoingHeaders;
	}

	protected String extractPath(HttpServletRequest request) {
		String query = request.getQueryString();
		try {
			query = query == null ? "" : "?" + URLDecoder.decode(query, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Cannot decode query string: " + query);
		}
		String path = request.getRequestURI() + query;
		String context = request.getContextPath();
		path = path.substring(context.length());
		if (path.startsWith("/")) {
			// In the root context we have to remove this as well
			path = path.substring(1);
		}
		logger.debug("Path: " + path);
		return path;
	}
	protected void populateBuildAndLinkInfo(Model model) {
	    Map<String, Object> attributes = new HashMap<String, Object>();
        populateBuildAndLinkInfo(attributes);
        model.addAllAttributes(attributes);
        model.addAttribute("links", getLinks() );
    }
	
	protected void populateBuildAndLinkInfo(Map<String, Object> model) {
		model.putAll(getBuildInfo());
		model.put("links", getLinksInfo());
	}


}
