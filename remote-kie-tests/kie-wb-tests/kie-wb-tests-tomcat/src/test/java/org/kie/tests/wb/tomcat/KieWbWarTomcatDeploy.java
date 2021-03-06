package org.kie.tests.wb.tomcat;

import static org.kie.remote.tests.base.DeployUtil.*;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarTomcatDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarTomcatDeploy.class);

    static WebArchive createTestWar() {
        return createTestWar(true);
    }
    
    static WebArchive createTestWar(boolean replace) {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", "tomcat7", PROJECT_VERSION);

        war.addAsWebInfResource("war/logging.properties", "classes/logging.properties");

        if( replace ) { 
            String [][] jarsToReplace = {
                    { "org.jbpm", "jbpm-human-task-audit" }, 
                    { "org.jbpm", "jbpm-human-task-core" }, 
                    { "org.kie.remote", "kie-remote-services" }, 
                    { "org.kie.remote", "kie-remote-jaxb" },
                    { "org.kie.remote", "kie-remote-common" }
            };
            replaceJars(war, PROJECT_VERSION, jarsToReplace);
        }
        
        // Add data service resource for tests
        war.addPackage("org/kie/tests/wb/base/services/data");
       
        return war;
    }

    protected void printTestName() { 
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        logger.info( "] Starting " + ste.getMethodName());
    }
    
}
