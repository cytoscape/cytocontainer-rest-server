package org.cytoscape.cytocontainer.rest.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResultStatus;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResult;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequestId;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerBadRequestException;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.cytoscape.cytocontainer.rest.engine.CytoContainerEngine;
import org.easymock.EasyMock;
import org.jboss.resteasy.core.ResteasyContext;
import java.io.InputStream;
import java.util.Map;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataReader;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataWriter;
import org.jboss.resteasy.plugins.providers.InputStreamProvider;
import org.jboss.resteasy.plugins.providers.ByteArrayProvider;
import org.jboss.resteasy.plugins.providers.StringTextStar;
/**
 *
 * @author churas
 */
public class TestCytoContainer {

    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    private static final String BOUNDARY = "----TestBoundary12345";
	@After
	public void tearDown(){
		try {
			Configuration.getInstance().setCytoContainerEngine(null);
		} catch(Exception ex){
			ex.printStackTrace();
		}
	}

	/**
	 * Creates Dispatcher used to invoke mock request
	 * @return Dispatcher loaded with CytoContainer clas
	 */
    public Dispatcher getDispatcher(){
        Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getProviderFactory().registerProvider(MultipartFormDataReader.class);
        dispatcher.getProviderFactory().registerProvider(MultipartFormDataWriter.class);
        dispatcher.getProviderFactory().registerProvider(InputStreamProvider.class);
        dispatcher.getProviderFactory().registerProvider(ByteArrayProvider.class);
        dispatcher.getProviderFactory().registerProvider(StringTextStar.class);
        dispatcher.getRegistry().addSingletonResource(new CytoContainer());
        return dispatcher;
    }

	/**
	 * Creates basic configuration with task directory set to full path
	 * of tempDir passed in
	 * @param tempDir
	 * @return
	 * @throws IOException
	 */
	public File createBasicConfigurationFile(File tempDir) throws IOException {
		File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");

		FileWriter fw = new FileWriter(confFile);

		fw.write(Configuration.TASK_DIR + " = "
				+ tempDir.getAbsolutePath() + "\n");
		fw.flush();
		fw.close();
		return confFile;
	}

    /**
     * Builds a raw multipart/form-data byte array with a metadata JSON part
     * and optional file parts.
     * @param metadataJson the JSON string for the metadata part, or null to omit
     * @param files map of partName -> {filename, content} pairs
     */
    private byte[] buildMultipartBody(String metadataJson, Map<String, String[]> files) {
        StringBuilder sb = new StringBuilder();
        if (metadataJson != null) {
            sb.append("--").append(BOUNDARY).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"metadata\"; filename=\"metadata.json\"\r\n");
            sb.append("Content-Type: application/json\r\n");
            sb.append("\r\n");
            sb.append(metadataJson).append("\r\n");
        }
        if (files != null) {
            for (Map.Entry<String, String[]> entry : files.entrySet()) {
                String partName = entry.getKey();
                String fileName = entry.getValue()[0];
                String content = entry.getValue()[1];
                sb.append("--").append(BOUNDARY).append("\r\n");
                sb.append("Content-Disposition: form-data; name=\"").append(partName)
                        .append("\"; filename=\"").append(fileName).append("\"\r\n");
                sb.append("Content-Type: application/octet-stream\r\n");
                sb.append("\r\n");
                sb.append(content).append("\r\n");
            }
        }
        sb.append("--").append(BOUNDARY).append("--\r\n");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String getMultipartContentType() {
        return "multipart/form-data; boundary=" + BOUNDARY;
    }

    @Test
    public void testRequestCytoContainerWhereEngineNotLoaded() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            CytoContainerRequest query = new CytoContainerRequest();
            ObjectMapper omappy = new ObjectMapper();

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(omappy.writeValueAsBytes(query));

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setCytoContainerEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(), ErrorResponse.class);
            assertEquals("Error requesting CytoContainer", er.getMessage());
            assertEquals("CytoContainer Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testRequestWhereQueryRaisesError() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);

            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo");
            CytoContainerRequest query = new CytoContainerRequest();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);

            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(notNull(), notNull())).andThrow(new CytoContainerException("some error"));
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error requesting CytoContainer", er.getMessage());
            assertEquals("some error", er.getDescription());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testRequestWhereQueryRaisesBadRequestError() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo");
            CytoContainerRequest query = new CytoContainerRequest();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);

            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(notNull(), notNull())).andThrow(new CytoContainerBadRequestException("some error"));
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(400, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Bad request received", er.getMessage());
            assertEquals("some error", er.getDescription());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testRequestWhereQueryRaisesBadRequestErrorWithErrorResponse() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo");
            CytoContainerRequest query = new CytoContainerRequest();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);

            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            ErrorResponse xer = new ErrorResponse();
            xer.setMessage("hello");
            expect(mockEngine.request(notNull(), notNull())).andThrow(new CytoContainerBadRequestException("some error", xer));
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(400, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("hello", er.getMessage());
            assertEquals(null, er.getDescription());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testRequestWhereQueryReturnsNull() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo");
            CytoContainerRequest query = new CytoContainerRequest();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);

            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(notNull(), notNull())).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error requesting CytoContainer", er.getMessage());
            assertEquals("No id returned from CytoContainer engine", er.getDescription());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testRequestWhereQuerySuccess() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo");
            CytoContainerRequest query = new CytoContainerRequest();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);

            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(notNull(), notNull())).andReturn("12345");
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(202, response.getStatus());

            MultivaluedMap<String, Object> resmap = response.getOutputHeaders();
            assertEquals(new URI(Configuration.V_ONE_PATH + "/12345"), resmap.getFirst("Location"));
            ObjectMapper mapper = new ObjectMapper();
            CytoContainerRequestId t = mapper.readValue(response.getOutput(),
                    CytoContainerRequestId.class);
            assertEquals("12345", t.getId());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }

        @Test
    public void testRequestWhereQuerySuccessAndHostURLSet() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            FileWriter fw = new FileWriter(confFile);
            fw.write(Configuration.HOST_URL + " = http://foo.com\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo");
            CytoContainerRequest query = new CytoContainerRequest();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);

            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(notNull(), notNull())).andReturn("12345");
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(202, response.getStatus());

            MultivaluedMap<String, Object> resmap = response.getOutputHeaders();
            assertEquals(new URI("http://foo.com" + Configuration.V_ONE_PATH + "/12345"), resmap.getFirst("Location"));
            ObjectMapper mapper = new ObjectMapper();
            CytoContainerRequestId t = mapper.readValue(response.getOutput(),
                    CytoContainerRequestId.class);
            assertEquals("12345", t.getId());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testGetWhereCytoContainerEngineNotLoaded() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/algo/12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setCytoContainerEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error getting results for id: 12345", er.getMessage());
            assertEquals("CytoContainer Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testGetWhereIdDoesNotExist() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/algo/12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.getResult("algo","12345")).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(400, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testGetWhereIdExists() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH +
                                                          "/algo/12345?start=1&size=2");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            CytoContainerResult eqr = new CytoContainerResult();
            eqr.setMessage("hi");
            expect(mockEngine.getResult("algo","12345")).andReturn(eqr);
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CytoContainerResult res = mapper.readValue(response.getOutput(),
                    CytoContainerResult.class);
            assertEquals("hi", res.getMessage());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testGetAlgorithmsWhereCytoContainerEngineNotLoaded() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/algorithms");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setCytoContainerEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error trying to get metadata", er.getMessage());
            assertEquals("CytoContainer Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
        }
    }



    @Test
    public void testGetStatusWhereEnrichmentEngineNotLoaded() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/algo/12345/status");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setCytoContainerEngine(null);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error getting results for id: 12345", er.getMessage());
            assertEquals("CytoContainer Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testGetStatusWhereIdDoesNotExist() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/algo/12345/status");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.getStatus("algo","12345")).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(400, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testGetStatusWhereIdExists() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH +
                                                          "/algo/12345/status");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            CytoContainerResultStatus eqs = new CytoContainerResultStatus();
            eqs.setProgress(55);
            expect(mockEngine.getStatus("algo","12345")).andReturn(eqs);
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CytoContainerResultStatus res = mapper.readValue(response.getOutput(),
                    CytoContainerResultStatus.class);
            assertEquals(55, res.getProgress());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testDeleteWhereEnrichmentEngineNotLoaded() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.delete(Configuration.V_ONE_PATH + "/algo/12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setCytoContainerEngine(null);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error deleting: 12345", er.getMessage());
            assertEquals("CytoContainer Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testDeleteSuccessful() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            MockHttpRequest request = MockHttpRequest.delete(Configuration.V_ONE_PATH +
                                                          "/algo/12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            // create mock enrichment engine that returns null
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            mockEngine.delete("algo","12345");
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(204, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testMultipartRequestWhereEngineNotLoaded() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            byte[] body = buildMultipartBody("{\"data\":{}}", null);
            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(getMultipartContentType())
                    .content(body);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setCytoContainerEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(), ErrorResponse.class);
            assertEquals("CytoContainer Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testMultipartRequestMissingMetadataPart() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            // Build a body with only a file part, no metadata
            StringBuilder sb = new StringBuilder();
            sb.append("--").append(BOUNDARY).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"network\"; filename=\"foo.cx2\"\r\n");
            sb.append("Content-Type: application/octet-stream\r\n");
            sb.append("\r\n");
            sb.append("{\"nodes\":[]}").append("\r\n");
            sb.append("--").append(BOUNDARY).append("--\r\n");

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(getMultipartContentType())
                    .content(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(400, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(), ErrorResponse.class);
            assertEquals("Missing 'metadata' part", er.getMessage());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testMultipartRequestSuccessWithMetadataOnly() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            byte[] body = buildMultipartBody("{\"data\":{}}", null);
            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(getMultipartContentType())
                    .content(body);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(EasyMock.eq("algo"), notNull(),
                    EasyMock.isNull(), EasyMock.anyObject(Map.class))).andReturn("99999");
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(202, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CytoContainerRequestId rid = mapper.readValue(response.getOutput(),
                    CytoContainerRequestId.class);
            assertEquals("99999", rid.getId());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testMultipartRequestSuccessWithFile() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            Map<String, String[]> files = new java.util.LinkedHashMap<>();
            files.put("network", new String[]{"foo.cx2", "{\"nodes\":[]}"});

            byte[] body = buildMultipartBody("{\"data\":{}}", files);
            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(getMultipartContentType())
                    .content(body);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(EasyMock.eq("algo"), notNull(),
                    notNull(), notNull())).andReturn("88888");
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(202, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CytoContainerRequestId rid = mapper.readValue(response.getOutput(),
                    CytoContainerRequestId.class);
            assertEquals("88888", rid.getId());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testMultipartRequestSuccessWithMultipleFiles() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            Map<String, String[]> files = new java.util.LinkedHashMap<>();
            files.put("network", new String[]{"foo.cx2", "{\"nodes\":[]}"});
            files.put("features", new String[]{"data.tsv", "col1\tcol2\n"});

            byte[] body = buildMultipartBody("{\"data\":{}}", files);
            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(getMultipartContentType())
                    .content(body);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(EasyMock.eq("algo"), notNull(),
                    notNull(), notNull())).andReturn("77777");
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(202, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CytoContainerRequestId rid = mapper.readValue(response.getOutput(),
                    CytoContainerRequestId.class);
            assertEquals("77777", rid.getId());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testMultipartRequestEngineThrowsException() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            Map<String, String[]> files = new java.util.LinkedHashMap<>();
            files.put("network", new String[]{"foo.cx2", "{\"nodes\":[]}"});

            byte[] body = buildMultipartBody("{\"data\":{}}", files);
            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(getMultipartContentType())
                    .content(body);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(EasyMock.eq("algo"), notNull(),
                    notNull(), notNull())).andThrow(new CytoContainerException("engine broke"));
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(), ErrorResponse.class);
            assertEquals("engine broke", er.getDescription());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testMultipartRequestEngineBadRequestException() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            Map<String, String[]> files = new java.util.LinkedHashMap<>();
            files.put("network", new String[]{"foo.cx2", "{\"nodes\":[]}"});

            byte[] body = buildMultipartBody("{\"data\":{}}", files);
            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(getMultipartContentType())
                    .content(body);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(EasyMock.eq("algo"), notNull(),
                    notNull(), notNull())).andThrow(new CytoContainerBadRequestException("bad input"));
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(400, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(), ErrorResponse.class);
            assertEquals("Bad request received", er.getMessage());
            assertEquals("bad input", er.getDescription());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testMultipartRequestInvalidMetadataJson() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            // Send malformed JSON as metadata
            byte[] body = buildMultipartBody("not valid json{{{", null);
            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(getMultipartContentType())
                    .content(body);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testJsonEndpointStillWorksAfterMultipartChanges() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = createBasicConfigurationFile(tempDir);
            Dispatcher dispatcher = getDispatcher();

            CytoContainerRequest query = new CytoContainerRequest();
            ObjectMapper omappy = new ObjectMapper();

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH + "/algo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(omappy.writeValueAsBytes(query));

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());

            CytoContainerEngine mockEngine = createMock(CytoContainerEngine.class);
            expect(mockEngine.request(notNull(), notNull())).andReturn("55555");
            replay(mockEngine);
            Configuration.getInstance().setCytoContainerEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(202, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CytoContainerRequestId rid = mapper.readValue(response.getOutput(),
                    CytoContainerRequestId.class);
            assertEquals("55555", rid.getId());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }
    }

