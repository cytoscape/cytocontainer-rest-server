package org.cytoscape.cytocontainer.rest.services;


import jakarta.servlet.ServletException;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author churas
 */
public class OpenApiHttpServletDispatcher extends HttpServletDispatcher {
    
    static Logger _logger = LoggerFactory.getLogger(OpenApiHttpServletDispatcher.class.getSimpleName());
    
    public OpenApiHttpServletDispatcher() throws CytoContainerException{
        super();
        _logger.info("In constructor");
       

    }

    @Override
    public void init(jakarta.servlet.ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        _logger.info("Entering init()");
        _logger.info("Exiting init()");
    }
    
    @Override
    public void destroy() {
        super.destroy();
        _logger.info("In destroy()");
    }
}
