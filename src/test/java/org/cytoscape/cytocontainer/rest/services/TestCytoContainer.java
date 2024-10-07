package org.cytoscape.cytocontainer.rest.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
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

/**
 *
 * @author churas
 */
public class TestCytoContainer {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();

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
	
    @Test
    public void testRequestCytoContainerWhereEngineNotLoaded() throws Exception {
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
            Configuration.getInstance().setCytoContainerEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
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
            assertEquals(500, response.getStatus());
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
            assertEquals(500, response.getStatus());
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
            assertEquals(410, response.getStatus());
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
            assertEquals(410, response.getStatus());
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
            assertEquals(200, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }
}
