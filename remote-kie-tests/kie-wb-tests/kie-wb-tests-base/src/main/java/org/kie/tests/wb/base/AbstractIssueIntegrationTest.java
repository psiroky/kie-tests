package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.AfterClass;
import org.junit.Test;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIssueIntegrationTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractIssueIntegrationTest.class);
    
    @ArquillianResource
    URL deploymentUrl;
   
    public abstract MediaType getMediaType();
   
    public AbstractIssueIntegrationTest() { 
    
    }
    
    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException {
        long sleep = 1000;
        logger.info("Waiting " + sleep / 1000 + " secs for tx's on server to close.");
        Thread.sleep(sleep);
    }

    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    public void issueTest() throws Exception { 
        printTestName();
        
        KieWbRestIntegrationTestMethods restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                .setDeploymentId(KJAR_DEPLOYMENT_ID)
                .setMediaType(getMediaType())
                .build();
        
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, false);
        restTests.urlsGroupAssignmentTest(deploymentUrl);
        restTests.remoteApiGetTaskInstance(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
    
}
