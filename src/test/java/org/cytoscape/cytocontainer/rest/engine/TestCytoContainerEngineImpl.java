package org.cytoscape.cytocontainer.rest.engine;


import com.fasterxml.jackson.databind.node.TextNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import org.easymock.Capture;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.cytoscape.cytocontainer.rest.engine.util.DockerCytoContainerRunner;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithms;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResult;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResultStatus;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;
import org.cytoscape.cytocontainer.rest.model.ServerStatus;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerBadRequestException;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.cytoscape.cytocontainer.rest.services.Configuration;
import org.cytoscape.cytocontainer.rest.engine.util.CytoContainerRequestValidator;


/**
 *
 * @author churas
 */
public class TestCytoContainerEngineImpl {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    
    public TestCytoContainerEngineImpl() {
    }
   
    @Test
    public void testthreadSleep() throws Exception {
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", null, null);
        engine.updateThreadSleepTime(1);
        engine.threadSleep();
    }
    
    @Test
    public void testRunWithShutDownTrue(){
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", null, null);
        engine.shutdown();
        engine.run();
    }
    
    @Test
    public void testLogServerStatus(){
        // try with null
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", null, null);
        engine.logServerStatus(null);
        
        // try with empty ServerStatus
        ServerStatus ss = new ServerStatus();
        engine.logServerStatus(ss);
        
        // try with string fields set
        ss.setVersion("version");
        ss.setStatus("status");
        engine.logServerStatus(ss);
        
    }
    
    @Test
    public void testgetCytoContainerResultFilePath(){
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", null, null);
        String res = engine.getCytoContainerResultFilePath("12345");
        assertEquals("task/12345/" + CytoContainerEngineImpl.CDRESULT_JSON_FILE, res);
    }
    
    @Test
    public void testsaveCytoContainerResultToFilesystem() throws IOException {
        try {
            
            File tempDir = _folder.newFolder();
            CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
        
            //try with null 1st
            engine.saveCytoContainerResultToFilesystem(null);
            
            CytoContainerResult cdr = new CytoContainerResult();
            cdr.setId("1");
            cdr.setResult(TextNode.valueOf("hi"));
            cdr.setMessage("message");
            
            //try where dest directory does not exist
            engine.saveCytoContainerResultToFilesystem(cdr);
            
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + cdr.getId());
            assertTrue(taskDir.mkdirs());

            engine.saveCytoContainerResultToFilesystem(cdr);
            
            CytoContainerResult cRes = engine.getCytoContainerResultFromDbOrFilesystem(cdr.getId());
            assertEquals("message", cRes.getMessage());
            
        }
        finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testlogResult() throws IOException {
      
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", null, null);
        
        //try passing null
        engine.logResult(null);
        
        //try passing empty
        engine.logResult(new CytoContainerResult());
        
        //try passing fully set
        CytoContainerResult cdr = new CytoContainerResult();
        cdr.setId("1");
        cdr.setMessage("message");
        cdr.setProgress(50);
        cdr.setResult(TextNode.valueOf("hi"));
        cdr.setStartTime(1);
        cdr.setStatus("status");
        cdr.setWallTime(2);
        engine.logResult(cdr);
    }
    
    @Test
    public void testgetCytoContainerResultFromDbOrFilesystem() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            
            //try with invalid id
            assertNull(engine.getCytoContainerResultFromDbOrFilesystem("1"));
            
            //try with invalid data
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "1");
            assertTrue(taskDir.mkdirs());
            
            FileWriter fw = new FileWriter(engine.getCytoContainerResultFilePath("1"));
            fw.write("xxx");
            fw.flush();
            fw.close();
            assertNull(engine.getCytoContainerResultFromDbOrFilesystem("1"));
            
            File resFile = new File(engine.getCytoContainerResultFilePath("1"));
            assertTrue(resFile.delete());
            CytoContainerResult cdr = new CytoContainerResult();
            cdr.setId("1");
            cdr.setMessage("message");
            engine.saveCytoContainerResultToFilesystem(cdr);
            CytoContainerResult cRes = engine.getCytoContainerResultFromDbOrFilesystem("1");
            assertEquals("message", cRes.getMessage());
            
            
        } finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testgetResult() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            
            //try with null
            try {
                engine.getResult(null, null);
                fail("Expected CytoContainerException");
            } catch(CytoContainerException cde){
                assertEquals("Id is null", cde.getMessage());
            }
            
            //try with invalid id
            try {
                engine.getResult("algo","1");
                fail("Expected CytoContainerException");
            } catch(CytoContainerException cde){
                assertEquals("No task with id of 1 found", cde.getMessage());
            }
            
            //try with invalid data
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "1");
            assertTrue(taskDir.mkdirs());
            
            
            File resFile = new File(engine.getCytoContainerResultFilePath("1"));
           
            CytoContainerResult cdr = new CytoContainerResult();
            cdr.setId("1");
            cdr.setMessage("message");
            engine.saveCytoContainerResultToFilesystem(cdr);
            try {
                CytoContainerResult res = engine.getResult("algo","1");
                assertEquals("message", res.getMessage());
            } catch(CytoContainerException cde){
                fail("Unexpected CytoContainerException " + cde.getMessage());
            }
        } finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testgetStatus() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            
            //try with null
            try {
                engine.getStatus("algo",null);
                fail("Expected CytoContainerException");
            } catch(CytoContainerException cde){
                assertEquals("Id is null", cde.getMessage());
            }
            
            //try with invalid id
            try {
                engine.getStatus("algo","1");
                fail("Expected CytoContainerException");
            } catch(CytoContainerException cde){
                assertEquals("No task with id of 1 found", cde.getMessage());
            }
            
            //try with invalid data
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "1");
            assertTrue(taskDir.mkdirs());
            
            
            File resFile = new File(engine.getCytoContainerResultFilePath("1"));
           
            CytoContainerResult cdr = new CytoContainerResult();
            cdr.setId("1");
            cdr.setMessage("message");
            engine.saveCytoContainerResultToFilesystem(cdr);
            try {
                CytoContainerResultStatus res = engine.getStatus("algo","1");
                assertEquals("message", res.getMessage());
            } catch(CytoContainerException cde){
                fail("Unexpected CytoContainerException " + cde.getMessage());
            }
        } finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testRequestWhereRequestIsNull(){
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", null, null);
        try {
            engine.request("algo",null);
            fail("Expected CytoContainerBadRequestException");
        } catch(CytoContainerBadRequestException cdbe){
            assertEquals("Request is null", cdbe.getMessage());
        } catch(CytoContainerException cde){
            fail("Unexpected exception: " + cde.getMessage());
        }
    }
    
    @Test
    public void testRequestNoAlgorithm(){
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", null, null);
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            
            engine.request(null,cdr);
            fail("Expected CytoContainerBadRequestException");
        } catch(CytoContainerBadRequestException cdbe){
            assertEquals("No algorithm specified", cdbe.getMessage());
        } catch(CytoContainerException cde){
            fail("Unexpected exception: " + cde.getMessage());
        }
    }
    
    @Test
    public void testRequestNoAlgorithmsSetInConstructor(){
        
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", null, null);
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setAlgorithm("foo");
            engine.request("foo",cdr);
            fail("Expected CytoContainerException");
        } catch(CytoContainerBadRequestException cdbe){
            fail("Unexpected exception: " + cdbe.getMessage());
            
        } catch(CytoContainerException cde){
            assertEquals("No algorithms are available to run in service", cde.getMessage());
        }
    }
    
    @Test
    public void testRequestNoAlgorithmsMatch(){
        CytoContainerAlgorithms algos = new CytoContainerAlgorithms();
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("blah");
        LinkedHashMap<String, CytoContainerAlgorithm> aMap = new LinkedHashMap<>();
        aMap.put(cda.getName(), cda);
        algos.setAlgorithms(aMap);
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task",
                "docker", algos, null);
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setAlgorithm("foo");
            engine.request("algo",cdr);
            fail("Expected CytoContainerBadRequestException");
        } catch(CytoContainerBadRequestException cdbe){
            assertEquals("foo is not a valid algorithm", cdbe.getMessage());
        } catch(CytoContainerException cde){
            fail("Unexpected exception: " + cde.getMessage());
            
        }
    }
    
    @Test
    public void testRequestValidationFails(){
        CytoContainerAlgorithms algos = new CytoContainerAlgorithms();
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("foo");
        LinkedHashMap<String, CytoContainerAlgorithm> aMap = new LinkedHashMap<>();
        aMap.put(cda.getName(), cda);
        algos.setAlgorithms(aMap);
        CytoContainerRequestValidator mockValidator = mock(CytoContainerRequestValidator.class);
        CytoContainerRequest cdr = new CytoContainerRequest();
        cdr.setAlgorithm("foo");
        ErrorResponse er = new ErrorResponse();
        er.setMessage("problem");
        expect(mockValidator.validateRequest(cda, cdr)).andReturn(er);
        replay(mockValidator);
        CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null, "task", 
                "docker", algos, mockValidator);
        try {
            engine.request("algo",cdr);
            fail("Expected CytoContainerBadRequestException");
        } catch(CytoContainerBadRequestException cdbe){
            assertEquals("Validation failed", cdbe.getMessage());
            assertEquals("problem", er.getMessage());
        } catch(CytoContainerException cde){
            fail("Unexpected exception: " + cde.getMessage());
        }
        verify(mockValidator);
    }
    
    @Test
    public void testRequestTaskCreationFails() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.MOUNT_OPTIONS + " = :ro,z\n");
            fw.write(Configuration.ALGORITHM_TIMEOUT + " = 10\n");
            
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            CytoContainerAlgorithms algos = new CytoContainerAlgorithms();
            CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
            cda.setName("foo");
            LinkedHashMap<String, CytoContainerAlgorithm> aMap = new LinkedHashMap<>();
            aMap.put(cda.getName(), cda);
            algos.setAlgorithms(aMap);
            CytoContainerRequestValidator mockValidator = mock(CytoContainerRequestValidator.class);
            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setAlgorithm("foo");

            expect(mockValidator.validateRequest(cda, cdr)).andReturn(null);

            ExecutorService mockES = mock(ExecutorService.class);
            Capture<DockerCytoContainerRunner> cappy = Capture.newInstance();
            expect(mockES.submit(capture(cappy))).andThrow(new RejectedExecutionException("failed"));
            replay(mockES);
            replay(mockValidator);
            CytoContainerEngineImpl engine = new CytoContainerEngineImpl(mockES,
                    tempDir.getAbsolutePath(), "docker", algos, mockValidator);
            try {
                engine.request("algo",cdr);
                fail("Expected CytoContainerException");
            } catch(CytoContainerBadRequestException cdbe){
                fail("Unexpected exception: " + cdbe.getMessage());
            } catch(CytoContainerException cde){
                assertEquals("failed", cde.getMessage());
            }
            
            assertNotNull(cappy.getValue());
            verify(mockValidator);
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRequestSuccess() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.MOUNT_OPTIONS + " = :ro,z\n");
            fw.write(Configuration.ALGORITHM_TIMEOUT + " = 10\n");
            
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            CytoContainerAlgorithms algos = new CytoContainerAlgorithms();
            CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
            cda.setName("foo");
            LinkedHashMap<String, CytoContainerAlgorithm> aMap = new LinkedHashMap<>();
            aMap.put(cda.getName(), cda);
            algos.setAlgorithms(aMap);
            CytoContainerRequestValidator mockValidator = mock(CytoContainerRequestValidator.class);
            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setAlgorithm("foo");
            cdr.setData(TextNode.valueOf("hi"));
            Map<String, String> cParams = new HashMap<>();
            cParams.put("key1", null);
            cParams.put("key2", "val2");
            cdr.setParameters(cParams);

            expect(mockValidator.validateRequest(cda, cdr)).andReturn(null);

            ExecutorService mockES = mock(ExecutorService.class);
            Capture<DockerCytoContainerRunner> cappy = Capture.newInstance();
            FutureTask<CytoContainerResult> mockFT = mock(FutureTask.class);
            expect(mockES.submit(capture(cappy))).andReturn(mockFT);
            replay(mockFT);
            replay(mockES);
            replay(mockValidator);
            CytoContainerEngineImpl engine = new CytoContainerEngineImpl(mockES,
                    tempDir.getAbsolutePath(), "docker", algos, mockValidator);
            try {
                assertNotNull(engine.request("algo",cdr));
            } catch(CytoContainerBadRequestException cdbe){
                fail("Unexpected exception: " + cdbe.getMessage());
            } catch(CytoContainerException cde){
                fail("Unexpected exception: " + cde.getMessage());
            }
            
            assertNotNull(cappy.getValue());
            verify(mockValidator);
            verify(mockES);
            verify(mockFT);
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testDeleteNullId() throws IOException {
        try {
            File tempDir = _folder.newFolder();
             CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            try {
                engine.delete("algo",null);
                fail("Expected CytoContainerException ");
            } catch(CytoContainerException cde){
                assertEquals("id is null", cde.getMessage());
            }
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testDeleteWhereIdNotInResultsOrFilesystem() throws IOException {
        try {
            File tempDir = _folder.newFolder();
             CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            try {
                engine.delete("algo","1");
                
            } catch(CytoContainerException cde){
                fail("unexpected CytoContainerException: " + cde.getMessage());
            }
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testDeleteWhereIdIsInFilesystem() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "1");
            assertTrue(taskDir.mkdirs());
            
            FileWriter fw = new FileWriter(taskDir.getAbsolutePath()
                    + File.separator + CytoContainerEngineImpl.CDRESULT_JSON_FILE);
            fw.write("haha");
            fw.flush();
            fw.close();
            
             CytoContainerEngineImpl engine = new CytoContainerEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            try {
                engine.delete("algo","1");
                assertFalse(taskDir.exists());
            } catch(CytoContainerException cde){
                fail("unexpected CytoContainerException: " + cde.getMessage());
            }
            
        } finally {
            _folder.delete();
        }
    }
}