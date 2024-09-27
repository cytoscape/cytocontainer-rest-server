package org.cytoscape.cytocontainer.rest.engine.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.cytoscape.cytocontainer.rest.model.BinaryData;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResult;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs algorithm via commandline
 * @author churas
 */
public class DockerCytoContainerRunner implements Callable {

    
    static Logger _logger = LoggerFactory.getLogger(DockerCytoContainerRunner.class);

    public static final String INPUT_FILE = "input.txt";
    public static final String STD_OUT_FILE = "stdout.txt";
    public static final String STD_ERR_FILE = "stderr.txt";
    public static final String CMD_RUN_FILE = "cmdrun.sh";
    
    private String _id;
    private CytoContainerRequest _cdr;
    private String _dockerCmd;
    private String _dockerImage;
    private Map<String, String> _customParameters;
    private String _taskDir;
    private String _workDir;
    private long _startTime;
    private long _timeOut;
    private TimeUnit _timeUnit;
    private String _mountOptions;
    private String _inputFilePath;
	private boolean _outputIsBinary;
	private String _rawResultContentType;
 
    private CommandLineRunner _runner;
    
    /**
     * Constructor 
     * @param id id of task (should be a 37 char uuid string)
     * @param cdr The request to process
     * @param startTime Time task started in ms since epoch (1969)
     * @param taskDir Base directory for tasks (this task will be put into taskDir/id)
     * @param dockerCmd Command to run docker (/usr/bin/docker /bin/docker etc..)
     * @param dockerImage Docker image to run (hello-world)
     * @param customParameters Parameters to add to command line
     * @param timeOut Any task exceeding this time (in unit set by unit) will be killed
     * @param unit Unit to use for timeout
     * @param mountOptions flags used by container to mount filesystem
	 * @param outputIsBinary if true then output from container is binary
	 * @param rawResultContentType denotes mime type of result data
     * @throws Exception If there is an issue writing the input data from the cdr object
     */
    public DockerCytoContainerRunner(final String id,
            final CytoContainerRequest cdr, final long startTime, final String taskDir,
            final String dockerCmd, final String dockerImage,
            final Map<String, String> customParameters,
            final long timeOut,
            final TimeUnit unit,
            final String mountOptions,
			final boolean outputIsBinary,
			final String rawResultContentType) throws Exception{
        _id = id;
        _cdr = cdr;
        _dockerCmd = dockerCmd;
        _dockerImage = dockerImage;
        _customParameters = customParameters;
        _startTime = startTime;
        _taskDir = taskDir;
        _workDir = _taskDir + File.separator + _id;
        _timeOut = timeOut;
        _timeUnit = unit;
        if (mountOptions != null){
            _mountOptions = mountOptions;
        }
        else {
            _mountOptions = "";
        }
		
		_outputIsBinary = outputIsBinary;
		_rawResultContentType = rawResultContentType;

        _inputFilePath = writeInputFile();
       
        _runner = new CommandLineRunnerImpl();
        
    }
	
	/**
     * Constructor 
     * @param id id of task (should be a 37 char uuid string)
     * @param cdr The request to process
     * @param startTime Time task started in ms since epoch (1969)
     * @param taskDir Base directory for tasks (this task will be put into taskDir/id)
     * @param dockerCmd Command to run docker (/usr/bin/docker /bin/docker etc..)
     * @param dockerImage Docker image to run (hello-world)
     * @param customParameters Parameters to add to command line
     * @param timeOut Any task exceeding this time (in unit set by unit) will be killed
     * @param unit Unit to use for timeout
     * @param mountOptions flags used by container to mount filesystem
     * @throws Exception If there is an issue writing the input data from the cdr object
     */
    public DockerCytoContainerRunner(final String id,
            final CytoContainerRequest cdr, final long startTime, final String taskDir,
            final String dockerCmd, final String dockerImage,
            final Map<String, String> customParameters,
            final long timeOut,
            final TimeUnit unit,
            final String mountOptions) throws Exception{
        this(id, cdr, startTime, taskDir, dockerCmd, dockerImage, customParameters,
				timeOut, unit, mountOptions, false, null);
    }
    
    /**
     * For testing, lets one set alternate command line runner
     * @param clr alternate command line runner
     */
    protected void setAlternateCommandLineRunner(CommandLineRunner clr){
        _runner = clr;
    }
    
    /**
     * Writes contents {@link org.ndexbio.communitydetection.rest.model.CytoContainerRequest#getData()}
     * set via constructor 
     * to file which is assumed to be either a {@link com.fasterxml.jackson.databind.node.TextNode}
     * which is written as text or JSON which is mapped back via ObjectMapper
     * @return full path to input file as String
     * @throws CytoContainerException If there was an issue creating task directories
     * @throws IOException If there was IO error writing the data to a file
     */
    protected String writeInputFile() throws CytoContainerException, IOException {
        File workDir = new File(_workDir);
        
        if (workDir.isDirectory() == false){
            if (workDir.mkdirs() == false){
                throw new CytoContainerException("Unable to create directory: " + _workDir);
            }
        }
        File destFile = getInputFile();
        if (_cdr.getData()instanceof TextNode){
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(destFile))){
                bw.write(_cdr.getData().asText());
            }
        }
        else {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(destFile, _cdr.getData()); 
        }
        return destFile.getAbsolutePath();
    }
    
    /**
     * This method generates a {@link java.io.File} object pointing to standard 
     * out file generated by {@link #call()}
     * which may or may not exist yet.
     * @return standard out file
     */
    protected File getStandardOutFile(){
        return new File(_workDir + File.separator + STD_OUT_FILE);
    }
    
    /**
     * This method generates a {@link java.io.File} object pointing to 
     * standard error file generated by {@link #call()}
     * which may or may not exist yet.
     * @return standard error file
     */
    protected File getStandardErrorFile(){
        return new File(_workDir + File.separator + STD_ERR_FILE);
    }
    
    /**
     * This method generates a {@link java.io.File} object pointing to input 
     * file generated by {@link #writeInputFile() ()}
     * which may or may not exist yet.
     * @return input file
     */
    protected File getInputFile(){
        return new File(_workDir + File.separator + INPUT_FILE);
    }

    /**
     * This method generates a {@link java.io.File} object pointing to a 
     * file that contains the
     * actual command run by the command line program which is created by 
     * {@link #call()}
     * @return file containing command run
     */
    protected File getCommandRunFile(){
        return new File(_workDir + File.separator + CMD_RUN_FILE);
    }
    
    /**
     * Creates a {@link org.ndexbio.communitydetection.rest.model.CytoContainerResult}
     * object with id, status, and start time set.
     *
     * @return empty result
     */
    protected CytoContainerResult createCytoContainerResult(){
        CytoContainerResult cdr = new CytoContainerResult();
        cdr.setId(_id);
        cdr.setProgress(0);
        cdr.setStartTime(_startTime);
        cdr.setStatus(CytoContainerResult.PROCESSING_STATUS);
        return cdr;
    }
    
    /**
     * Reads contents of 'outFile' {@link java.io.File} setting those contents
     * via {@link org.ndexbio.communitydetection.rest.model.CytoContainerResult#setResult(com.fasterxml.jackson.databind.JsonNode)}
     * This implementation is a bit naive it will first try to use Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper#readTree(java.io.File)}
     * to parse the 'outFile' {@link java.io.File} and if that tosses an exception the code will
     * assume its raw text data and store the result as a {@link com.fasterxml.jackson.databind.node.TextNode}
     * @param cdr The object to update with results from 'outFile' {@link java.io.File}
     * @param outFile {@link java.io.File} to get data from
     * @throws Exception if there is an error
     */
    protected void updateCytoContainerResultWithFileContents(CytoContainerResult cdr, File outFile) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (outFile.isFile() == false){
            _logger.error(outFile.getAbsolutePath() + " does not exist or is not a file");
            return;
        }
		ObjectMapper mapper = new ObjectMapper();

		// if job succeeded and output is binary 
		// then dont load the contents of stdout.txt into
		// json, instead add the data type and size of result for now
		// at some point we might also want a URL so caller can just grab that
		// to get the result from the server
		
		if (_outputIsBinary && cdr.getStatus().equals(CytoContainerResult.COMPLETE_STATUS)){
			BinaryData bData = new BinaryData();
			bData.setContentType(_rawResultContentType);
			bData.setSizeInBytes(outFile.length());
			bData.setDownloadUrl("Filled in by REST endpoint");
			cdr.setResult(mapper.valueToTree(bData));
			return;
		}
		
        try {
            cdr.setResult(mapper.readTree(outFile));
        } catch(JsonParseException jpe){
            _logger.debug("Received a json parsing error going to try to store result as string: ", jpe);
            try (BufferedReader br = new BufferedReader(new FileReader(outFile))){
                String line = br.readLine();
                while(line != null){
                    sb.append(line).append("\n");
                    line = br.readLine();
                }
                cdr.setResult(new TextNode(sb.toString()));
            }
        }
    }
    
    /**
     * Updates the 'cdr' {@link org.ndexbio.communitydetection.rest.model.CytoContainerResult} status,
     * message, and result fields
     * 
     * @param exitValue  exit code of command line process
     * @param stdOutFile the {@link java.io.File} containing standard output from command line process
     * @param stdErrFile the {@link java.io.File} containing standard error from command line process
     * @param cdr {@link org.ndexbio.communitydetection.rest.model.CytoContainerResult} to update and persist to the filesystem
     * @throws Exception If there was a parsing/writing problem
     */
    protected void updateCytoContainerResult(int exitValue, File stdOutFile, File stdErrFile, CytoContainerResult cdr) throws Exception {
        
        File outFile = stdOutFile;
        if (exitValue != 0){
                cdr.setStatus(CytoContainerResult.FAILED_STATUS);
                if (exitValue == 500){
                    cdr.setMessage("Runtime limit exceeded");
                } else {
                    cdr.setMessage("Received non zero exit code: " +
                            Integer.toString(exitValue) + " when running algorithm for task: " + cdr.getId());
                }
                outFile = stdErrFile;
                _logger.error(cdr.getMessage());
        } else {
            
                cdr.setStatus(CytoContainerResult.COMPLETE_STATUS);
        }
        updateCytoContainerResultWithFileContents(cdr, outFile);
    }
    
    /**
     * Writes the command run by the commandline process in {@link #call() } to the 
     * filesystem storing contents at {@link #getCommandRunFile() }
     */
    protected void writeCommandRunToFile(){
        File outFile = getCommandRunFile();
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))){
            bw.write(_runner.getLastCommand());
        } catch(IOException io){
            _logger.error("Error writing command run to: " + outFile.getAbsolutePath(), io);
        }
    }
    
    /**
     * Runs the command line process set via the constructor storing output, error, and
     * command run by this process to the file system. 
     * @throws Exception if there was a problem with IO.
     * @return Result of running task
     */
    @Override
    public CytoContainerResult call() throws Exception {
        
        File workDir = new File(_workDir);
        
        String mapDir = _workDir + ":" + _workDir + _mountOptions;
        _runner.setWorkingDirectory(_workDir);
        
        File stdOutFile = getStandardOutFile();
        File stdErrFile = getStandardErrorFile();
        
        CytoContainerResult cdr = createCytoContainerResult();
        
        try {
            if (workDir.isDirectory() == false){
                throw new Exception(_workDir + " directory does not exist");
            }
            ArrayList<String> mCmd = new ArrayList<String>();
            mCmd.add(_dockerCmd);
            mCmd.add("run");
            mCmd.add("--rm");
            mCmd.add("-v");
            mCmd.add(mapDir);
            mCmd.add(_dockerImage);
            if (_customParameters != null){
                _logger.debug("Custom Parameters is not null adding to command line call");
                for (String key : _customParameters.keySet()){
                    mCmd.add(key);
                    
                    String val = _customParameters.get(key);
                    if (val != null && val.trim().isEmpty() == false){
                        mCmd.add(val);
                    }
                }
            } else {
                _logger.debug("Custom Parameters is null");
            }
            mCmd.add(_inputFilePath);
            int  exitValue = _runner.runCommandLineProcess(_timeOut, _timeUnit,
                    stdOutFile, stdErrFile, mCmd.toArray(new String[0]));
            writeCommandRunToFile();
            updateCytoContainerResult(exitValue, stdOutFile, stdErrFile, cdr);
            
        } catch(Exception ex){
            cdr.setStatus(CytoContainerResult.FAILED_STATUS);
            cdr.setMessage("Received error trying to run task: " + ex.getMessage());
            _logger.error("Received error trying to run algorithm for task in " + _workDir, ex);
        }
        cdr.setProgress(100);
        cdr.setWallTime(System.currentTimeMillis() - cdr.getStartTime());
        return cdr;          
    }
    
}
