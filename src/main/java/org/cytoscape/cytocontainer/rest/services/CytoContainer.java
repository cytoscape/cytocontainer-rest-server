package org.cytoscape.cytocontainer.rest.services; // Note your package will be {{ groupId }}.rest

import java.net.URI;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResult;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResultStatus;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequestId;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerBadRequestException;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.cytoscape.cytocontainer.rest.engine.CytoContainerEngine;
import org.cytoscape.cytocontainer.rest.model.Algorithm;
import org.cytoscape.cytocontainer.rest.model.Algorithms;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import java.util.List;

/**
 * CytoContainer service
 * @author churas
 */
@OpenAPIDefinition( info =
@Info(title = "Cytoscape Container REST service",
        version = "Unknown",
        description = "This service lets caller invoke various "
                + "algorithms which have been packaged into Docker images. To see what "
                + "algorithms are supported visit the 'algorithms' endpoint below.\n\n "
                + "<b>NOTE:</b> This service is experimental. The interface is subject to change.\n" +
                ""), servers = @Server(
        description = "default",
        url = "/cy/cytocontainer"
)

)

@Path("/")
public class CytoContainer {

    static Logger _logger = LoggerFactory.getLogger(CytoContainer.class);

    /**
     * Handles requests to run CytoContainer
     * @param query The task to run
     * @return {@link jakarta.ws.rs.core.Response}
     */
    @POST
    @Path(Configuration.V_ONE_PATH + "/{algorithm}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestJson(
            @PathParam("algorithm") final String algorithm,
            final CytoContainerRequest query) {

        try {
            CytoContainerEngine engine = Configuration.getInstance().getCytoContainerEngine();
            if (engine == null) throw new NullPointerException("CytoContainer Engine not loaded");

            String id = engine.request(algorithm, query);
            if (id == null) throw new CytoContainerException("No id returned from CytoContainer engine");

            CytoContainerRequestId t = new CytoContainerRequestId();
            t.setId(id);
            return Response.status(202)
                    .location(new URI(Configuration.getInstance().getHostURL() +
                            Configuration.V_ONE_PATH + "/" + id).normalize())
                    .entity(t).build();
        } catch (CytoContainerBadRequestException breq) {
            ErrorResponse er = breq.getErrorResponse();
            if (er == null) er = new ErrorResponse("Bad request received", breq);
            return Response.status(400).type(MediaType.APPLICATION_JSON).entity(er).build();
        } catch (Exception ex) {
            ErrorResponse er = new ErrorResponse("Error requesting CytoContainer", ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er).build();
        }
    }

    @POST
    @Path(Configuration.V_ONE_PATH + "/{algorithm}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Submits task using multipart/form-data",
            description = """
        Contains a 'metadata' JSON part and optional named file parts.
        Any parameters that take a file as input should have the value set
        to the name of the corresponding file part in the multipart body.
        """,
            responses = {
                    @ApiResponse(responseCode = "202", description = "Accepted",
                            headers = @Header(name = "Location", description = "Polling URL"),
                            content = @Content(schema = @Schema(implementation = CytoContainerRequestId.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Server Error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response requestMultipart(
            @PathParam("algorithm") final String algorithm,
            MultipartFormDataInput input) {

        try {
            Map<String, List<InputPart>> formParts = input.getFormDataMap();

            // 1. Extract and parse metadata
            List<InputPart> metadataParts = formParts.get("metadata");
            if (metadataParts == null || metadataParts.isEmpty()) {
                return Response.status(400)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(new ErrorResponse("Missing 'metadata' part")).build();
            }

            InputPart metadataPart = metadataParts.get(0);
            metadataPart.setMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
            InputStream metaStream = metadataPart.getBody(InputStream.class, null);
            String metadataJson = new String(metaStream.readAllBytes(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            CytoContainerRequest query = mapper.readValue(metadataJson, CytoContainerRequest.class);

            // 2. Collect all non-metadata parts as file streams keyed by part name.
            //    Per the spec, parameter values in metadata reference these part names.
            Map<String, InputStream> fileParts = new HashMap<>();
            for (Map.Entry<String, List<InputPart>> entry : formParts.entrySet()) {
                if ("metadata".equals(entry.getKey())) {
                    continue;
                }
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    InputPart filePart = entry.getValue().get(0);
                    filePart.setMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    fileParts.put(entry.getKey(), filePart.getBody(InputStream.class, null));
                }
            }

            // 3. Delegate to engine
            CytoContainerEngine engine = Configuration.getInstance().getCytoContainerEngine();
            if (engine == null) {
                throw new NullPointerException("CytoContainer Engine not loaded");
            }

            // TODO: pass fileParts to the engine — you'll need to extend
            //       engine.request() to accept the file map, e.g.:
            //       String id = engine.request(algorithm, query, fileParts);
            String id = engine.request(algorithm, query);
            if (id == null) {
                throw new CytoContainerException("No id returned from CytoContainer engine");
            }

            CytoContainerRequestId t = new CytoContainerRequestId();
            t.setId(id);
            return Response.status(202)
                    .location(new URI(Configuration.getInstance().getHostURL() +
                            Configuration.V_ONE_PATH + "/" + id).normalize())
                    .entity(t).build();

        } catch (CytoContainerBadRequestException breq) {
            ErrorResponse er = breq.getErrorResponse();
            if (er == null) er = new ErrorResponse("Bad request received", breq);
            return Response.status(400).type(MediaType.APPLICATION_JSON).entity(er).build();
        } catch (Exception e) {
            return Response.serverError()
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse("Error", e)).build();
        }
    }


    private String extractNameFromContentDisposition(String contentDisposition) {
        for (String param : contentDisposition.split(";")) {
            param = param.trim();
            if (param.startsWith("name=")) {
                return param.substring("name=".length()).replace("\"", "");
            }
        }
        return null;
    }

    @GET
    @Path(Configuration.V_ONE_PATH + "/{algorithm}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets result of task",
            description="NOTE: For incomplete/failed jobs only Status, message, progress, and walltime will\n" +
                    "be returned in JSON",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Success",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = CytoContainerResult.class))),
                    @ApiResponse(responseCode = "400",
                            description = "Task not found"),
                    @ApiResponse(responseCode = "500", description = "Server Error",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getResult(@PathParam("algorithm") final String algorithm, @PathParam("id") final String id) {

        try {
            CytoContainerEngine engine = Configuration.getInstance().getCytoContainerEngine();
            if (engine == null){
                throw new NullPointerException("CytoContainer Engine not loaded");
            }

            CytoContainerResult eqr = engine.getResult(algorithm, id);
            if (eqr == null){
                return Response.status(400).build();
            }

            return Response.ok().type(MediaType.APPLICATION_JSON).entity(eqr).build();
        }
        catch(CytoContainerBadRequestException cbre){
            ErrorResponse er = new ErrorResponse("Error getting results for id: " + id, cbre);
            return Response.status(400).type(MediaType.APPLICATION_JSON).entity(er).build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error getting results for id: " + id, ex);
            return Response.status(500).type(MediaType.APPLICATION_JSON).entity(er).build();
        }
    }

    @GET
    @Path(Configuration.V_ONE_PATH + "/{algorithm}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets meta data about this service/algorithm",
            description = "Provides detailed information about algorithm offered by this service",
            responses = {@ApiResponse(responseCode = "200",
                    description = "Success",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Algorithm.class))),
                    @ApiResponse(responseCode = "400",
                            description = "Task not found"),
                    @ApiResponse(responseCode = "500", description = "Server Error",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getMetaData(@PathParam("algorithm") final String algorithm){

        try {
            CytoContainerEngine engine = Configuration.getInstance().getCytoContainerEngine();
            if (engine == null){
                throw new NullPointerException("CytoContainer Engine not loaded");
            }

            Algorithm cda = engine.getMetaData(algorithm);
            if (cda == null){
                return Response.status(400).build();
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(cda).build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error trying to get metadata", ex);
            return Response.status(500).type(MediaType.APPLICATION_JSON).entity(er).build();
        }
    }

    @GET
    @Path(Configuration.V_ONE_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets all algorithms offered by this service",
            description = "Provides detailed information about ALL algorithms offered by this service",
            responses = {@ApiResponse(responseCode = "200",
                    description = "Success",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Algorithms.class))),
                    @ApiResponse(responseCode = "400",
                            description = "Task not found"),
                    @ApiResponse(responseCode = "500", description = "Server Error",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getMetaData(){

        try {
            CytoContainerEngine engine = Configuration.getInstance().getCytoContainerEngine();
            if (engine == null){
                throw new NullPointerException("CytoContainer Engine not loaded");
            }

            Algorithms algos = engine.getAllAlgorithms();
            if (algos == null){
                return Response.status(400).build();
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(algos).build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error trying to get all algorithms", ex);
            return Response.status(500).type(MediaType.APPLICATION_JSON).entity(er).build();
        }
    }

    @GET
    @Path(Configuration.V_ONE_PATH + "/{algorithm}/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets status of task",
            description="This lets caller get status without getting the full result back",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Success",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = CytoContainerResultStatus.class))),
                    @ApiResponse(responseCode = "400",
                            description = "Task not found"),
                    @ApiResponse(responseCode = "500", description = "Server Error",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getRequestStatus(@PathParam("algorithm") final String algorithm, @PathParam("id") final String id) {

        try {
            CytoContainerEngine engine = Configuration.getInstance().getCytoContainerEngine();
            if (engine == null){
                throw new NullPointerException("CytoContainer Engine not loaded");
            }
            CytoContainerResultStatus eqs = engine.getStatus(algorithm, id);
            if (eqs ==  null){
                return Response.status(400).build();
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(eqs).build();
        }
        catch(CytoContainerBadRequestException cbre){
            ErrorResponse er = new ErrorResponse("Error getting results for id: " + id, cbre);
            return Response.status(400).type(MediaType.APPLICATION_JSON).entity(er).build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error getting results for id: " + id, ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er).build();
        }
    }

    @DELETE
    @Path(Configuration.V_ONE_PATH + "/{algorithm}/{id}")
    @Operation(summary = "Deletes task associated with {id} passed in",
            description="",
            responses = {
                    @ApiResponse(responseCode = "204",
                            description = "Delete request successfully received"),
                    @ApiResponse(responseCode = "400",
                            description = "Not found"),
                    @ApiResponse(responseCode = "500", description = "Server Error",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response deleteRequest(@PathParam("algorithm") final String algorithm, @PathParam("id") final String id) {
        try {
            CytoContainerEngine engine = Configuration.getInstance().getCytoContainerEngine();
            if (engine == null){
                throw new NullPointerException("CytoContainer Engine not loaded");
            }
            engine.delete(algorithm, id);
            return Response.status(204).build();
        }
        catch(CytoContainerBadRequestException cbre){
            ErrorResponse er = new ErrorResponse("Error deleting: " + id, cbre);
            return Response.status(400).type(MediaType.APPLICATION_JSON).entity(er).build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error deleting: " + id, ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er).build();
        }
    }
}