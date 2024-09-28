package org.cytoscape.cytocontainer.rest.engine;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.cytoscape.cytocontainer.rest.engine.util.CytoContainerRequestValidatorImpl;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithms;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.cytoscape.cytocontainer.rest.services.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cytoscape.cytocontainer.rest.engine.util.CytoContainerRequestValidator;

/**
 * Factory to create {@link org.cytoscape.cytocontainer.rest.engine.CytoContainerEngine} objects
 * 
 * @author churas
 */
public class CytoContainerEngineFactory {
    
    static Logger _logger = LoggerFactory.getLogger(CytoContainerEngineFactory.class);

    private final int _numWorkers;
    private final String _taskDir;
    private final String _dockerCmd;
    private final CytoContainerAlgorithms _algorithms;
    private final CytoContainerRequestValidator _validator;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param config Configuration containing number of workers, task directory, docker command,
     *               and algorithms.
     */
    public CytoContainerEngineFactory(Configuration config){
        
        _numWorkers = config.getNumberWorkers();
        _taskDir = config.getTaskDirectory();
        _dockerCmd = config.getDockerCommand();
        _algorithms = config.getAlgorithms();
        _validator = new CytoContainerRequestValidatorImpl();
 
    }

    /**
     * Creates CytoContainerEngine with a fixed threadpool to process requests
     * @throws CytoContainerException if there is an error
     * @return {@link org.cytoscape.cytocontainer.rest.engine.CytoContainerEngine} object 
     *         ready to service requests
     */
    public CytoContainerEngine getCytoContainerEngine() throws CytoContainerException {
        _logger.debug("Creating executor service with: " + Integer.toString(_numWorkers) + " workers");
        ExecutorService es = Executors.newFixedThreadPool(_numWorkers);
		
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(es, _taskDir,
                _dockerCmd, _algorithms, _validator);
        return engine;
    }
}
