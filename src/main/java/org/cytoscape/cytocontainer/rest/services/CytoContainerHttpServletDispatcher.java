/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cytoscape.cytocontainer.rest.services;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import jakarta.servlet.ServletException;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.cytoscape.cytocontainer.rest.engine.CytoContainerEngineFactory;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cytoscape.cytocontainer.rest.engine.CytoContainerEngine;
/**
 *
 * @author churas
 */
public class CytoContainerHttpServletDispatcher extends HttpServletDispatcher {
    
    static Logger _logger = LoggerFactory.getLogger(CytoContainerHttpServletDispatcher.class.getSimpleName());

    private static String _version = "";
    private static String _buildNumber = "";
    private CytoContainerEngine _communityDetectionEngine;
    private Thread _communityDetectionEngineThread;
    
    
    public CytoContainerHttpServletDispatcher() throws CytoContainerException{
        super();
        _logger.info("In constructor");
        createAndStartCytoContainerEngine();

    }
    
    protected void createAndStartCytoContainerEngine() throws CytoContainerException {
        
        try {
            CytoContainerEngineFactory fac = new CytoContainerEngineFactory(Configuration.getInstance());
            _logger.debug("Creating CytoContainer Engine from factory");
            _communityDetectionEngine = fac.getCytoContainerEngine();
            _logger.debug("Starting CytoContainer Engine thread");
            _communityDetectionEngineThread = new Thread(_communityDetectionEngine);
            _communityDetectionEngineThread.start();
            _logger.info("CytoContainer Engine thread running id => " + Long.toString(_communityDetectionEngineThread.getId()));
            Configuration.getInstance().setCytoContainerEngine(_communityDetectionEngine);
        }
        catch(CytoContainerException ex){
            _logger.error("Unable to start enrichment engine", ex);
            throw ex;
        }
    }

    @Override
    public void init(jakarta.servlet.ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        _logger.info("Entering init()");
        updateVersion();
        _logger.info("Exiting init()");
    }
    
    @Override
    public void destroy() {
        super.destroy();
        _logger.info("In destroy()");
        if (_communityDetectionEngine != null){
            _communityDetectionEngine.shutdown();
            _logger.info("Waiting for CytoContainer engine to shutdown");
            try {
                if (_communityDetectionEngineThread != null){
                    _communityDetectionEngineThread.join(10000);
                }
            }
            catch(InterruptedException ie){
                _logger.error("Caught exception waiting for community detection engine to exit", ie);
            }
        } else {
            _logger.error("No community detection engine found to destroy");
        
        }
    }
    
    /**
     * Reads /META-INFO/MANIFEST.MF for version and build information
     * setting _version and _buildNumber to those values if found.
     */
    private void updateVersion(){
        String jarPath = CytoContainerHttpServletDispatcher.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        JarFile jar = null;
        try {
            jar = new JarFile(jarPath);
            Manifest manifest = jar.getManifest();
           
            Attributes aa = manifest.getMainAttributes();	

            String ver = aa.getValue("CytoContainer-Version");
           
            _logger.info("CytoContainer: " + ver);
            if (ver != null){
                _version = ver;
            }
        } catch (IOException e) {
            _logger.error("failed to read MANIFEST.MF", e);
        } finally {
            
            if (jar != null){
                try {
                    jar.close();
                } catch(IOException io){
                    _logger.warn("Not a show stopper, but caught IOException closing jar", io);
                }
            }
        }   
        
    }
    
    public static String getVersion(){
        return _version;
    }
    
    public static String getBuildNumber(){
        return _buildNumber;
    }
}
