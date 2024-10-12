package org.cytoscape.cytocontainer.rest;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.OutputStreamAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.RolloverFileOutputStream;

import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithms;
import org.cytoscape.cytocontainer.rest.model.AlgorithmParameter;
import org.cytoscape.cytocontainer.rest.model.CXDataType;
import org.cytoscape.cytocontainer.rest.model.ServiceInputDefinition;
import org.cytoscape.cytocontainer.rest.model.SelectedDataParameter;
import org.cytoscape.cytocontainer.rest.model.CyWebMenuItem;
import org.cytoscape.cytocontainer.rest.model.CyWebMenuItemPath;
import org.cytoscape.cytocontainer.rest.model.CytoContainerParameter;
import org.cytoscape.cytocontainer.rest.model.InputColumn;
import org.cytoscape.cytocontainer.rest.model.InputNetwork;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.cytoscape.cytocontainer.rest.services.Configuration;
import org.cytoscape.cytocontainer.rest.services.CytoContainerHttpServletDispatcher;
import org.eclipse.jetty.ee10.servlet.FilterHolder;



/**
 *
 * @author churas
 */
public class App {
    
    static Logger _logger = LoggerFactory.getLogger(App.class);

    public static final String DESCRIPTION = "\nCytoscape Container REST service\n\n"
            + "For usage information visit:  https://github.com/cytoscape/cytocontainer-rest-server\n\n";
    
    /**
     * Default name for community detection algorithms json file
     */
    public static final String CD_ALGORITHMS_FILE = "algorithms.json";
    
    /**
     * Sets logging level valid values DEBUG INFO WARN ALL ERROR
     */
    public static final String RUNSERVER_LOGLEVEL = "runserver.log.level";

    /**
     * Sets log directory for embedded Jetty
     */
    public static final String RUNSERVER_LOGDIR = "runserver.log.dir";
    
    /**
     * Sets port for embedded Jetty
     */
    public static final String RUNSERVER_PORT = "runserver.port";
        
    /**
     * Sets context path for embedded Jetty
     */
    
    public static final String RUNSERVER_DOSFILTER_MAX_REQS = "runserver.dosfilter.maxrequestspersec";
    
    public static final String RUNSERVER_DOSFILTER_DELAY = "runserver.dosfilter.delayms";
    
    public static final String MODE = "mode";
    public static final String CONF = "conf";    
    public static final String EXAMPLE_CONF_MODE = "exampleconf";
    public static final String EXAMPLE_ALGO_MODE = "examplealgo";
    public static final String RUNSERVER_MODE = "runserver";
	public static final String PRINT_ALGOS_MODE = "printalgorithms";
	public static final String GENERATE_ALGO_MODE = "generatealgorithm";
    
    public static final String SUPPORTED_MODES = EXAMPLE_CONF_MODE + ", " +
                                                 EXAMPLE_ALGO_MODE +
                                                    ", " + RUNSERVER_MODE +
			                                        ", " + PRINT_ALGOS_MODE +
			                                        ", " + GENERATE_ALGO_MODE;
    
    public static void main(String[] args){

        final List<String> helpArgs = Arrays.asList("h", "help", "?");
        try {
            OptionParser parser = new OptionParser() {

                {
                    accepts(MODE, "Mode to run. Supported modes: " + SUPPORTED_MODES).withRequiredArg().ofType(String.class).required();
                    accepts(CONF, "Configuration file")
                            .withRequiredArg().ofType(String.class);
                    acceptsAll(helpArgs, "Show Help").forHelp();
                }
            };
            
            OptionSet optionSet = null;
            try {
                optionSet = parser.parse(args);
            } catch (OptionException oe) {
                System.err.println("\nThere was an error parsing arguments: "
                        + oe.getMessage() + "\n\n");
                parser.printHelpOn(System.err);
                System.exit(1);
            }

            //help check
            for (String helpArgName : helpArgs) {
                if (optionSet.has(helpArgName)) {
                    System.out.println(DESCRIPTION);
                    parser.printHelpOn(System.out);
                    System.exit(2);
                }
            }
            
            String mode = optionSet.valueOf(MODE).toString();

            if (mode.equals(EXAMPLE_CONF_MODE)){
                System.out.println(generateExampleConfiguration());
                System.out.flush();
                return;
            }
            
            if (mode.equals(EXAMPLE_ALGO_MODE)){
                System.out.println(generateExampleCytoContainerAlgorithms());
                System.out.flush();
                return;
            }
			if (mode.equals(PRINT_ALGOS_MODE)){
				Configuration.setAlternateConfigurationFile(optionSet.valueOf(CONF).toString());
			
				System.out.println(getAlgorithmSummary(Configuration.getInstance().getAlgorithms()));
				System.out.flush();
				return;
			}
			if (mode.equals(GENERATE_ALGO_MODE)){
				System.out.println(generateAlgorithm());
				return;
			}
      
            if (mode.equals(RUNSERVER_MODE)){
                Configuration.setAlternateConfigurationFile(optionSet.valueOf(CONF).toString());
                Properties props = getPropertiesFromConf(optionSet.valueOf(CONF).toString());
                ch.qos.logback.classic.Logger rootLog = 
        		(ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                rootLog.setLevel(Level.toLevel(props.getProperty(App.RUNSERVER_LOGLEVEL, "INFO")));

                String logDir = props.getProperty(App.RUNSERVER_LOGDIR, ".");
                RolloverFileOutputStream os = new RolloverFileOutputStream(logDir 
                        + File.separator + "cytocontainer_yyyy_mm_dd.log", true);
				
				RolloverFileOutputStream requestOS = new RolloverFileOutputStream(logDir + File.separator + "requests_yyyy_mm_dd.log", true);
				
				LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

				PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
				logEncoder.setContext(lc);
				logEncoder.setPattern("[%date]\t%msg%n");
				logEncoder.start();
				
				OutputStreamAppender osa = new OutputStreamAppender();
				osa.setOutputStream(requestOS);
				osa.setContext(lc);
				osa.setEncoder(logEncoder);
				osa.setName(RequestLoggingFilter.REQUEST_LOGGER_NAME + "appender");
				osa.start();
				ch.qos.logback.classic.Logger requestLog = 
					  (ch.qos.logback.classic.Logger) lc.getLogger(RequestLoggingFilter.REQUEST_LOGGER_NAME);
                requestLog.setLevel(Level.toLevel("INFO"));
				
				requestLog.setAdditive(false);
				requestLog.addAppender(osa);
		
		
                final int port = Integer.valueOf(props.getProperty(App.RUNSERVER_PORT, "8081"));
                String applicationPath = App.getApplicationPath(props);
                System.out.println("\nSpinning up server. For status visit: \nhttp://localhost:" 
                        + Integer.toString(port) + props.getProperty(Configuration.RUNSERVER_CONTEXTPATH) 
                        + applicationPath + Configuration.V_ONE_PATH + "/status\n");
                System.out.println("Swagger documentation: " + "http://localhost:" 
                        + Integer.toString(port) + props.getProperty(Configuration.RUNSERVER_CONTEXTPATH) + "\n");
                System.out.flush();
                
						//We are creating a print stream based on our RolloverFileOutputStream
				PrintStream logStream = new PrintStream(os);

						//We are redirecting system out and system error to our print stream.
				System.setOut(logStream);
				System.setErr(logStream);

                final Server server = new Server(port);

                final ServletContextHandler webappContext = new ServletContextHandler(Configuration.getInstance().getRunServerContextPath());
              
                
                HashMap<String, String> initMap = new HashMap<>();
                initMap.put("resteasy.servlet.mapping.prefix",
                             applicationPath + "/");
                initMap.put("jakarta.ws.rs.Application",
                        "org.cytoscape.cytocontainer.rest.CytoContainerApplication");
                final ServletHolder restEasyServlet = new ServletHolder(
                     new CytoContainerHttpServletDispatcher());
                
                restEasyServlet.setInitOrder(1);
                
                webappContext.addServlet(restEasyServlet,
                                          applicationPath + "/*");
                
				FilterHolder filterHolder = new FilterHolder(new CorsFilter());
                webappContext.addFilter(filterHolder,
                                        applicationPath + "/*", null);
				
				FilterHolder reqFilterHolder = new FilterHolder(new RequestLoggingFilter());
				webappContext.addFilter(reqFilterHolder,
                                        applicationPath + "/*", null);
                webappContext.addFilter(FilterDispatcher.class, "/*", null);
                

				initMap.put("openApi.configuration.resourcePackages",
						"org.cytoscape.cytocontainer.rest.services,org.cytoscape.cytocontainer.rest.model");
				initMap.put("openApi.configuration.scannerClass", "org.cytoscape.cytocontainer.rest.swagger.SwaggerScanner");
				initMap.put("openApi.configuration.filterClass", "org.cytoscape.cytocontainer.rest.swagger.SwaggerFilter");
				initMap.put("openApi.configuration.prettyPrint", "true");


                String resourceBasePath = App.class.getResource("/webapp").toExternalForm();
                webappContext.setWelcomeFiles(new String[] { "index.html" });
                webappContext.setBaseResourceAsString(resourceBasePath);
                webappContext.addServlet(new ServletHolder(new DefaultServlet()), "/*");
				
				restEasyServlet.setInitParameters(initMap);
                ContextHandlerCollection contexts = new ContextHandlerCollection();
                contexts.setHandlers(new Handler[] { webappContext });
 
                server.setHandler(contexts);
                   
                server.start();	    
                System.out.println("Server started on: " + server.getURI().toString());
                server.join();
                return;
            }
            System.err.println("Invalid --mode: " + mode + " mode must be one of the "
                    + "following: " + SUPPORTED_MODES);
            System.exit(3);
      
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }
    
	public static String getAlgorithmSummary(CytoContainerAlgorithms algos) {
		StringBuilder sb = new StringBuilder();
		
		if (algos == null || algos.getAlgorithms() == null){
			return "No algorithms found";
		}
		for (String key : algos.getAlgorithms().keySet()){
			sb.append(key);
			sb.append("\n");

		}

		return sb.toString();
	}
    /**
     * Gets Comma delimited list of classes that should be parsed by OpenAPI to
     * generate Swagger documentation. If a diffusion algorithm was set in config,
     * then that class will also be added.
     * 
     * @return Comma delimited list of classes as a String
     */
    public static String getClassesToParseByOpenApi(){
        StringBuilder sb = new StringBuilder();
        sb.append("org.cytoscape.cytocontainer.rest.services.CytoContainer,");
        sb.append("org.cytoscape.cytocontainer.rest.services.Status");
        return sb.toString();
    }
    
    /**
     * Gets {@link org.cytoscape.cytocontainer.rest.services.Configuration#RUNSERVER_APP_PATH} 
     * from properties passed in
     * @param props Properties to examine
     * @return Application path
     */
    public static String getApplicationPath(Properties props){
        return props.getProperty(Configuration.RUNSERVER_APP_PATH, "/cytocontainer");
    }
    
    /**
     * Loads properties from configuration file specified by {@code path}
     * @param path Path to configuration file
     * @return Properties found in configuration file passed in
     * @throws IOException thrown by {@link java.util.Properties#load(java.io.InputStream)}
     * @throws FileNotFoundException thrown by {@link java.util.Properties#load(java.io.InputStream)}
     */
    public static Properties getPropertiesFromConf(final String path) throws IOException, FileNotFoundException {
        Properties props = new Properties();
        props.load(new FileInputStream(path));
        return props;
    }
    
	/**
	 * Asks user a series of questions to generate json of an algorithm
	 * @return 
	 */
	public static String generateAlgorithm() throws Exception {
		CytoContainerAlgorithm algo = new CytoContainerAlgorithm();
		Scanner userIn = new Scanner(System.in);
		System.out.print("Algorithm name: ");
		algo.setName(userIn.nextLine());
		System.out.print("Docker Image: ");
		algo.setDockerImage(userIn.nextLine());
		while(true){
			System.out.print("cyWebAction (" + CytoContainerAlgorithm.ACTION_SET + "): ");
			try {
				algo.setCyWebAction(userIn.nextLine());
				break;
			} catch(CytoContainerException cce){
				System.err.println("\t" + cce.getMessage());
			}
		}
		
		System.out.print("Version: ");
		algo.setVersion(userIn.nextLine());
		
		System.out.print("Description: ");
		algo.setDescription(userIn.nextLine());
		
		System.out.print("Author: ");
		algo.setAuthor(userIn.nextLine());
		
		System.out.print("Citation: ");
		algo.setCitation(userIn.nextLine());
		
		System.out.print("Top Menu (Example Apps): ");
		CyWebMenuItem menu = new CyWebMenuItem();
		menu.setRoot(userIn.nextLine());
		CyWebMenuItemPath path = null;
		List<CyWebMenuItemPath> submenus = new ArrayList<>();
		while(true){
			path = new CyWebMenuItemPath();
			System.out.print("Menu name: ");
			path.setName(userIn.nextLine());
			while(true){
				System.out.print("Menu gravity: ");
				try {
					path.setGravity(Integer.parseInt(userIn.nextLine()));
					break;
				} catch(NumberFormatException nfe){
					System.err.print("Must enter an integer\n");
				}
			}
			submenus.add(path);
			System.out.print("Add another submenu (y/n)? ");
			String answer = userIn.nextLine();
			if (answer.startsWith("n") || answer.startsWith("N")){
				break;
			}
		}
		menu.setPath(submenus);
		algo.setCyWebMenuItem(menu);
		
		ServiceInputDefinition inputDef = new ServiceInputDefinition();
		while(true){
			System.out.print("Type of data that should be sent to service (" + ServiceInputDefinition.TYPE_SET +"): ");
			try {
				inputDef.setType(userIn.nextLine());
				break;
			} catch(CytoContainerException cee){
				System.err.println("\t" + cee.getMessage());
			}
		}
		
		while(true){
			System.out.print("Scope of data that should be sent to service (" + ServiceInputDefinition.SCOPE_SET +"): ");
			try {
				inputDef.setScope(userIn.nextLine());
				break;
			} catch(CytoContainerException cee){
				System.err.println("\t" + cee.getMessage());
			}
		}
		if (inputDef.getType().equals(ServiceInputDefinition.NETWORK_TYPE)){
			// prompt user for inputNetwork data
			InputNetwork inputNet = new InputNetwork();
			while (true){
				System.out.print("Model (" + InputNetwork.MODEL_SET + "): ");
				try {
					inputNet.setModel(userIn.nextLine());
				} catch(CytoContainerException cce){
					System.err.println("\t" + cce.getMessage());
				}
				break;
			}
			while (true){
				System.out.print("Format (" + InputNetwork.FORMAT_SET + "): ");
				try{
					inputNet.setFormat(userIn.nextLine());
					break;
				} catch(CytoContainerException cce){
					System.err.println("\t" + cce.getMessage());
				}
			}
			inputDef.setInputNetwork(inputNet);
		} else {
			// prompt user for input columns
			List<InputColumn> inputCols = new ArrayList<>();
			InputColumn inputCol = null;
			String res = null;
			while(true){
				inputCol = new InputColumn();
				System.out.print("InputColumn columnName(if set, other args ignored): ");
				res = userIn.nextLine();
				if (res != null && !res.isBlank()){
					inputCol.setColumnName(res);
					inputCols.add(inputCol);
					System.out.print("Add another InputColumn (y/n)? ");
					String answer = userIn.nextLine();
					if (answer.startsWith("n") || answer.startsWith("N")){
						break;
					}
					continue;
				}
				System.out.print("InputColumn name: ");
				inputCol.setName(userIn.nextLine());
				
				System.out.print("InputColumn description: ");
				inputCol.setDescription(userIn.nextLine());
				
				while(true){
					System.out.print("InputColumn dataType (" + CXDataType.COLUMN_FILTER_SET +"): ");
					try {
						res = userIn.nextLine();
						if (res != null && !res.isBlank()){
							inputCol.setDataType(res);
						}
						break;
					} catch(CytoContainerException cce){
						System.err.println("\t" + cce.getMessage());
					}
				}
				while (true){
					System.out.print("InputColumn allowMultipleSelection (true/false): ");
					String aMSelect = userIn.nextLine();
					try {
						inputCol.setAllowMultipleSelection(Boolean.parseBoolean(aMSelect));
						break;
					} catch(Exception ex){
						System.err.println("\t" + ex.getMessage());
					}
				}
				System.out.print("InputColumn defaultColumnName: ");
				inputCol.setDefaultColumnName(userIn.nextLine());
				
				inputCols.add(inputCol);
				System.out.print("Add another InputColumn (y/n)? ");
				String answer = userIn.nextLine();
				if (answer.startsWith("n") || answer.startsWith("N")){
					break;
				}
			}
			inputDef.setInputColumns(inputCols);
		}
		algo.setServiceInputDefinition(inputDef);
		CytoContainerAlgorithms algos = new CytoContainerAlgorithms();
		LinkedHashMap<String, CytoContainerAlgorithm> aMap = new LinkedHashMap<>();
		aMap.put(algo.getName(), algo);
		algos.setAlgorithms(aMap);
		ObjectMapper mappy = new ObjectMapper();
		System.out.println("\n");
        return mappy.writerWithDefaultPrettyPrinter().writeValueAsString(algos);
		
	}
	
	public static CytoContainerParameter promptUserForParameter(Scanner userIn) {
		CytoContainerParameter param = new CytoContainerParameter();
		System.out.print("Parameter displayName: ");
		param.setDisplayName(userIn.nextLine());
		System.out.print("Parameter description: ");
		param.setDisplayName(userIn.nextLine());
		System.out.print("Parameter displayName: ");
		param.setDisplayName(userIn.nextLine());
		promptForParameterType(param, userIn);
		promptForValidationType(param, userIn);
		return param;
	}
	
	public static final void promptForParameterType(CytoContainerParameter param, Scanner userIn){
		while (true){
			System.out.print("Parameter type (" + CytoContainerParameter.TYPE_SET + "): ");
			try {
				param.setType(userIn.nextLine());
				return;
			} catch(CytoContainerException cce){
				System.err.println("\t" + cce.getMessage());
			}
		}
	}
	
	public static final void promptForValidationType(CytoContainerParameter param, Scanner userIn){
		while (true){
			System.out.print("Parameter validationType (" + CytoContainerParameter.VALIDATION_SET + "): ");
			try {
				param.setValidationType(userIn.nextLine());
				return;
			} catch(CytoContainerException cce){
				System.err.println("\t" + cce.getMessage());
			}
		}
	}
    /**
     * Generates an example community detection algorithms json string
     * with actual docker images
     * @return json string
     * @throws Exception If there is an error
     */
    public static String generateExampleCytoContainerAlgorithms() throws Exception {
        
		Map<String, String> pFlagMap = null;
         LinkedHashMap<String, CytoContainerAlgorithm> algoSet = new LinkedHashMap<>();
        //gprofiler term mapper
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
		cda.setCyWebAction(CytoContainerAlgorithm.ADD_TABLES_ACTION);
		cda.setVersion("1.0.0");
		CyWebMenuItem toolsMenuItem = new CyWebMenuItem();
		toolsMenuItem.setRoot("Tools");
		CyWebMenuItemPath cdMenuItemPath = new CyWebMenuItemPath();
		cdMenuItemPath.setName("Community Detection");
		cdMenuItemPath.setGravity(0);
		CyWebMenuItemPath enMenuItemPath = new CyWebMenuItemPath();
		enMenuItemPath.setName("Enrichment");
		enMenuItemPath.setGravity(1);
		CyWebMenuItemPath gpMenuItemPath = new CyWebMenuItemPath();
		gpMenuItemPath.setName("gProfiler");
		gpMenuItemPath.setGravity(2);
		toolsMenuItem.setPath(Arrays.asList(cdMenuItemPath, enMenuItemPath, gpMenuItemPath));
		cda.setCyWebMenuItem(toolsMenuItem);
		cda.setName("gprofilersingletermv2");
		cda.setHiddenParameters(Arrays.asList("--someflag", "flagvalue"));
        algoSet.put(cda.getName(), cda);
        cda.setDescription("Uses gprofiler to find best term below pvalue cut off"
                + "using a list of genes as input");
        cda.setDockerImage("coleslawndex/gprofilersingletermv2");
		ServiceInputDefinition sda = new ServiceInputDefinition();
		sda.setType(ServiceInputDefinition.NODES_TYPE);
		SelectedDataParameter nodeparam = new SelectedDataParameter();
		InputColumn cdaInputColumn = new InputColumn();
		cdaInputColumn.setDefaultColumnName("CD_MemberList");
		cdaInputColumn.setDataType("string");
		sda.setInputColumns(Arrays.asList(cdaInputColumn));
		cda.setServiceInputDefinition(sda);
		

        AlgorithmParameter cp = new AlgorithmParameter();
        cp.setDescription("Maximum pvalue to allow for results");
        cp.setType("text");
        cp.setDefaultValue("0.00001");
        cp.setDisplayName("Maximum Pvalue");
        cp.setValidationHelp("Must be a number");
        cp.setValidationType("number");
        HashSet<AlgorithmParameter> cpSet = new HashSet<>();
        cpSet.add(cp);
		pFlagMap = new HashMap<>();
		pFlagMap.put(cp.getDisplayName(), "--maxpval");
        cda.setParameterFlagMap(pFlagMap);
        cda.setParameters(cpSet);
        
        //louvain
        CytoContainerAlgorithm cdb = new CytoContainerAlgorithm();
        cdb.setName("louvain");
		cdb.setCyWebAction(CytoContainerAlgorithm.ADD_NETWORKS_ACTION);

		CyWebMenuItem toolsCdbMenuItem = new CyWebMenuItem();
		toolsCdbMenuItem.setRoot("Tools");
		CyWebMenuItemPath cdCdbMenuItemPath = new CyWebMenuItemPath();
		cdCdbMenuItemPath.setName("Community Detection");
		cdCdbMenuItemPath.setGravity(0);
		CyWebMenuItemPath loCdbMenuItemPath = new CyWebMenuItemPath();
		loCdbMenuItemPath.setName("Louvain");
		loCdbMenuItemPath.setGravity(1);
		toolsCdbMenuItem.setPath(Arrays.asList(cdCdbMenuItemPath, loCdbMenuItemPath));
		cdb.setCyWebMenuItem(toolsCdbMenuItem);
        algoSet.put(cdb.getName(), cdb );
        cdb.setDescription("Runs louvain community detection algorithm");
        cdb.setDockerImage("ecdymore/slouvaintest");

        cdb.setVersion("2.0.0");
        
        cp = new AlgorithmParameter();
        cp.setDescription("If set, generate directed graph");
        cp.setDisplayName("Generate directed graph");
        cp.setType("checkBox");
        cpSet = new HashSet<>();
        cpSet.add(cp);
		pFlagMap = new HashMap<>();
		pFlagMap.put(cp.getDisplayName(), "--directed");
        cdb.setParameterFlagMap(pFlagMap);
		InputNetwork iNet = new InputNetwork();
		iNet.setFormat(InputNetwork.CX_FORMAT);
		iNet.setModel(InputNetwork.NETWORK_MODEL);
        
		ServiceInputDefinition sdb = new ServiceInputDefinition();
		sdb.setType(ServiceInputDefinition.NETWORK_TYPE);
		sdb.setScope(ServiceInputDefinition.ALL_SCOPE);
		sdb.setInputNetwork(iNet);
		cdb.setServiceInputDefinition(sdb);
        
		cp = new AlgorithmParameter();
        cp.setDescription("Configuration model which must be one of following:"
                + ": RB, RBER, CPM, Suprise, Significance, Default");
        cp.setDisplayName("Configuration Model");
        cp.setType("value");
        cp.setDefaultValue("Default");
        cp.setValidationType("string");
        cp.setValidationHelp("Must be one of following: RB, RBER, CPM, Suprise, Significance, Default");
        cp.setValidationRegex("RB|RBER|CPM|Suprise|Significance|Default");
        cpSet.add(cp);
		pFlagMap = new HashMap<>();
		pFlagMap.put(cp.getDisplayName(), "--configmodel");
        cdb.setParameterFlagMap(pFlagMap);
        cdb.setParameters(cpSet);
        CytoContainerAlgorithms algos = new CytoContainerAlgorithms();
        algos.setAlgorithms(algoSet);
        ObjectMapper mappy = new ObjectMapper();
        return mappy.writerWithDefaultPrettyPrinter().writeValueAsString(algos);
    }
   
    /**
     * Generates example Configuration file writing to standard out
     * @throws Exception if there is an error
     * @return example configuration
     */
    public static String generateExampleConfiguration() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# Example configuration file for Cytoscape Container REST service\n\n");
        
        sb.append("# Sets Community Detection task directory where results from queries are stored\n");
        sb.append(Configuration.TASK_DIR + " = /tmp/tasks\n\n");
        
        sb.append("# Sets number of workers to use to run tasks\n");
        sb.append(Configuration.NUM_WORKERS + " = 1\n\n");
        
        sb.append("# Docker command to run\n");
        sb.append(Configuration.DOCKER_CMD + " = docker\n\n");
        
        sb.append("# Algorithm/ docker command timeout in seconds. Anything taking longer will be killed\n");
        sb.append(Configuration.ALGORITHM_TIMEOUT + " = 180\n\n");
        
        sb.append("# Path to file containing json of algorithms\n");
        sb.append(Configuration.ALGORITHM_MAP + " = " + CD_ALGORITHMS_FILE + "\n\n");
        
        sb.append("# Mount options, if unset :ro is used (podman may require :ro,z)\n");
        sb.append(Configuration.MOUNT_OPTIONS + " = :ro\n\n");
        
        sb.append("# Sets HOST URL prefix (value is prefixed to Location header when query is invoked. Can be commented out)\n");
        sb.append("# " + Configuration.HOST_URL + " = http://localhost:8081\n\n");
        
        sb.append("# If set, overrides title shown in Swagger and openapi.json\n");
        sb.append("# " + Configuration.SWAGGER_TITLE + " = my service\n\n");
        
        sb.append("# If set, overrides description shown in Swagger and openapi.json\n");
        sb.append("# " + Configuration.SWAGGER_DESC + " = description of my service\n\n");
        
        sb.append("# Sets directory where log files will be written for Jetty web server\n");
        sb.append(App.RUNSERVER_LOGDIR + " = /tmp/logs\n\n");
        
        sb.append("# Sets port Jetty web service will be run under\n");
        sb.append(App.RUNSERVER_PORT + " = 8081\n\n");
        
        sb.append("# Sets Jetty Context Path for Cytoscape Container Service\n");
        sb.append(Configuration.RUNSERVER_CONTEXTPATH + " = /cy\n\n");
        
        sb.append("# Valid log levels DEBUG INFO WARN ERROR ALL\n");
        sb.append(App.RUNSERVER_LOGLEVEL + " = INFO\n");

        return sb.toString();
    }
}
