package org.cytoscape.cytocontainer.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.cytoscape.cytocontainer.rest.services.Configuration;

/**
 *
 * @author churas
 */
public class TestApp {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    @Test
    public void testGenerateExampleConfiguration() throws Exception{
        String res = App.generateExampleConfiguration();
        assertTrue(res.contains("# Example configuration file for Cytoscape Container REST service"));
    }
    
    @Test
    public void testExampleConf(){
        String[] args = {"--mode", App.EXAMPLE_CONF_MODE};
        App.main(args);
    }
    
    @Test
    public void testExampleAlgo(){
        String[] args = {"--mode", App.EXAMPLE_ALGO_MODE};
        App.main(args);
    }
    
    @Test
    public void testGetClassesToParseByOpenApi() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            assertEquals("org.cytoscape.cytocontainer.rest.services.CytoContainer,"
                    + "org.cytoscape.cytocontainer.rest.services.Status",
                    App.getClassesToParseByOpenApi());
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetApplicationPath(){
        // try with empty props
        Properties props = new Properties();
        assertEquals("/cytocontainer", App.getApplicationPath(props));
        
        //test with runserver app path set
        props.setProperty(Configuration.RUNSERVER_APP_PATH, "/foo");
        assertEquals("/foo", App.getApplicationPath(props));
    }
    
    @Test
    public void testGetPropertiesFromConf() throws IOException {
        try {
            File confFile = _folder.newFile();
            
            Properties props = new Properties();
            props.setProperty(Configuration.RUNSERVER_APP_PATH, "/foo");
            FileWriter fw = new FileWriter(confFile.getAbsolutePath());
            props.store(fw, null);
            fw.flush();
            fw.close();
            
            Properties res = App.getPropertiesFromConf(confFile.getAbsolutePath());
            assertEquals("/foo", App.getApplicationPath(res));
        } finally {
            _folder.delete();
        }
    }

}
