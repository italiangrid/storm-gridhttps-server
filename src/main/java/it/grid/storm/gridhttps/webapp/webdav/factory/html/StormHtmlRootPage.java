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

import it.grid.storm.gridhttps.common.storagearea.StorageArea;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StormHtmlRootPage extends HtmlPage {

	public StormHtmlRootPage(OutputStream out) {
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

	public void addStorageAreaList(List<StorageArea> sas) {
		openTable();
		openTableRow();
		addTableHeaderCol("storage-area name");
		closeTableRow();
		for (StorageArea sa : sas) {
			String name = sa.getStfnRoot().substring(1);
			String path = buildHref("/webdav" + sa.getStfnRoot(), "");
			openTableRow();
			openTableCol();
			addImage(getFolderIco());
			addLink(name, path);			
			closeTableCol();
			closeTableRow();
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
		openTableRow(new HashMap<String,String>());
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
		addTableCol(new HashMap<String,String>(), content);
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
		String out = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAAGXcA1uAAAABGdBTUEAALGO" +
				"fPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAACFklEQVR4nGL4//8/Aww" +
				"DBBBDSkpKKRDvB2GAAAJxfIH4PwgDBBADsjKAAALJsMKUAQQQA0wJCAMEEIhTiCQwHYjngrQABBBIIgtZJQwDBB" +
				"DYQCCjAIgbkLAmQACh2IiMAQII2cKtQDwbiDtAEgABBJNYB3MaFEcABBDMDgzLAQIIJKiPRaIOIIBgOpBd1AASA" +
				"wggmEQ4SBU0tCJwuRSEAQIIpLgam91Y8CaQBoAAggVGCJGa/gMEEEhDCi4fYsGXAAII5odiIjVIAwQQTs/hwgAB" +
				"RLIGgAACOUcY6vEqIC4DYlF8GgACCJdnpwLxTGiimAPEFjANAAFEbOj8h2kACCCYhjwgTkdLUeh4A0gDQADBNGg" +
				"RaVMpQACBNBwB4jYiNagDBBBIQwYQ3yLWHwABBNIgQIrHAQIIZ07Bgv+A1AIEEEyDUgpaZkHDebBgBQggkpMGQA" +
				"CRrIFUDBBAIOfLAvEbAv49B8Ss5FgAEEAgC+qIjQUi8F8gtke2ACCAQBZUICn4lwIpeX2AWAeINaC0FpQNwmpAr" +
				"IKEQRGoAMVyQCyJbAFAACGXFheAmAmIV1LRR+4AAQSyIBeIfwExHxDHUNFwEA4ACCCQBaBcPAWaXjdQ0fAfQMwC" +
				"EEAgQxOB2AaIOYD4DxUtWAFyNEAAwSMDKBBJ5eAJAZkLEEDIFmgD8TcqGAxKqmUwcwECiOY5GSCAaG4BQIABAFb" +
				"NMXYg1UnRAAAAAElFTkSuQmCC";
		return out;
	}
	
//	private static String getMiltonLogoStyle() {
//		String out = "#miltonlogo { width: 150px; margin-left: 15px; position: absolute; bottom: 8px; right: 15px; }";
//		return out;
//	}
	
	private static String getH1Style() {
		String out = "h1.title { float: left; font-size: 22pt; padding-top: 10px; padding-left: 5px; }";
		return out;
	}

//	private static String getStormLogoStyle() {
//		String out = "#stormlogo { width: 180px; float: right; padding-top: 5px; }";
//		return out;
//	}

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
	
	
}