/*
 * JBoss, Home of Professional Open Source
 * 
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.kie.tests.wb.eap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.SCRIPT_TASK_PROCESS_ID;
import static org.kie.tests.wb.eap.KieWbWarJbossEapDeploy.createTestWar;

import java.net.MalformedURLException;
import java.net.URL;

import javax.jws.WebService;
import javax.xml.namespace.QName;

import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.services.ws.wsdl.generated.CommandWebService;
import org.kie.remote.services.ws.wsdl.generated.CommandWebServiceClient;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class JbossEapWebServicesIntegrationTest {

    protected static final Logger logger = LoggerFactory.getLogger(JbossEapWebServicesIntegrationTest.class);

    @Deployment(testable = false, name = "kie-wb-eap-ws")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    private KieWbRestIntegrationTestMethods restTests 
        = KieWbRestIntegrationTestMethods.newBuilderInstance().setDeploymentId(KJAR_DEPLOYMENT_ID).build();

    @ArquillianResource
    URL deploymentUrl;

    private static final String WSDL_PATH = "ws/CommandService?wsdl";

    private CommandWebServiceClient client;

    private static QName getServiceQName() {
        WebService wsAnno = CommandWebService.class.getAnnotation(WebService.class);
        String namespace = wsAnno.targetNamespace();
        String name = wsAnno.name();
        return new QName(namespace, name);
    }

    
    @Test
    public void testCommandWebService() throws Exception {
        try {
            client = new CommandWebServiceClient(new URL(deploymentUrl, WSDL_PATH), getServiceQName());
        } catch (MalformedURLException murle) {
            String msg = "Unable to create service client: " + murle.getMessage();
            logger.error(msg, murle);
            fail(msg);
        }

        if( ! checkDeployFlagFile() ) { 
            restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, false);
        }
        
        logger.info("[Client] Webservice request.");
        // Get a response from the WebService
        StartProcessCommand cmd = new StartProcessCommand(SCRIPT_TASK_PROCESS_ID);
        JaxbCommandsRequest req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, cmd);
        final WebServiceCommandsResponse response = client.getCommandServicePort().execute(null);
        assertNotNull( "Null webservice response", response );
//        assertFalse( "Empty webservice response", response.getResponses().isEmpty() );

//        JaxbCommandResponse<?> cmdResp = response.getResponses().get(0);
//        assertNotNull( "Null command response", cmdResp );
//        assertTrue( "Incorrect cmd response type", cmdResp instanceof JaxbProcessInstanceResponse );
        
//        logger.info("[WebService] response: {} [{}]", 
//                ((JaxbProcessInstanceResponse) cmdResp).getId(),
//                ((JaxbProcessInstanceResponse) cmdResp).getProcessId()
//                );
    }

}
