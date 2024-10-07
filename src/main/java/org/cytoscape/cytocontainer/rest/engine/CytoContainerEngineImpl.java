package org.cytoscape.cytocontainer.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.cytoscape.cytocontainer.rest.engine.util.DockerCytoContainerRunner;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithms;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResultStatus;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResult;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;
import org.cytoscape.cytocontainer.rest.model.ServerStatus;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerBadRequestException;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;

import org.cytoscape.cytocontainer.rest.services.CytoContainerHttpServletDispatcher;
import org.cytoscape.cytocontainer.rest.services.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cytoscape.cytocontainer.rest.engine.util.CytoContainerRequestValidator;
import org.cytoscape.cytocontainer.rest.model.Algorithm;
import org.cytoscape.cytocontainer.rest.model.Algorithms;

/**
 * Runs CytoContainer tasks 
 * @author churas
 */
public class CytoContainerEngineImpl implements CytoContainerEngine {

    public static final String CDREQUEST_JSON_FILE = "cdrequest.json";
    
    public static final String CDRESULT_JSON_FILE = "cdresult.json";
	
	// @TODO rethink what we want to name this
	public static final String RESULT_DATA_FILE = "stdout.txt";
    
    static Logger _logger = LoggerFactory.getLogger(CytoContainerEngineImpl.class);

    private String _taskDir;
    private boolean _shutdown;
    private ExecutorService _executorService;
    private ConcurrentHashMap<String, Future> _futureTaskMap;
    private AtomicInteger _completedTasks;
    private AtomicInteger _queuedTasks;
    private AtomicInteger _canceledTasks;
    private CytoContainerAlgorithms _algorithms;
	
    private CytoContainerRequestValidator _validator;
    private String _dockerCmd;
        
    /**
     * This should be a map of <query UUID> => EnrichmentQueryResults object
     */
    private ConcurrentHashMap<String, CytoContainerResult> _results;

    private long _threadSleep = 10;
	
    /**
     * Constructor 
     * @param es Executor Service to run tasks
     * @param taskDir Base directory for tasks
     * @param dockerCmd Docker command to run
     * @param algorithms Algorithms that can be run by this object
     * @param validator Validates requestsß
     */
    public CytoContainerEngineImpl(ExecutorService es,
            final String taskDir,
            final String dockerCmd,
            final CytoContainerAlgorithms algorithms,
            final CytoContainerRequestValidator validator){
        _executorService = es;
        _shutdown = false;
        _futureTaskMap = new ConcurrentHashMap<>();
        _taskDir = taskDir;
        _dockerCmd = dockerCmd;
        _algorithms = algorithms;
        _validator = validator;
        _results = new ConcurrentHashMap<>();
        _completedTasks = new AtomicInteger(0);
        _queuedTasks = new AtomicInteger(0);
        _canceledTasks = new AtomicInteger(0);
    }
    
    /**
     * Sets milliseconds thread should sleep if no work needs to be done.
     * @param sleepTime time in milliseconds
     */
    public void updateThreadSleepTime(long sleepTime){
        _threadSleep = sleepTime;
    }

    protected void threadSleep(){
        try {
            Thread.sleep(_threadSleep);
        }
        catch(InterruptedException ie){

        }
    }
    
    /**
     * Processes any query tasks, looping until {@link #shutdown()} is invoked
     */
    @Override
    public void run() {
        while(_shutdown == false){
            Future f;
            String taskId;
            int queuedCount = 0;
            Iterator<String> idItr = _futureTaskMap.keySet().iterator();
            while(idItr.hasNext()){
                taskId = idItr.next();
                
                f = _futureTaskMap.get(taskId);
                if (f == null){
                    continue;
                }
                if (f.isCancelled()){
                    _futureTaskMap.remove(taskId);
                    _canceledTasks.incrementAndGet();
                } else if (f.isDone()){
                    _logger.debug("Found a completed or failed task");
                    try {
                        CytoContainerResult cdr = (CytoContainerResult) f.get();
                        saveCytoContainerResultToFilesystem(cdr);
                        _completedTasks.incrementAndGet();
                    } catch (InterruptedException ex) {
                        _logger.error("Got interrupted exception", ex);
                    } catch (ExecutionException ex) {
                        _logger.error("Got execution exception", ex);
                    } catch (CancellationException ex){
                        _logger.error("Got cancellation exception", ex);
                    }
                    _futureTaskMap.remove(taskId);
                } else {
                    queuedCount++;
                }
            }
            if (_queuedTasks.get() != queuedCount){
                _queuedTasks.set(queuedCount);
            }
            threadSleep();
        }
        _logger.debug("Shutdown was invoked");
        logServerStatus(null);
    }

    @Override
    public void shutdown() {
        _shutdown = true;
    }
    
    /**
     * Calls {@link #getServerStatus() } and dumps the status of the server as
     * a JSON string to the info level of the logger for this class
     * @param ss status to log
     */
    protected void logServerStatus(final ServerStatus ss){
        try {
            final ServerStatus sStat;
            if (ss != null){
                sStat = ss;
            } else {
                sStat = this.getServerStatus(null);
            }
           
           ObjectMapper mapper = new ObjectMapper();
           _logger.info("ServerStatus: " + mapper.writeValueAsString(sStat));
        }catch(Exception ex){
            _logger.error("error trying to log server status", ex);
        }
    }
    
    protected String getCytoContainerResultFilePath(final String id){
        return this._taskDir + File.separator + id + File.separator + CytoContainerEngineImpl.CDRESULT_JSON_FILE;
    }
	
	protected String getCytoContainerResultDataFilePath(final String id){
		return this._taskDir + File.separator + id + File.separator + CytoContainerEngineImpl.RESULT_DATA_FILE;
    }

    protected void saveCytoContainerResultToFilesystem(final CytoContainerResult cdr){
        if (cdr == null){
            _logger.error("Received a null result, unable to save");
            return;
        }
        logResult(cdr);
        File destFile = new File(getCytoContainerResultFilePath(cdr.getId()));
        ObjectMapper mappy = new ObjectMapper();
        try (FileOutputStream out = new FileOutputStream(destFile)){
            mappy.writeValue(out, cdr);
        } catch(IOException io){
            _logger.error("Caught exception writing " + destFile.getAbsolutePath(), io);
        }
        _results.remove(cdr.getId());
    }
    
    protected void logResult(final CytoContainerResult result){
	if (result == null){
	    return;
	}
	StringBuilder sb = new StringBuilder();
	sb.append("Result id: ");
	sb.append(result.getId() == null ? "NULL" : result.getId());
	sb.append(" ");
	sb.append("start time: ");
	sb.append(Long.toString(result.getStartTime()));
	sb.append(" wall time: ");
	sb.append(Long.toString(result.getWallTime()));
	sb.append(" status: ");
	sb.append(result.getStatus() == null ? "NULL" : result.getStatus());
	sb.append(" message: ");
	sb.append(result.getMessage() == null ? "NULL" : result.getMessage());
	_logger.info(sb.toString());
    }

    protected CytoContainerResult getCytoContainerResultFromDbOrFilesystem(final String id){
        ObjectMapper mappy = new ObjectMapper();
        File cdrFile = new File(getCytoContainerResultFilePath(id));
        if (cdrFile.isFile() == false){
            _logger.debug(cdrFile.getAbsolutePath() + " is not a file. "
		    + "Will attempt to retreive from in memory store");
            return _results.get(id);
        }
        try {
            return mappy.readValue(cdrFile, CytoContainerResult.class);
        }catch(IOException io){
            _logger.error("Caught exception trying to load " + cdrFile.getAbsolutePath(), io);
        }
        return _results.get(id);
    }
    
    /**
     * Request a algorithm be run. This is the call that
     * should be coming from the rest POST endpoint
     * @param request The request
     * @return UUID as string
     * @throws CytoContainerBadRequestException if request is invalid
     * @throws CytoContainerException If there is a server side error
     */
    @Override
    public String request(final String algorithm, CytoContainerRequest request) throws CytoContainerException,
            CytoContainerBadRequestException {

        if (request == null){ 
            throw new CytoContainerBadRequestException("Request is null");
        }
        if (request.getAlgorithm() == null){
			if (algorithm == null){
				throw new CytoContainerBadRequestException("No algorithm specified");
			}
			request.setAlgorithm(algorithm);
        }
        
        if (_algorithms == null || _algorithms.getAlgorithms() == null){
            throw new CytoContainerException("No algorithms are available to run in service");
        }
        
        if (_algorithms.getAlgorithms().containsKey(request.getAlgorithm()) == false){
            throw new CytoContainerBadRequestException(request.getAlgorithm() 
                    + " is not a valid algorithm");
        }
        
        CytoContainerAlgorithm cda = _algorithms.getAlgorithms().get(request.getAlgorithm());
        ErrorResponse er = this._validator.validateRequest(cda, request);
        if (er != null){
            throw new CytoContainerBadRequestException("Validation failed", er);
        }
        
        String id = UUID.randomUUID().toString();

        CytoContainerResult cdr = new CytoContainerResult(System.currentTimeMillis());
        cdr.setStatus(CytoContainerResult.SUBMITTED_STATUS);
        cdr.setId(id);
        _results.put(id, cdr);
        logRequest(request, id);
        String dockerImage = cda.getDockerImage();
		Map<String, String> combinedParams = getParametersCombinedWithHiddenParameters(cda, request.getParameters());
        try {
            DockerCytoContainerRunner task = new DockerCytoContainerRunner(id, request, cdr.getStartTime(),
            _taskDir, _dockerCmd, dockerImage, combinedParams,
                    Configuration.getInstance().getAlgorithmTimeOut(),
            TimeUnit.SECONDS,
            Configuration.getInstance().getMountOptions(),
					false, null);
            _futureTaskMap.put(id, _executorService.submit(task));
            return id;
        } catch(Exception ex){
            throw new CytoContainerException(ex.getMessage());
        }
    }
	
	private Map<String, String> getParametersCombinedWithHiddenParameters(CytoContainerAlgorithm algo, Map<String, String> params) throws CytoContainerException {
		if (algo.getHiddenParameters() == null && params == null){
			return null;
		}
		Map<String, String> pMap = new LinkedHashMap<>();
		Map<String, String> paramFlagMap = algo.getParameterFlagMap();
		if (paramFlagMap != null && params != null){
			for (String key : params.keySet()){
				if (!paramFlagMap.containsKey(key)){
					throw new CytoContainerException("'" + key + "' parameter not found in algorithm. skipping...");
				}
				pMap.put(paramFlagMap.get(key), params.get(key));
			}
		}
		if (algo.getHiddenParameters() != null){
			for (String p : algo.getHiddenParameters()){
				pMap.put(p, "");
			}
		}
		return pMap;
	}
    
    private void logRequest(final CytoContainerRequest request,
	    final String id){
	if (request == null){
	    return;
	}
	StringBuilder sb = new StringBuilder();
	sb.append("Request id: ");
	sb.append(id == null ? "NULL" : id);
	sb.append(" to run ( ");
	sb.append(request.getAlgorithm() == null ? "NULL" : request.getAlgorithm());
	
        sb.append(" ) ");
	
	if (request.getData() == null){
	    sb.append(" with NO data");
	}
	else {
	    sb.append(" with data of type ");
	    sb.append(request.getData().getNodeType().toString());
	}
	
	Map<String, String> custParams = request.getParameters();
	String val = null;
	if (custParams != null){
	    sb.append(" and custom parameters: ");
	    for (String key : custParams.keySet()){
		sb.append(key).append("=>");
		val = custParams.get(key);
		sb.append(val == null ? "NULL" : val);
		sb.append(" ");
	    }
	}
	_logger.info(sb.toString());
    }

    /**
     * Gets result with given {@code id}
     * @param id Id of task
     * @return The result
     * @throws CytoContainerException If id is {@code null} or no task is found
     */
    @Override
    public CytoContainerResult getResult(final String algorithm, String id) throws CytoContainerException {
        if (id == null){
            throw new CytoContainerException("Id is null");
        }
        
        CytoContainerResult cdr = getCytoContainerResultFromDbOrFilesystem(id);
        if (cdr == null){
            throw new CytoContainerException("No task with id of " + id + " found");
        }
        return cdr;
    }

	@Override
	public InputStream getResultData(final String algorithm, String id) throws CytoContainerException {
		File dataFile = new File(this.getCytoContainerResultDataFilePath(id));
		CytoContainerResult cdr = getCytoContainerResultFromDbOrFilesystem(id);
        if (cdr == null){
            throw new CytoContainerException("No task with id of " + id + " found");
        }
		

		while (cdr.getProgress() < 100){
			try {
				Thread.sleep(300L);
			} catch(InterruptedException ie){
				
			}
			
			cdr = getCytoContainerResultFromDbOrFilesystem(id);
		}
		
		if (cdr.getStatus().equals(CytoContainerResult.COMPLETE_STATUS)){

			try {
				if (dataFile.exists() || dataFile.length() > 0){
					return new FileInputStream(dataFile);
				}
			} catch(FileNotFoundException fe){
				_logger.error("File not found", fe);
			}
		}
		return null;
	}

    /**
     * Gets status of task with given {@code id}
     * @param id Id of task
     * @return The result
     * @throws CytoContainerException If id is {@code null} or no task is found
     */
    @Override
    public CytoContainerResultStatus getStatus(final String algorithm, final String id) throws CytoContainerException {
        if (id == null){
            throw new CytoContainerException("Id is null");
        }
        
        CytoContainerResult cdr = getCytoContainerResultFromDbOrFilesystem(id);
        if (cdr == null){
            throw new CytoContainerException("No task with id of " + id + " found");
        }
        return new CytoContainerResultStatus(cdr);
    }

    /**
     * Deletes task with {@code id} from internally memory and from filesystem
     * if found there
     * 
     * @param id Id of task to delete
     * @throws CytoContainerException if {@code id} is null
     */
    @Override
    public void delete(final String algorithm, String id) throws CytoContainerException {
        if (id == null){
            throw new CytoContainerException("id is null");
        }
        _logger.debug("Deleting task " + id);
        if (_results.containsKey(id) == true){
            _results.remove(id);
        }
        Future f = _futureTaskMap.get(id);
        if (f != null){
            _logger.info("Delete invoked, canceling task: " + id +
		    " result of cancel(): " +
                    Boolean.toString(f.cancel(true)));
        }
        File thisTaskDir = new File(this._taskDir + File.separator + id);
        if (thisTaskDir.exists() == false){
            return;
        }
        _logger.debug("Attempting to delete task from filesystem: " + thisTaskDir.getAbsolutePath());
        if (FileUtils.deleteQuietly(thisTaskDir) == false){
            _logger.error("There was a problem deleting the directory: " + thisTaskDir.getAbsolutePath());
        }
    }

	@Override
	public Algorithm getMetaData(final String algorithm) throws CytoContainerException {
		if (_algorithms == null){
            throw new CytoContainerException("No Algorithms found");
        }
		if (algorithm == null){
			throw new CytoContainerException("Algorithm must be set");
		}
		if (_algorithms.getAlgorithms() == null){
			throw new CytoContainerException("No algorithms found in in db");
		}
		CytoContainerAlgorithm algo = _algorithms.getAlgorithms().get(algorithm);
		if (algo == null){
			throw new CytoContainerException("No algorithm matching name found");
		}
        return new Algorithm(algo);
	}
	
	@Override
	public Algorithms getAllAlgorithms() throws CytoContainerException {
		if (_algorithms == null){
            throw new CytoContainerException("No Algorithms found");
        }
		if (_algorithms.getAlgorithms() == null){
			throw new CytoContainerException("No algorithms found in in db");
		}
		return new Algorithms(_algorithms);
	}

    /**
     * Gets status of server
     * @return Server status
     * @throws CytoContainerException If there was an errorß
     */
    @Override
    public ServerStatus getServerStatus(final String algorithm) throws CytoContainerException {
        try {
            String version = "unknown";
            ServerStatus sObj = new ServerStatus();
            sObj.setStatus(ServerStatus.OK_STATUS);
            sObj.setVersion(CytoContainerHttpServletDispatcher.getVersion());
            OperatingSystemMXBean omb = ManagementFactory.getOperatingSystemMXBean();
            float unknown = (float)-1;
            float load = (float)omb.getSystemLoadAverage();
            sObj.setLoad(Arrays.asList(load, unknown, unknown));
            File taskDir = new File(this._taskDir);
            sObj.setPcDiskFull(100-(int)Math.round(((double)taskDir.getFreeSpace()/(double)taskDir.getTotalSpace())*100));
            sObj.setQueuedTasks(_queuedTasks.get());
            sObj.setCompletedTasks(_completedTasks.get());
            sObj.setCanceledTasks(_canceledTasks.get());
            logServerStatus(sObj);
            return sObj;
        } catch(Exception ex){
            _logger.error("ServerStatus error", ex);
            throw new CytoContainerException("Exception raised when getting ServerStatus: " + ex.getMessage());
        }
    }
}
