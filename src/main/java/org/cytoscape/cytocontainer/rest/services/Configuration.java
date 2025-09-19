package org.cytoscape.cytocontainer.rest.services;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;

import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithms;
import org.cytoscape.cytocontainer.rest.engine.CytoContainerEngine;

/**
 * Contains configuration for Enrichment. The configuration
 is extracted by looking for a file under the environment
 variable COMMUNITY_DETECTION_CONFIG and if that fails defaults are
 used
 * @author churas
 */
public class Configuration {
    
    //public static final String APPLICATION_PATH = "/cytocontainer";
    public static final String V_ONE_PATH = "/v1";

    public static final String LEGACY_DIFFUSION_PATH = "/diffusion";
    public static final String COMMUNITY_DETECTION_CONFIG = "COMMUNITY_DETECTION_CONFIG";
    
	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	
    public static final String TASK_DIR = "cytocontainer.task.dir";
    public static final String HOST_URL = "cytocontainer.host.url";    
    public static final String NUM_WORKERS = "cytocontainer.number.workers";
    public static final String DOCKER_CMD = "cytocontainer.docker.cmd";
    public static final String ALGORITHM_CONF_DIR = "cytocontainer.algorithm.conf.dir";
    public static final String ALGORITHM_TIMEOUT = "cytocontainer.algorithm.timeout";

    public static final String MOUNT_OPTIONS = "cytocontainer.mount.options";
    public static final String DIFFUSION_ALGO = "cytocontainer.diffusion.algorithm";
    public static final String DIFFUSION_POLLDELAY = "cytocontainer.diffusion.polldelay";
	public static final String BYTES_OF_STDERR_TO_PARSE = "cytocontainer.bytes.of.stderr.toparse";
    public static final String SWAGGER_TITLE = "swagger.title";
    public static final String SWAGGER_DESC = "swagger.description";

    
    public static final String RUNSERVER_CONTEXTPATH = "runserver.contextpath";
    public static final String RUNSERVER_APP_PATH = "runserver.applicationpath";
    
    private static Configuration INSTANCE;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private static String _alternateConfigurationFile;
    private static CytoContainerEngine _communityEngine;
    private static String _taskDir;
    private static String _hostURL;
    private static String _dockerCmd;
    private static int _numWorkers;
	private static String _name;
	private static String _description;
	private static String _inputDataFormat;
	private static String _outputDataFormat;
    private static CytoContainerAlgorithms _algorithms;
    private static long _diffusionPollingDelay;
    private static long _timeOut;
    private String _mountOptions;
    private String _swaggerTitle;
    private String _swaggerDescription;
    private String _contextPath;
    private String _applicationPath;
	private long _bytesOfStdErrToParse;
    
    /**
     * Constructor that attempts to get configuration from properties file
     * specified via configPath
     */
    private Configuration(final String configPath) throws CytoContainerException
    {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configPath));
        }
        catch(FileNotFoundException fne){
            _logger.error("No configuration found at " + configPath, fne);
            throw new CytoContainerException("FileNotFound Exception when attempting to load " 
                    + configPath + " : " +
                    fne.getMessage());
        }
        catch(IOException io){
            _logger.error("Unable to read configuration " + configPath, io);
            throw new CytoContainerException("IOException when trying to read configuration file " + configPath +
                     " : " + io);
        }
        
        _taskDir = props.getProperty(Configuration.TASK_DIR, "/tmp");
        _numWorkers = Integer.parseInt(props.getProperty(Configuration.NUM_WORKERS, "1"));
        _hostURL = props.getProperty(Configuration.HOST_URL, "");
        _dockerCmd = props.getProperty(Configuration.DOCKER_CMD, "docker");
        _algorithms = getAlgorithms(props.getProperty(Configuration.ALGORITHM_CONF_DIR, null));
        _timeOut = Long.parseLong(props.getProperty(Configuration.ALGORITHM_TIMEOUT, "180"));
        _mountOptions = props.getProperty(Configuration.MOUNT_OPTIONS, ":ro");
        _swaggerTitle = props.getProperty(Configuration.SWAGGER_TITLE, null);
        _swaggerDescription = props.getProperty(Configuration.SWAGGER_DESC, null);
        _contextPath = props.getProperty(Configuration.RUNSERVER_CONTEXTPATH, "/cy");
        _applicationPath = props.getProperty(Configuration.RUNSERVER_APP_PATH, "/cytocontainer");
        _name = props.getProperty(Configuration.NAME, "Some App");
		_description = props.getProperty(Configuration.DESCRIPTION, "Some description");
		if (_hostURL.trim().isEmpty()){
            _hostURL = "";
        } else if (!_hostURL.endsWith("/")){
            _hostURL =_hostURL + "/";
        }
		_bytesOfStdErrToParse = Long.parseLong(props.getProperty(Configuration.BYTES_OF_STDERR_TO_PARSE, Long.toString(1024*1024*10)));
    }
    
    protected CytoContainerAlgorithms getAlgorithms(final String algoConfDir){
        if (algoConfDir == null){
            _logger.error("Path to algorithms configuration dir is null");
            return null;
        }
		File algoFileDir = new File(algoConfDir);

		if (algoFileDir.exists() == false){
			_logger.error(algoConfDir + " does not exist");
			return null;
		}

		if (algoFileDir.isDirectory() == false){
			_logger.error(algoConfDir + " is not a directory");
			return null;
		}
		
		CytoContainerAlgorithms cca = new CytoContainerAlgorithms();
		LinkedHashMap<String, CytoContainerAlgorithm> algoMap = new LinkedHashMap<>();
		String[] extensions = new String[] { "json" };
		CytoContainerAlgorithm algo = null;
		String algoName = null;
		for (File f : FileUtils.listFiles(algoFileDir, extensions, true)){
	        ObjectMapper mapper = new ObjectMapper();
			try {
				algo =  mapper.readValue(f, CytoContainerAlgorithm.class);
				algoName = FilenameUtils.removeExtension(f.getName());
			}
			catch(IOException io){
				  _logger.error("Skipping, error parsing json: " + f.getAbsolutePath() + " : " + io.getMessage());
				  continue;
			}
			if (algoMap.containsKey(algoName) == true){
				_logger.error("Skipping, algorithm with matching name already found: " + f.getAbsolutePath());
				continue;
			}
			algoMap.put(algoName, algo);
		}
        cca.setAlgorithms(algoMap);
        return cca;
    }
        
    protected void setCytoContainerEngine(CytoContainerEngine ee){
        _communityEngine = ee;
    }
    public CytoContainerEngine getCytoContainerEngine(){
        return _communityEngine;
    }

    /**
     * Gets alternate URL prefix for the host running this service.
     * @return String containing alternate URL ending with / or empty
     *         string if not is set
     */
    public String getHostURL(){
        return _hostURL;
    }
	
    /**
     * Gets directory where enrichment task results should be stored
     * @return task directory
     */
    public String getTaskDirectory(){
        return _taskDir;
    }
    
    /**
     * Gets number of workers to process tasks
     * @return number of workers
     */
    public int getNumberWorkers(){
        return _numWorkers;
    }
    
    /**
     * Algorithm timeout
     * @return seconds
     */
    public long getAlgorithmTimeOut(){
        return _timeOut;
    }
    
    /**
     * Full path to docker command
     * @return docker command
     */
    public String getDockerCommand(){
        return _dockerCmd;
    }
    
    /**
     * Algorithms available from this service
     * @return algorithms
     */
    public CytoContainerAlgorithms getAlgorithms(){
        return _algorithms;
    }
    
    /**
     * Mount options needed by containers such as docker or pod
     * @return usually :ro or :ro,z
     */
    public String getMountOptions(){
        return _mountOptions;
    }
    
    /**
     * Alternate swagger title
     * @return swagger title
     */
    public String getSwaggerTitle(){
        return _swaggerTitle;
    }
    
    /**
     * Alternate swagger description
     * @return swagger description
     */
    public String getSwaggerDescription(){
        return _swaggerDescription;
    }

	public static String getName() {
		return _name;
	}

	public static void setName(String _name) {
		Configuration._name = _name;
	}

	public static String getDescription() {
		return _description;
	}

	public static void setDescription(String _description) {
		Configuration._description = _description;
	}

	public static String getInputDataFormat() {
		return _inputDataFormat;
	}

	public static void setInputDataFormat(String _inputDataFormat) {
		Configuration._inputDataFormat = _inputDataFormat;
	}

	public static String getOutputDataFormat() {
		return _outputDataFormat;
	}

	public static void setOutputDataFormat(String _outputDataFormat) {
		Configuration._outputDataFormat = _outputDataFormat;
	}
    
	
    /**
     * Run server context path
     * @return context path
     */
    public String getRunServerContextPath(){
        return _contextPath;
    }
    
    /**
     * Application path
     * @return path
     */
    public String getRunServerApplicationPath(){
        return _applicationPath;
    }
    
    /**
     * URL for Swagger server
     * @return URL
     */
    public String getSwaggerServer(){
        return getRunServerContextPath() + getRunServerApplicationPath();
    }
    
    /**
     * Gets the polling delay for diffusion which denotes how  
     * long service should wait before checking if diffusion task
     * is complete
     * @return time in milliseconds
     */
    public long getDiffusionPollingDelay(){
		return _diffusionPollingDelay;
    }
	
	/**
	 * Defines number of bytes to examine from end of stderr file
	 * when looking for message and progress data
	 * 
	 * @return 
	 */
	public long getNumberOfBytesToParseFromStdErrorFile(){
		return this._bytesOfStdErrToParse;
	}
    
    /**
     * Gets singleton instance of configuration
     * @return {@link org.cytoscape.cytocontainer.rest.services.Configuration} object with configuration loaded
     * @throws CytoContainerException if there was a problem reading the configuration
     */
    public static Configuration getInstance() throws CytoContainerException
    {
    	if (INSTANCE == null)  { 
            
            try {
                String configPath = null;
                if (_alternateConfigurationFile != null){
                    configPath = _alternateConfigurationFile;
                    _logger.info("Alternate configuration path specified: " + configPath);
                } else {
                    try {
                        configPath = System.getenv(Configuration.COMMUNITY_DETECTION_CONFIG);
                    } catch(SecurityException se){
                        _logger.error("Caught security exception ", se);
                    }
                }
                if (configPath == null){
                    InitialContext ic = new InitialContext();
                    configPath = (String) ic.lookup("java:comp/env/" + Configuration.COMMUNITY_DETECTION_CONFIG); 

                }
                INSTANCE = new Configuration(configPath);
            } catch (NamingException ex) {
                _logger.error("Error loading configuration", ex);
                throw new CytoContainerException("NamingException encountered. Error loading configuration: " 
                         + ex.getMessage());
            }
    	} 
        return INSTANCE;
    }
    
    /**
     * Reloads configuration
     * @return {@link org.cytoscape.cytocontainer.rest.services.Configuration} object
     * @throws CytoContainerException if there was a problem reading the configuration
     */
    public static Configuration reloadConfiguration() throws CytoContainerException  {
        INSTANCE = null;
        return getInstance();
    }
    
    /**
     * Lets caller set an alternate path to configuration. Added so the command
     * line application can set path to configuration and it makes testing easier
     * This also sets the internal instance object to {@code null} so subsequent
     * calls to {@link #getInstance() } will load a new instance with this configuration
     * @param configFilePath - Path to configuration file
     */
    public static void  setAlternateConfigurationFile(final String configFilePath) {
    	_alternateConfigurationFile = configFilePath;
        INSTANCE = null;
    }
}
