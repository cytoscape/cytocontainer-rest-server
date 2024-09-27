package org.cytoscape.cytocontainer.rest.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.cytoscape.cytocontainer.rest.engine.util.CytoContainerRequestValidatorImpl;
import org.cytoscape.cytocontainer.rest.model.AlgorithmCustomParameter;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithms;
import org.cytoscape.cytocontainer.rest.model.Parameter;
import org.cytoscape.cytocontainer.rest.model.ServiceAlgorithm;
import org.cytoscape.cytocontainer.rest.model.ServiceMetaData;
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

    private int _numWorkers;
    private String _taskDir;
    private String _dockerCmd;
    private CytoContainerAlgorithms _algorithms;
    private CytoContainerRequestValidator _validator;
	private ServiceMetaData _metaData;
    
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
		_metaData = getMetaData(config.getName(), config.getDescription(),
				config.getInputDataFormat(), config.getOutputDataFormat());
       
    }
	
	private ServiceMetaData getMetaData(final String name, final String description,
			final String inputDataFormat, final String outputDataFormat){
		ServiceMetaData metaData = new ServiceMetaData();
		ArrayList<ServiceAlgorithm> algos = new ArrayList<>();
		if (_algorithms != null && _algorithms.getAlgorithms() != null){
			for (String key : _algorithms.getAlgorithms().keySet()){
				CytoContainerAlgorithm cda = _algorithms.getAlgorithms().get(key);
				
					algos.add(getServiceAlgorithm(cda));
			}
		}
		//metaData.setAlgorithms(algos);
		metaData.setName(name);
		metaData.setDescription(description);
		
		return metaData;
	}
	
	private ServiceAlgorithm getServiceAlgorithm(CytoContainerAlgorithm cda){
		ServiceAlgorithm sa = new ServiceAlgorithm();
		sa.setDescription(cda.getDescription());
		sa.setDisplayName(cda.getDisplayName());
		sa.setVersion(cda.getVersion());
		sa.setName(cda.getName());
		HashSet<Parameter> parameters = new HashSet<>();
		if (cda.getParameterMap() != null){
			for (Parameter cp : cda.getParameterMap().values()){
				Parameter acp = new Parameter();
				acp.setDefaultValue(cp.getDefaultValue());
				acp.setDescription(cp.getDescription());
				acp.setName(cp.getDisplayName());
				acp.setMaxValue(cp.getMaxValue());
				acp.setMinValue(cp.getMinValue());
				acp.setType(cp.getType());
				acp.setValidationHelp(cp.getValidationHelp());
				acp.setValidationRegex(cp.getValidationRegex());
				acp.setValidationType(cp.getValidationType());
				parameters.add(acp);
			}
		}
		
		//sa.setParameters(parameters);
		return sa;
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
                _dockerCmd, _algorithms, _metaData, _validator);
        return engine;
    }
}
