/*
 * JBoss, Home of Professional Open Source
 * 
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.tests.wb.tomcat;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.tomcat.KieWbWarTomcatDeploy.createTestWar;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;

//@RunAsClient
//@RunWith(Arquillian.class)
public class TomcatMemoryRemoteApiIntegrationTest {

    @Deployment(testable = false, name = "kie-wb-tomcat")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    @ArquillianResource
    URL deploymentUrl;
    
    private KieWbRestIntegrationTestMethods restTests 
        = KieWbRestIntegrationTestMethods.newBuilderInstance()
        .setDeploymentId(KJAR_DEPLOYMENT_ID)
        .setMediaType( MediaType.APPLICATION_XML_TYPE)
        .build();
   
    @Test
    public void deployAndRunMemoryTest() throws Exception { 
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, false);
        restTests.urlsCreateMemoryLeakOnTomcat(deploymentUrl, MARY_USER, MARY_PASSWORD, 5000);
    }
}