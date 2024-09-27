package org.cytoscape.cytocontainer.rest.engine.util;

import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;

/**
 *
 * @author churas
 */
public interface CytoContainerRequestValidator {
    
    /**
     * Validates the request 
     * @param cda Algorithm to run
     * @param cdr The request to validate
     * @return null upon success otherwise {@link org.cytoscape.cytocontainer.rest.model.ErrorResponse} describing the error
     */
    public ErrorResponse validateRequest(CytoContainerAlgorithm cda, CytoContainerRequest cdr);
    
}
