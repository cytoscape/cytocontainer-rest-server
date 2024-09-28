package org.cytoscape.cytocontainer.rest.engine;

import java.io.InputStream;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResultStatus;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResult;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.cytoscape.cytocontainer.rest.model.ServerStatus;
import org.cytoscape.cytocontainer.rest.model.Algorithm;

/**
 *
 * @author churas
 */
public interface CytoContainerEngine extends Runnable {
    
    /**
     * Submits request for processing
     * @param request to process
     * @throws CytoContainerException if there is an error
     * @return UUID as a string that is an identifier for query
     */
    public String request(final String algorithm, CytoContainerRequest request) throws CytoContainerException;
     
    /**
     * Gets query results
     * @param id id of task

     * @return result of task
     * @throws CytoContainerException  if there is an error
     */
    public CytoContainerResult getResult(final String algorithm, final String id) throws CytoContainerException;
	
	/**
     * Gets query result data only as {@code InputStream}
     * @param id id of task
     * @return data in result of task as {@code InputStream}
     * @throws CytoContainerException  if there is an error
     */
    public InputStream getResultData(final String algorithm, final String id) throws CytoContainerException;
	
    /**
     * Gets query status
     * @param id id of task
     * @return status of task
     * @throws CytoContainerException if there is an error
     */
    public CytoContainerResultStatus getStatus(final String algorithm, final String id) throws CytoContainerException;
    
    /**
     * Deletes query
     * @param id id of task
     * @throws CytoContainerException if there is an error
     */
    public void delete(final String algorithm, final String id) throws CytoContainerException;
    
	/**
	 * Gets community detection metadata for this algorithm
	 * 
	 * @throws CytoContainerException if there is an a error
	 * @return metadata
	 */
	public Algorithm getMetaData(final String algorithm) throws CytoContainerException;
	
    /**
     * Gets status of server
     * @return status of server
     * @throws CytoContainerException if there is an error
     */
    public ServerStatus getServerStatus(final String algorithm) throws CytoContainerException;
    
    /**
     * Tells implementing objects to shutdown
     */
    public void shutdown();
    
}
