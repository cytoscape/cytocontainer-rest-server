package org.cytoscape.cytocontainer.rest.engine;


import static org.easymock.EasyMock.expect;

import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithms;
import org.cytoscape.cytocontainer.rest.model.ServerStatus;
import org.cytoscape.cytocontainer.rest.services.Configuration;



/**
 *
 * @author churas
 */
public class TestCytoContainerEngineFactory {
    

   
    @Test
    public void testGetCommunityDetectionEngine() throws Exception {

        Configuration mockConfig = mock(Configuration.class);
        expect(mockConfig.getNumberWorkers()).andReturn(5);
        expect(mockConfig.getTaskDirectory()).andReturn("/task");
        expect(mockConfig.getDockerCommand()).andReturn("/bin/docker");
        CytoContainerAlgorithms cdas = new CytoContainerAlgorithms();

        expect(mockConfig.getAlgorithms()).andReturn(cdas);
        replay(mockConfig);
        CytoContainerEngineFactory factory = new CytoContainerEngineFactory(mockConfig);
        CytoContainerEngine cde = factory.getCytoContainerEngine();

        verify(mockConfig);
        ServerStatus ss = cde.getServerStatus("");
        assertEquals(ServerStatus.OK_STATUS, ss.getStatus());
    }
}