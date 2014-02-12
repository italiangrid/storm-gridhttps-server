package it.grid.storm.gridhttps.webapp.common.utils;

import java.util.Stack;


public class URIUtils {

	/**
	 * Removes dot segments according to RFC 3986, section 5.2.4
	 * 
	 * @param uri
	 *          the original URI
	 * @return the URI without dot segments
	 */
	public static String removeDotSegments(String path) {

		if ((path == null) || (path.indexOf("/.") == -1)) {
			// No dot segments to remove
			return path;
		}
		String[] inputSegments = path.split("/");
		Stack<String> outputSegments = new Stack<String>();
		for (int i = 0; i < inputSegments.length; i++) {
			if ((inputSegments[i].length() == 0) || (".".equals(inputSegments[i]))) {
				// Do nothing
			} else if ("..".equals(inputSegments[i])) {
				if (!outputSegments.isEmpty()) {
					outputSegments.pop();
				}
			} else {
				outputSegments.push(inputSegments[i]);
			}
		}
		StringBuilder outputBuffer = new StringBuilder();
		for (String outputSegment : outputSegments) {
			outputBuffer.append('/').append(outputSegment);
		}
		return outputBuffer.toString();
	}
	
	/**
   * This class should not be instantiated.
   */
  private URIUtils() {
  }
	
}
