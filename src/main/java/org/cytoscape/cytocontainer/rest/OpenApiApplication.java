package org.cytoscape.cytocontainer.rest; 

import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;


/**
 * REST application to generate OpenAPI documentation and to 
 * server openapi.json file
 * 
 * @author churas
 */
public class OpenApiApplication extends Application {

    private final Set<Object> _singletons = new HashSet<Object>();
    public OpenApiApplication() {        
        // Register our hello service
        CorsFilter corsFilter = new CorsFilter();
        corsFilter.getAllowedOrigins().add("*");
        corsFilter.setAllowCredentials(true);
        _singletons.add(corsFilter);
    }
    @Override
    public Set<Object> getSingletons() {
        return _singletons;
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        return Stream.of(
                OpenApiResource.class,
                AcceptHeaderOpenApiResource.class).collect(Collectors.toSet());
    }
}