package org.kie.tests.console.jboss;

import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarJbossAsDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarJbossAsDeploy.class);
    
    static WebArchive createTestWar() {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.jbpm", "jbpm-console-ng-distribution-wars", "jboss-as7", PROJECT_VERSION);
       
        String [][] jarsToReplace = {
                { "org.kie.remote", "kie-remote-services" }, 
                { "org.kie.remote", "kie-remote-common" },
                { "org.kie.remote", "kie-remote-jaxb" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
        
        // Add data service resource for tests
        war.addPackage("org/kie/tests/wb/base/services/data");
        
        return war;
    }
    
}
