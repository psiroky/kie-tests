package org.kie.tests.drools.wb.base;

import java.net.URL;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.kie.tests.drools.wb.base.methods.DroolsWbRestIntegrationTestMethods;

public abstract class AbstractDroolsWbIntegrationTest {

    private DroolsWbRestIntegrationTestMethods restTests = new DroolsWbRestIntegrationTestMethods();
    
    @ArquillianResource
    URL deploymentUrl;
    
    @Test
    public void manipulatingRepositories() throws Exception {
        restTests.manipulatingRepositories(deploymentUrl);
    }
    
    @Test
    public void mavenOperations() throws Exception {
        restTests.mavenOperations(deploymentUrl);
    }
    
    @Test
    public void manipulatingOUs() throws Exception {
        restTests.manipulatingOUs(deploymentUrl);
    }
    
}
