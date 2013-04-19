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
package it.grid.storm.gridhttps.webapp.webdav.factory.html;

import it.grid.storm.srm.types.SizeUnit;
import it.grid.storm.srm.types.TFileType;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormHtmlFolderPage extends HtmlPage {

	private static final Logger log = LoggerFactory.getLogger(StormHtmlFolderPage.class);
	
	private class SurlInfoComparator implements Comparator<SurlInfo> {
		public int compare(SurlInfo s1, SurlInfo s2) {
			if (s1 != null && s2 == null) {
				log.warn("s2 surlInfo is NULL!");
				return -1;
			}
			if (s2 != null && s1 == null) {
				log.warn("s1 surlInfo is NULL!");
				return 1;
			}
			if (s1.getType().equals(s2.getType())) {
				return s1.getStfn().compareTo(s2.getStfn());
			}
			if (s1.getType().equals(TFileType.DIRECTORY)) {
				return -1;
			} else { 
				return 1;
			} 
		}
	}

	public StormHtmlFolderPage(OutputStream out) {
		super(out);
	}

	public void start() {
		writeText(HTML_CONSTANT);
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("lang", "en");
		attributes.put("xmlns", "http://www.w3.org/1999/xhtml");
		open("html", attributes);
		open("head");
		addStyle(getEntryListStyle());
		addStyle(getH1Style());
		addStyle(getNavigationTdStyle());
		addStyle(getNavigationTableStyle());
		addStyle(getTooManyResultsWarningStyle());
		close("head");
		open("body");
	}

	public void end() {
		close("body");
		close("html");
		flush();
	}

	public void addTitle(String title) {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("class", "title");
		open("h1", attributes);
		writeText(title);
		close("h1");
	}

	public void addNavigator(String whereami) {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("class", "navigator");
		openTable(attributes);
		openTableRow();
		addTableCol(whereami);
		closeTableRow();
		closeTable();
	}

	public void addTooManyResultsWarning(int nmax) {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("class", "toomanyresults");
		open("div", attributes);
		writeText("This directory contains too many entries, max number returned is " + nmax
				+ ". To increase this value modify storm.properties or ask your system administrator");
		close("div");
	}

	public void addFolderList(String dirPath, ArrayList<SurlInfo> entries) {
		Map<String, String> attributes = new HashMap<String, String>();
		openTable();
		openTableRow();
		addTableHeaderCol("name");
		addTableHeaderCol("size");
		addTableHeaderCol("modified");
		addTableHeaderCol("checksum-type");
		addTableHeaderCol("checksum-value");
		closeTableRow();
		// parent link:
		openTableRow();
		attributes.clear();
		attributes.put("colspan", "5");
		openTableCol(attributes);
		attributes.clear();
		attributes.put("href", buildParentHref(dirPath));
		open("a", attributes);
		addImage(getFolderIco());
		writeText(".");
		close("a");
		closeTableCol();
		closeTableRow();
		// other entries:
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		if (entries != null) {
			Collections.sort(entries, new SurlInfoComparator());
			for (SurlInfo entry : entries) {
				String name = entry.getStfn().split("/")[entry.getStfn().split("/").length - 1];
				String path = buildHref(dirPath, name);
				String size = entry.getSize() != null ? decimalFormat.format(entry.getSize().getSizeIn(SizeUnit.KILOBYTES)) + " KB" : "";
				String modTime = entry.getModificationTime() != null ? dateFormat.format(entry.getModificationTime()) : "";
				String checksumType = entry.getCheckSumType() == null ? "" : entry.getCheckSumType().toString();
				String checksumValue = entry.getCheckSumValue() == null ? "" : entry.getCheckSumValue().toString();
				openTableRow();
				openTableCol();
				if (entry.getType() == null) {
					addImage(getUnknownIco());
				} else if (entry.getType().equals(TFileType.DIRECTORY)) {
					addImage(getFolderIco());
				}
				addLink(name, path);
				closeTableCol();
				addTableCol(size);
				addTableCol(modTime);
				addTableCol(checksumType);
				addTableCol(checksumValue);
				closeTableRow();
			}
		}
		closeTable();
	}

	private void addStyle(String cssStyle) {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("type", "text/css");
		open("style", attributes);
		writeText(cssStyle);
		close("style");
	}

	private void addTableHeaderCol(String content) {
		openTableCol();
		open("b");
		writeText(content);
		close("b");
		closeTableCol();
	}

	private String buildHref(String uri, String name) {
		String abUrl = uri;
		if (!abUrl.endsWith("/")) {
			abUrl += "/";
		}
		return abUrl + name;
	}

	private String buildParentHref(String uri) {
		String abUrl = uri;
		if (abUrl.endsWith("/")) {
			abUrl = abUrl.substring(0, abUrl.length() - 2);
		}
		String[] parts = abUrl.split("/");
		String lastPart = parts[parts.length - 1];
		abUrl = abUrl.substring(0, abUrl.length() - lastPart.length());
		return abUrl;
	}

	private void addLink(String label, String href) {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("href", href);
		addLink(label, attributes);
	}

	private void addLink(String label, Map<String, String> attributes) {
		open("a", attributes);
		writeText(label);
		close("a");
	}

	private void addImage(String src) {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("alt", "");
		attributes.put("src", src);
		addImage(attributes);
	}

	private void addImage(Map<String, String> attributes) {
		open("img", attributes);
		close("img");
	}

	private void openTable() {
		open("table");
	}

	private void closeTable() {
		close("table");
	}

	private void openTable(Map<String, String> attributes) {
		open("table", attributes);
	}

	private void openTableRow() {
		openTableRow(new HashMap<String, String>());
	}

	private void openTableRow(Map<String, String> attributes) {
		open("tr", attributes);
	}

	private void closeTableRow() {
		close("tr");
	}

	private void openTableCol() {
		open("td");
	}

	private void openTableCol(Map<String, String> attributes) {
		open("td", attributes);
	}

	private void addTableCol(String content) {
		addTableCol(new HashMap<String, String>(), content);
	}

	private void addTableCol(Map<String, String> attributes, String content) {
		openTableCol(attributes);
		writeText(content);
		closeTableCol();
	}

	private void closeTableCol() {
		close("td");
	}

	private static String getFolderIco() {
		String out = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAAGXcA1uAAAABGdBTUEAALGO"
				+ "fPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAACFklEQVR4nGL4//8/Aww"
				+ "DBBBDSkpKKRDvB2GAAAJxfIH4PwgDBBADsjKAAALJsMKUAQQQA0wJCAMEEIhTiCQwHYjngrQABBBIIgtZJQwDBB"
				+ "DYQCCjAIgbkLAmQACh2IiMAQII2cKtQDwbiDtAEgABBJNYB3MaFEcABBDMDgzLAQIIJKiPRaIOIIBgOpBd1AASA"
				+ "wggmEQ4SBU0tCJwuRSEAQIIpLgam91Y8CaQBoAAggVGCJGa/gMEEEhDCi4fYsGXAAII5odiIjVIAwQQTs/hwgAB"
				+ "RLIGgAACOUcY6vEqIC4DYlF8GgACCJdnpwLxTGiimAPEFjANAAFEbOj8h2kACCCYhjwgTkdLUeh4A0gDQADBNGg"
				+ "RaVMpQACBNBwB4jYiNagDBBBIQwYQ3yLWHwABBNIgQIrHAQIIZ07Bgv+A1AIEEEyDUgpaZkHDebBgBQggkpMGQA"
				+ "CRrIFUDBBAIOfLAvEbAv49B8Ss5FgAEEAgC+qIjQUi8F8gtke2ACCAQBZUICn4lwIpeX2AWAeINaC0FpQNwmpAr"
				+ "IKEQRGoAMVyQCyJbAFAACGXFheAmAmIV1LRR+4AAQSyIBeIfwExHxDHUNFwEA4ACCCQBaBcPAWaXjdQ0fAfQMwC"
				+ "EEAgQxOB2AaIOYD4DxUtWAFyNEAAwSMDKBBJ5eAJAZkLEEDIFmgD8TcqGAxKqmUwcwECiOY5GSCAaG4BQIABAFb" + "NMXYg1UnRAAAAAElFTkSuQmCC";
		return out;
	}

	private static String getUnknownIco() {
		String out = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0"
				+ "d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyJpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCB"
				+ "iZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYW"
				+ "RvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMC1jMDYxIDY0LjE0MDk0OSwgMjAxMC8xM"
				+ "i8wNy0xMDo1NzowMSAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5"
				+ "LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJ"
				+ "odHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YX"
				+ "AvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZ"
				+ "VJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENTNS4xIFdpbmRvd3MiIHhtcE1NOkluc3Rh"
				+ "bmNlSUQ9InhtcC5paWQ6RUVCMTcyNTE1NTc3MTFFMTk5MzNGMDZGQzY3RTI0Q0MiIHhtcE1NOkRvY3VtZW50SUQ"
				+ "9InhtcC5kaWQ6RUVCMTcyNTI1NTc3MTFFMTk5MzNGMDZGQzY3RTI0Q0MiPiA8eG1wTU06RGVyaXZlZEZyb20gc3"
				+ "RSZWY6aW5zdGFuY2VJRD0ieG1wLmlpZDpFRUIxNzI0RjU1NzcxMUUxOTkzM0YwNkZDNjdFMjRDQyIgc3RSZWY6Z"
				+ "G9jdW1lbnRJRD0ieG1wLmRpZDpFRUIxNzI1MDU1NzcxMUUxOTkzM0YwNkZDNjdFMjRDQyIvPiA8L3JkZjpEZXNj"
				+ "cmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/Pu+kldkAAAKmSURBVHj"
				+ "aZFNNSFRRFP7enTfzZhQyrUSplKQGosK1uUiCjNwU4iaCFgUVpBSUuChLrBYtKo1qZW2KWmhmKynBRWRBEFSTKe"
				+ "g4WYzO6KgzOuO8eT9zO/e9eaPVgcN795zvO/c7594r4R8buYKHyOIUOLz5oAQVDE9qb+H8eiznnFIOsR31XMeb3"
				+ "UeOo6TmGOAuz7NhzmBxZABjgy8guXG49gbeOgVs8lWc+HiN1pEuHvzSyauqtvKCQg+vrCwWCO73b+cr4Tucz3bx"
				+ "D4QjfP1fBd61gWcmOznXWy1C9b5Syt0k7+AzgTNWDIxZeZVwAu8UYO+p58qaQ/AoQVL6zSr4NTCH/p5BIDGB8j0"
				+ "aCgtkIJsFjAkohBN4wRNYRvHTW3ZWw1jsg4ul0fvAj5aTpWjYrwOpn0iO/kBq1bClJpdgLvVB4AVPhGQSo8huGc"
				+ "ZKClkziKYDDE11CqVmYMxxVNRFLG5PRxEwH4SxmoJcLIumFLsAWTYTh6mSQjUMnYbOGODzAQ10aAZtHngO7PUvQ"
				+ "40laBMiFMTzRylLkiiQgKkBuZlSK3bpoU9AephOlP6TcW6TxcESXspdAJlL0NT4rIcZXupLtQEuIBoFSki1QsBk"
				+ "kpTkyIx5QXgInrWOJTEQm56gbbfBzAg1pIR8M7Ww8Ar5mJlzgRN4wbMKNN5HazQURnrZpDtXQlsBHkp095KSg/b"
				+ "XLXrTYeUFTuAFzypA/uvzNNpDoyFoGReYXITJ3y5cemxLvkxfV9aOa6oLAjc8jmbBcwrg4jM8GvqO5qmxecQiae"
				+ "woU3DvnD2lu2clZEwFsWgaU+PzELjrL/F07Z2t2QbyXf0X0Fa2EUd9HnjcNEydhpfWoEXieN3YjduEoYFh+b/Xu"
				+ "M4qyDc56nJG9xgLjuz1z/mPAAMASV1EgHfj3lYAAAAASUVORK5CYII=";
		return out;
	}

	private static String getH1Style() {
		String out = "h1.title { float: left; font-size: 22pt; padding-top: 10px; padding-left: 5px; }";
		return out;
	}

	private static String getEntryListStyle() {
		String out = "table {width: 100%; font-family: Arial,\"Bitstream Vera Sans\",Helvetica,Verdana,sans-serif; color: #333;}";
		out += "table td, table th {color: #555;}";
		out += "table th {text-shadow: rgba(255, 255, 255, 0.796875) 0px 1px 0px; font-family: Georgia,\"Times New Roman\",\"Bitstream Charter\",Times,serif; font-weight: normal; padding: 7px 7px 8px; text-align: left; line-height: 1.3em; font-size: 14px;}";
		out += "table td {font-size: 12px; padding: 4px 7px 2px; vertical-align: top; }";
		out += "img {margin-right: 5px; margin-top: 0; vertical-align: bottom; width: 12px; }";
		return out;
	}

	private static String getNavigationTableStyle() {
		String out = "table.navigator { border-bottom: solid lightgray 1px; margin-bottom: 16px; margin-top: 20px; clear: both; }";
		return out;
	}

	private static String getNavigationTdStyle() {
		String out = "table.navigator td { font-size: 14pt; padding-bottom: 8px; }";
		return out;
	}

	private static String getTooManyResultsWarningStyle() {
		String out = "div.toomanyresults { background: #FFA18c; padding: 3px 10px; border: 1px solid red; font-weight: bold; color: #9a1f00; font-size: 11pt; font-style: italic; }";
		return out;
	}

}