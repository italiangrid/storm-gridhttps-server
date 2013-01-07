package it.grid.storm.gridhttps.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class MyCommandLineParser {

	private Options options = new Options();
	private String[] cmdlineArgs = null;
	private CommandLine cmdLine = null;
	private boolean isParsed = false;

	public MyCommandLineParser(String cmdLineArgs[]) {
		this.cmdlineArgs = cmdLineArgs;
		this.addOption("help", "", false, false);
	}

	/**
	 * Adds an option into the command line parser
	 * 
	 * @param optionName
	 *            - the option name
	 * @param description
	 *            - option descriptiuon
	 * @param hasValue
	 *            - if set to true, --option=value, otherwise, --option is a
	 *            boolean
	 * @param isMandatory
	 *            - if set to true, the option must be provided.
	 */
	@SuppressWarnings("static-access")
	public void addOption(String optionName, String description, boolean hasValue, boolean isMandatory) {
		OptionBuilder opt = OptionBuilder.withLongOpt(optionName);
		opt = opt.withDescription(description);
		if (hasValue)
			opt = opt.hasArg();
		if (isMandatory)
			opt = opt.isRequired();
		options.addOption(opt.create());
	}

	private void parse() throws Exception {
		CommandLineParser parser = new GnuParser();
		try {
			this.cmdLine = parser.parse(this.options, this.cmdlineArgs);
		} catch (MissingOptionException moe) {
			printUsage();
		}
		this.isParsed = true;
		if (this.cmdLine.hasOption("help"))
			printUsage();
	}
	
	public void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("available options as follow:", options);
		System.exit(1);
	}

	public String getString(String optionname) throws Exception {
		if (!this.isParsed)
			this.parse();
		return this.cmdLine.getOptionValue(optionname);
	}

	public Integer getInteger(String optionname) throws Exception {
		return Integer.parseInt(this.getString(optionname));
	}

	public Double getDouble(String optionname) throws Exception {
		return Double.parseDouble(this.getString(optionname));
	}

	public List<String> getList(String optionname, String delimiter) throws Exception {
		List<String> arrayList = new ArrayList<String>();
		StringTokenizer tkn = new StringTokenizer(this.getString(optionname), delimiter);
		while (tkn.hasMoreTokens())
			arrayList.add(tkn.nextToken());
		return arrayList;
	}

	public boolean hasOption(String optionName) throws Exception {
		if (!this.isParsed)
			this.parse();
		return this.cmdLine.hasOption(optionName);
	}

}