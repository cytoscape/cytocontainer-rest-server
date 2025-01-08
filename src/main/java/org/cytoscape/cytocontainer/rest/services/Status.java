package org.cytoscape.cytocontainer.rest.services; 

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;
import org.cytoscape.cytocontainer.rest.model.ServerStatus;
import org.cytoscape.cytocontainer.rest.engine.CytoContainerEngine;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerNotFoundException;

/**
 * Returns status of Server
 * @author churas
 */
@Path("/")
public class Status {
    
    /**
     * REST endpoint for status 
     */
    public static final String STATUS_PATH = "/status";
    
    static Logger _logger = LoggerFactory.getLogger(Status.class);
    
    
    /**
     * Returns status of server 
     * @return {@link org.cytoscape.cytocontainer.rest.model.ServerStatus} as JSON
     */
    @GET 
    @Path(Configuration.V_ONE_PATH + "/{algorithm}" + STATUS_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets server status",
               description="Gets version, load, and diskusage of server",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Server Status",
                           content = @Content(mediaType = MediaType.APPLICATION_JSON,
                           schema = @Schema(implementation = ServerStatus.class))),
				   @ApiResponse(responseCode = "404", description = "Not found",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ErrorResponse.class))),
                   @ApiResponse(responseCode = "500", description = "Server Error",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response status(@PathParam("algorithm") final String algorithm) {
        ObjectMapper omappy = new ObjectMapper();

        try {
            CytoContainerEngine engine = Configuration.getInstance().getCytoContainerEngine();
            if (engine == null){
                throw new NullPointerException("CytoContainer Engine not loaded");
            }
            ServerStatus sObj = engine.getServerStatus(algorithm);
            if (sObj == null){
                throw new NullPointerException("No Server Status object returned");
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(sObj).build();
        } catch(CytoContainerNotFoundException notFoundEx){
			ErrorResponse er = notFoundEx.getErrorResponse();
            if (er == null){
                er = new ErrorResponse("Not Found", notFoundEx);
            }
            return Response.status(404).type(MediaType.APPLICATION_JSON).entity(er).build(); 
     	} catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error retreiving server status", ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
    }
	
	/**
     * Returns status of server 
     * @return {@link org.cytoscape.cytocontainer.rest.model.ServerStatus} as JSON
     */
    @GET // This annotation indicates GET request
    @Path(Configuration.V_ONE_PATH + STATUS_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets server status",
               description="Gets version, load, and diskusage of server",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Server Status",
                           content = @Content(mediaType = MediaType.APPLICATION_JSON,
                           schema = @Schema(implementation = ServerStatus.class))),
                   @ApiResponse(responseCode = "500", description = "Server Error",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response status() {
        return status("");
    }
}