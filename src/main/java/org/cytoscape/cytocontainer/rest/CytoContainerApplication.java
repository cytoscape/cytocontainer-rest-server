package org.cytoscape.cytocontainer.rest; 


import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cytoscape.cytocontainer.rest.engine.util.StringMessageBodyWriter;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.cytoscape.cytocontainer.rest.services.CytoContainer;
import org.cytoscape.cytocontainer.rest.services.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CytoContainerApplication extends Application {

	static Logger _logger = LoggerFactory.getLogger(CytoContainerApplication.class.getSimpleName());
    private final Set<Object> _singletons = new HashSet<>();
	
    public CytoContainerApplication(@Context ServletConfig servletConfig) {        
        // Register our hello service
        CorsFilter corsFilter = new CorsFilter();
        corsFilter.getAllowedOrigins().add("*");
        corsFilter.setAllowCredentials(true);
        _singletons.add(corsFilter);
		
		
		_logger.info("Initializing Application");
    }
    @Override
    public Set<Object> getSingletons() {
        return _singletons;
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        // @TODO add logic to ony add Diffusion.class if there is a diffusion
        //       algorithm present
        return Stream.of(CytoContainer.class,
				         OpenApiResource.class, 
						 AcceptHeaderOpenApiResource.class,
						 StringMessageBodyWriter.class,
                         Status.class).collect(Collectors.toSet());
    }
}