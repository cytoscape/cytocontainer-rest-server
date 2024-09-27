package org.cytoscape.cytocontainer.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Properties;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithms;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.cytoscape.cytocontainer.rest.services.Configuration;

/**
 *
 * @author churas
 */
public class TestConfiguration {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    
    @Test
    public void testConfigurationNoConfigurationFound() throws IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
           
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            try {
                Configuration config = Configuration.reloadConfiguration();
                fail("Expected CytoContainerException");
            } catch(CytoContainerException ee){
                assertTrue(ee.getMessage().contains("FileNotFound Exception"));
            }
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testConfigurationConfigurationReadPermissionError() throws IOException {
        File tempDir = _folder.newFolder();
        File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
        try {
            Properties props = new Properties();
            props.setProperty("foo", "hello");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            configFile.setReadable(false);
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            try {
                Configuration config = Configuration.reloadConfiguration();
                fail("Expected CytoContainerException");
            } catch(CytoContainerException ee){
                assertTrue(ee.getMessage().contains("FileNotFound Exception"));
            }
        } finally {
            configFile.setReadable(true);
            _folder.delete();
        }
    }
    
    @Test
    public void testConfigurationAlternatePathNoMatchingProps() throws CytoContainerException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            Properties props = new Properties();
            props.setProperty("foo", "hello");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals("/tmp", config.getTaskDirectory());
            assertNull(config.getCytoContainerEngine());
            assertEquals(1, config.getNumberWorkers());
            assertEquals("docker", config.getDockerCommand());
            assertEquals(":ro", config.getMountOptions());
            assertNull(config.getSwaggerTitle());
            assertNull(config.getSwaggerDescription());
            assertEquals("/cy", config.getRunServerContextPath());
            assertEquals("/cytocontainer", config.getRunServerApplicationPath());
            assertEquals("/cy/cytocontainer", config.getSwaggerServer());
            
            
            assertEquals(null, config.getAlgorithms());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testConfigurationValidConfiguration() throws CytoContainerException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "tasks");
            Properties props = new Properties();
            props.setProperty(Configuration.TASK_DIR, taskDir.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals(taskDir.getAbsolutePath(), config.getTaskDirectory());
            assertNull(config.getCytoContainerEngine());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testParseAlgorithmMapInvalidData() throws CytoContainerException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "tasks");
            Properties props = new Properties();
            props.setProperty(Configuration.TASK_DIR, taskDir.getAbsolutePath());
            props.setProperty(Configuration.ALGORITHM_MAP, "haha");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals(taskDir.getAbsolutePath(), config.getTaskDirectory());
            assertNull(config.getCytoContainerEngine());
            assertEquals(180, config.getAlgorithmTimeOut());
            assertEquals(null, config.getAlgorithms());
        } finally {
            _folder.delete();
        }
    }
}
