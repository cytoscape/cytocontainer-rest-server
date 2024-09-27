package org.cytoscape.cytocontainer.rest.engine.util;

import org.cytoscape.cytocontainer.rest.engine.util.CommandLineRunnerImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author churas
 */
public class TestCommandLineRunnerImpl {
    
    public static String FALSE_BINARY = File.separator+"bin"+File.separator+"false";
    
    public TestCommandLineRunnerImpl() {
        FALSE_BINARY = getBinary(FALSE_BINARY);
    }
    
    public static String getBinary(final String basePath){
        File baseCheck = new File(basePath);
        if (!baseCheck.exists()){
            return File.separator + "usr" + basePath;
        }
        return basePath;
    }
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    @Test
    public void testRunCommandLineProcessNoArgs() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CommandLineRunnerImpl runner = new CommandLineRunnerImpl();
            File stdoutfile = new File(tempDir.getAbsolutePath() + File.separator + "stdout");
            File stderrfile = new File(tempDir.getAbsolutePath() + File.separator + "stderr");
            int eCode = runner.runCommandLineProcess(2, TimeUnit.SECONDS,
                    stdoutfile, stderrfile, "/bin/pwd");
            
            assertEquals(0, stderrfile.length());
            assertTrue(stdoutfile.length() > 0);
            assertEquals(0, eCode);
            assertEquals("/bin/pwd", runner.getLastCommand());
        }
        finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRunCommandLineProcessNonZeroExit() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CommandLineRunnerImpl runner = new CommandLineRunnerImpl();
            File stdoutfile = new File(tempDir.getAbsolutePath() + File.separator + "stdout");
            File stderrfile = new File(tempDir.getAbsolutePath() + File.separator + "stderr");
            int eCode = runner.runCommandLineProcess(2, TimeUnit.SECONDS,
                    stdoutfile, stderrfile, FALSE_BINARY);
            
            assertEquals(0, stderrfile.length());
            assertEquals(0, stdoutfile.length());
            assertEquals(1, eCode);
        }
        finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRunCommandLineProcessTimeoutExceeded() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CommandLineRunnerImpl runner = new CommandLineRunnerImpl();
            File stdoutfile = new File(tempDir.getAbsolutePath() + File.separator + "stdout");
            File stderrfile = new File(tempDir.getAbsolutePath() + File.separator + "stderr");
            int eCode = runner.runCommandLineProcess(1, TimeUnit.MILLISECONDS,
                    stdoutfile, stderrfile, "/bin/sleep", "5");
            assertEquals(500, eCode);
        }
        finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRunCommandLineProcessWorkDirSet() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CommandLineRunnerImpl runner = new CommandLineRunnerImpl();
            File stdoutfile = new File(tempDir.getAbsolutePath() + File.separator + "stdout");
            File stderrfile = new File(tempDir.getAbsolutePath() + File.separator + "stderr");
            runner.setWorkingDirectory(tempDir.getAbsolutePath());
            int eCode = runner.runCommandLineProcess(2, TimeUnit.SECONDS,
                    stdoutfile, stderrfile, "/bin/pwd", "");
            
            assertEquals(0, stderrfile.length());
            assertTrue(stdoutfile.length() > 0);
            assertEquals(0, eCode);
            assertEquals("/bin/pwd", runner.getLastCommand());
            try (BufferedReader br = new BufferedReader(new FileReader(stdoutfile))){
                assertTrue(br.readLine().contains(tempDir.getAbsolutePath()));
            }
        }
        finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRunCommandLineProcessMultipleArguments() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CommandLineRunnerImpl runner = new CommandLineRunnerImpl();
            File stdoutfile = new File(tempDir.getAbsolutePath() + File.separator + "stdout");
            File stderrfile = new File(tempDir.getAbsolutePath() + File.separator + "stderr");
            runner.setWorkingDirectory(tempDir.getAbsolutePath());
            int eCode = runner.runCommandLineProcess(2, TimeUnit.SECONDS,
                    stdoutfile, stderrfile, "/bin/echo", "well", "there");
            
            assertEquals(0, stderrfile.length());
            assertTrue(stdoutfile.length() > 0);
            assertEquals(0, eCode);
            assertEquals("/bin/echo well there", runner.getLastCommand());
            try (BufferedReader br = new BufferedReader(new FileReader(stdoutfile))){
                assertEquals("well there", br.readLine());
            }
        }
        finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRunCommandLineProcessSetEnv() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            CommandLineRunnerImpl runner = new CommandLineRunnerImpl();
            File stdoutfile = new File(tempDir.getAbsolutePath() + 
                    File.separator + "stdout");
            File stderrfile = new File(tempDir.getAbsolutePath() + 
                    File.separator + "stderr");
            
            HashMap<String,String> envMap = new HashMap<String,String>();
            envMap.put("HI", "hello");
            runner.setEnvironmentVariables(envMap);
            int eCode = runner.runCommandLineProcess(2, TimeUnit.SECONDS,
                    stdoutfile, stderrfile, "/usr/bin/env");
            
            assertEquals(0, stderrfile.length());
            assertTrue(stdoutfile.length() > 0);
            assertEquals(0, eCode);
            assertEquals("/usr/bin/env", runner.getLastCommand());
            try (BufferedReader br = new BufferedReader(new FileReader(stdoutfile))){
                String line = br.readLine();
                boolean foundLine = false;
                while(line != null){
                    if (line.startsWith("HI=hello")){
                        foundLine = true;
                    }
                    line = br.readLine();
                }
                assertTrue("Looking for HI=hello in output", foundLine);
            }
        }
        finally {
            _folder.delete();
        }
    }
    
    
}
