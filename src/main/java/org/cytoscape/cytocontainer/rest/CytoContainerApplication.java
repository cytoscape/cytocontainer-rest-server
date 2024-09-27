package org.cytoscape.cytocontainer.rest; 


import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cytoscape.cytocontainer.rest.services.CustomOpenApiResource;
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
		
		OpenAPI oas = new OpenAPI();
        Info info = new Info()
                .title("Swagger Sample App bootstrap code")
                .description("This is a sample server Petstore server.  You can find out more about Swagger " +
                        "at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, " +
                        "you can use the api key `special-key` to test the authorization filters.")
                .termsOfService("http://swagger.io/terms/")
                .contact(new Contact()
                        .email("apiteam@swagger.io"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));

        oas.info(info);
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of("io.swagger.sample.resource").collect(Collectors.toSet()));

        try {
            new JaxrsOpenApiContextBuilder()
                    .servletConfig(servletConfig)
                    .application(this)
                    .openApiConfiguration(oasConfig)
                    .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
		_logger.info("Did something here, not sure what");
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
				         CustomOpenApiResource.class,
                         Status.class).collect(Collectors.toSet());
    }
}