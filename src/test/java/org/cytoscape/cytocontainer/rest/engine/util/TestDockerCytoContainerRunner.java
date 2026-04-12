package org.cytoscape.cytocontainer.rest.engine.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResult;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
/**
 *
 * @author churas
 */
public class TestDockerCytoContainerRunner {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    @Test
    public void testConstructorWriteInputThrowsException() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "someid");
            assertTrue(taskDir.createNewFile());
            
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", new CytoContainerRequest(),
                    0, taskDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            fail("Expected Exception");
            
        } catch(CytoContainerException cde){
            assertTrue(cde.getMessage().contains("Unable to create directory:"));
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testConstructorSuccessfulTextDataAndTestAFewOtherThings() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            
            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setData(new TextNode("blah"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    3, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            
            try (BufferedReader br = new BufferedReader(new FileReader(runner.getInputFile()))){
                assertEquals("blah", br.readLine());
            }
            
            // check some of the basic get methods do the right thing
            assertEquals(tempDir.getAbsolutePath() + File.separator + "someid" + 
                    File.separator + DockerCytoContainerRunner.CMD_RUN_FILE, 
                    runner.getCommandRunFile().getAbsolutePath());
            assertEquals(tempDir.getAbsolutePath() + File.separator + "someid" + 
                    File.separator + DockerCytoContainerRunner.INPUT_FILE, 
                    runner.getInputFile().getAbsolutePath());
            assertEquals(tempDir.getAbsolutePath() + File.separator + "someid" + 
                    File.separator + DockerCytoContainerRunner.STD_OUT_FILE, 
                    runner.getStandardOutFile().getAbsolutePath());
            assertEquals(tempDir.getAbsolutePath() + File.separator + "someid" + 
                    File.separator + DockerCytoContainerRunner.STD_ERR_FILE, 
                    runner.getStandardErrorFile().getAbsolutePath());
            
            CytoContainerResult cdRes = runner.createCytoContainerResult();
            assertEquals(3, cdRes.getStartTime());
            assertEquals(0, cdRes.getProgress());
            assertEquals(CytoContainerResult.PROCESSING_STATUS, cdRes.getStatus());
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testConstructorSuccessfulJsonData() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            
            try (BufferedReader br = new BufferedReader(new FileReader(runner.getInputFile()))){
                assertEquals("{\"blah\":\"data\"}", br.readLine());
            }
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testUpdateCytoContainerResultWithFileContentsOutFileDoesNotExist() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            File nonExistantFile = new File(tempDir.getAbsolutePath() + File.separator + "doesnotexist");
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            CytoContainerResult res = new CytoContainerResult();
            res.setId("1");
            res.setResult(TextNode.valueOf("hello"));
            runner.updateCytoContainerResultWithFileContents(res, nonExistantFile);
            assertFalse(nonExistantFile.exists());
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testUpdateCytoContainerResultWithFileContentsJson() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            File outFile = new File(tempDir.getAbsolutePath() + File.separator + "somefile");
            FileWriter fw = new FileWriter(outFile.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(fw, TextNode.valueOf("hello"));

            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            CytoContainerResult res = new CytoContainerResult();
            res.setId("1");
            res.setResult(TextNode.valueOf("hello"));
            runner.updateCytoContainerResultWithFileContents(res, outFile);
            assertTrue(outFile.exists());
            assertEquals("hello", res.getResult().asText());
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testUpdateCytoContainerResultWithFileContentsText() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            File outFile = new File(tempDir.getAbsolutePath() + File.separator + "somefile");
            FileWriter fw = new FileWriter(outFile.getAbsolutePath());
            fw.write("hello");
            fw.flush();
            fw.close();
            ObjectMapper mapper = new ObjectMapper();
            

            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            CytoContainerResult res = new CytoContainerResult();
            res.setId("1");
            res.setResult(TextNode.valueOf("hello"));
            runner.updateCytoContainerResultWithFileContents(res, outFile);
            assertTrue(outFile.exists());
            assertEquals("hello\n", res.getResult().asText());
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testUpdateCytoContainerResultZeroExitCode() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            File outFile = new File(tempDir.getAbsolutePath() + File.separator + "somefile");
            FileWriter fw = new FileWriter(outFile.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(fw, TextNode.valueOf("hello"));

            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            CytoContainerResult res = new CytoContainerResult();
            res.setId("1");
            res.setResult(TextNode.valueOf("hello"));
            runner.updateCytoContainerResult(0, outFile, null, res);
            
            assertEquals("hello", res.getResult().asText());
            assertEquals(CytoContainerResult.COMPLETE_STATUS, res.getStatus());
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testUpdateCytoContainerResult500ExitCode() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            File outFile = new File(tempDir.getAbsolutePath() + File.separator + "somefile");
            FileWriter fw = new FileWriter(outFile.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(fw, TextNode.valueOf("hello"));

            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            CytoContainerResult res = new CytoContainerResult();
            res.setId("1");
            res.setResult(TextNode.valueOf("hello"));
            runner.updateCytoContainerResult(500, null, outFile, res);
            
            assertEquals("hello", res.getResult().asText());
            assertEquals(CytoContainerResult.FAILED_STATUS, res.getStatus());
            assertEquals("Runtime limit exceeded", res.getMessage());
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testUpdateCytoContainerResultNonZeroExitCode() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            File outFile = new File(tempDir.getAbsolutePath() + File.separator + "somefile");
            FileWriter fw = new FileWriter(outFile.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(fw, TextNode.valueOf("hello"));

            CytoContainerRequest cdr = new CytoContainerRequest();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            CytoContainerResult res = new CytoContainerResult();
            res.setId("1");
            res.setResult(TextNode.valueOf("hello"));
            runner.updateCytoContainerResult(1, null, outFile, res);
            
            assertEquals("hello", res.getResult().asText());
            assertEquals(CytoContainerResult.FAILED_STATUS, res.getStatus());
            assertEquals("Received non zero exit code: 1 when running algorithm for task: 1", res.getMessage());
        }finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testWriteCommandRunToFile() throws IOException, Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            CommandLineRunner mockCLR = mock(CommandLineRunner.class);
            expect(mockCLR.getLastCommand()).andReturn("command");
            runner.setAlternateCommandLineRunner(mockCLR);
            replay(mockCLR);
            
            runner.writeCommandRunToFile();
            
            
            try (BufferedReader br = new BufferedReader(new FileReader(runner.getCommandRunFile()))){
                assertEquals("command", br.readLine());
            } 
           
           
            
            verify(mockCLR);
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testWriteCommandRunToFileFailedToWrite() throws IOException, Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath() + File.separator + "doesnotexist", "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            CommandLineRunner mockCLR = mock(CommandLineRunner.class);
            runner.setAlternateCommandLineRunner(mockCLR);
            replay(mockCLR);
            assertTrue(runner.getCommandRunFile().mkdirs());
            runner.writeCommandRunToFile();
            
            assertTrue(runner.getCommandRunFile().isDirectory());
            verify(mockCLR);
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testCallWorkingDirectoryDoesNotExist() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            String workingDir = tempDir.getAbsolutePath() 
                    + File.separator + "task";
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, workingDir, "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            File workingDirFile = new File(workingDir);
            assertTrue(FileUtils.deleteQuietly(workingDirFile));
            CommandLineRunner mockCLR = mock(CommandLineRunner.class);
            mockCLR.setWorkingDirectory(workingDir + File.separator + "someid");
            runner.setAlternateCommandLineRunner(mockCLR);
            replay(mockCLR);
            try {
                CytoContainerResult res = runner.call();
                assertEquals(CytoContainerResult.FAILED_STATUS, res.getStatus());
                assertEquals(100, res.getProgress());
                assertTrue(res.getWallTime() >= 0);
                assertEquals("Received error trying to run task: " 
                        + workingDir + File.separator + "someid directory does not exist" ,
                        res.getMessage());
                
            } catch(Exception ex){
                fail("Unexpected Exception: " + ex.getMessage());
            }
            verify(mockCLR);
            
        } finally {
            _folder.delete();
        }
               
    }
    
    @Test
    public void testCallWorkingDirectoryNoCustomParameters() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            String workingDir = tempDir.getAbsolutePath() 
                    + File.separator + "task";
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, workingDir, "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro");
            
            CommandLineRunner mockCLR = mock(CommandLineRunner.class);
            String wDir = workingDir + File.separator + "someid";
            mockCLR.setWorkingDirectory(wDir);
            File stdOutFile = runner.getStandardOutFile();
            
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(stdOutFile))){
                bw.write("hello");
                bw.flush();
            } 
            File inputFile = runner.getInputFile();
            File stdErrFile = runner.getStandardErrorFile();
            expect(mockCLR.runCommandLineProcess(1, TimeUnit.SECONDS, stdOutFile, stdErrFile,
                    "docker", "run", "--rm", "-v", wDir + ":" + wDir + ":ro", "hello-world", inputFile.getAbsolutePath())).andReturn(0);
            expect(mockCLR.getLastCommand()).andReturn("lastcommand");
            runner.setAlternateCommandLineRunner(mockCLR);
            replay(mockCLR);
            try {
                CytoContainerResult res = runner.call();
                assertEquals(CytoContainerResult.COMPLETE_STATUS, res.getStatus());
                assertEquals(100, res.getProgress());
                assertTrue(res.getWallTime() >= 0);
                assertNull(res.getMessage());
                
            } catch(Exception ex){
                fail("Unexpected Exception: " + ex.getMessage());
            }
            verify(mockCLR);
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testCallWorkingDirectoryWithCustomParameters() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            Map<String, String> cParams = new LinkedHashMap<>();
            cParams.put("flagonly", null);
            cParams.put("somearg", "argvalue");
            
            String workingDir = tempDir.getAbsolutePath() 
                    + File.separator + "task";
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, workingDir, "docker", "hello-world", cParams, 1,
                    TimeUnit.SECONDS, ":ro");
            
            CommandLineRunner mockCLR = mock(CommandLineRunner.class);
            String wDir = workingDir + File.separator + "someid";
            mockCLR.setWorkingDirectory(wDir);
            File stdOutFile = runner.getStandardOutFile();
            
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(stdOutFile))){
                bw.write("hello");
                bw.flush();
            } 
            File inputFile = runner.getInputFile();
            File stdErrFile = runner.getStandardErrorFile();
            expect(mockCLR.runCommandLineProcess(1, TimeUnit.SECONDS, stdOutFile, stdErrFile,
                    "docker", "run", "--rm", "-v", wDir + ":" + wDir + ":ro", "hello-world",
                    inputFile.getAbsolutePath(),"flagonly", "somearg", "argvalue")).andReturn(0);
            expect(mockCLR.getLastCommand()).andReturn("lastcommand");
            runner.setAlternateCommandLineRunner(mockCLR);
            replay(mockCLR);
            try {
                CytoContainerResult res = runner.call();
                assertEquals(CytoContainerResult.COMPLETE_STATUS, res.getStatus());
                assertEquals(100, res.getProgress());
                assertTrue(res.getWallTime() >= 0);
                assertNull(res.getMessage());
                
            } catch(Exception ex){
                fail("Unexpected Exception: " + ex.getMessage());
            }
            verify(mockCLR);
            
        } finally {
            _folder.delete();
        }
    }
    @Test
    public void testWriteUploadedFilesNullFiles() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro", false, null, null);

            // Should not throw — just returns without doing anything
            File workDir = new File(tempDir.getAbsolutePath() + File.separator + "someid");
            // Only input.txt should exist
            assertEquals(1, workDir.listFiles().length);
            assertEquals(DockerCytoContainerRunner.INPUT_FILE, workDir.listFiles()[0].getName());
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testWriteUploadedFilesEmptyMap() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));
            Map<String, InputStream> files = new HashMap<>();
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro", false, null, files);

            File workDir = new File(tempDir.getAbsolutePath() + File.separator + "someid");
            assertEquals(1, workDir.listFiles().length);
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testWriteUploadedFilesSingleFile() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));

            String fileContent = "{\"nodes\": []}";
            Map<String, InputStream> files = new HashMap<>();
            files.put("network.cx2", new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)));

            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro", false, null, files);

            File workDir = new File(tempDir.getAbsolutePath() + File.separator + "someid");
            File uploadedFile = new File(workDir, "network.cx2");
            assertTrue(uploadedFile.exists());

            try (BufferedReader br = new BufferedReader(new FileReader(uploadedFile))) {
                assertEquals(fileContent, br.readLine());
            }
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testWriteUploadedFilesMultipleFiles() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));

            Map<String, InputStream> files = new HashMap<>();
            files.put("network.cx2", new ByteArrayInputStream("cx2data".getBytes(StandardCharsets.UTF_8)));
            files.put("features.tsv", new ByteArrayInputStream("col1\tcol2\n".getBytes(StandardCharsets.UTF_8)));

            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, tempDir.getAbsolutePath(), "docker", "hello-world", null, 1,
                    TimeUnit.SECONDS, ":ro", false, null, files);

            File workDir = new File(tempDir.getAbsolutePath() + File.separator + "someid");
            assertTrue(new File(workDir, "network.cx2").exists());
            assertTrue(new File(workDir, "features.tsv").exists());

            try (BufferedReader br = new BufferedReader(new FileReader(new File(workDir, "network.cx2")))) {
                assertEquals("cx2data", br.readLine());
            }
            try (BufferedReader br = new BufferedReader(new FileReader(new File(workDir, "features.tsv")))) {
                assertEquals("col1\tcol2", br.readLine());
            }
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testCallWithUploadedFilesAndFileFlags() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CytoContainerRequest cdr = new CytoContainerRequest();
            ObjectMapper mapper = new ObjectMapper();
            cdr.setData(mapper.readTree("{\"blah\": \"data\"}"));

            Map<String, String> cParams = new LinkedHashMap<>();
            cParams.put("--network", "network.cx2");

            Map<String, InputStream> files = new HashMap<>();
            files.put("network.cx2", new ByteArrayInputStream("cx2data".getBytes(StandardCharsets.UTF_8)));

            String workingDir = tempDir.getAbsolutePath() + File.separator + "task";
            DockerCytoContainerRunner runner = new DockerCytoContainerRunner("someid", cdr,
                    0, workingDir, "docker", "hello-world", cParams, 1,
                    TimeUnit.SECONDS, ":ro", false, null, files);

            CommandLineRunner mockCLR = mock(CommandLineRunner.class);
            String wDir = workingDir + File.separator + "someid";
            mockCLR.setWorkingDirectory(wDir);
            File stdOutFile = runner.getStandardOutFile();

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(stdOutFile))) {
                bw.write("hello");
                bw.flush();
            }
            File inputFile = runner.getInputFile();
            File stdErrFile = runner.getStandardErrorFile();
            expect(mockCLR.runCommandLineProcess(1, TimeUnit.SECONDS, stdOutFile, stdErrFile,
                    "docker", "run", "--rm", "-v", wDir + ":" + wDir + ":ro", "hello-world",
                    inputFile.getAbsolutePath(), "--network", "network.cx2")).andReturn(0);
            expect(mockCLR.getLastCommand()).andReturn("lastcommand");
            runner.setAlternateCommandLineRunner(mockCLR);
            replay(mockCLR);

            CytoContainerResult res = runner.call();
            assertEquals(CytoContainerResult.COMPLETE_STATUS, res.getStatus());
            assertEquals(100, res.getProgress());

            // Verify uploaded file was written
            File uploadedFile = new File(wDir, "network.cx2");
            assertTrue(uploadedFile.exists());

            verify(mockCLR);
        } finally {
            _folder.delete();
        }
    }


}
