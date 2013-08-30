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
package org.kie.tests.wb.base.methods;

import static org.junit.Assert.*;
import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.drools.core.command.runtime.process.GetProcessInstanceCommand;
import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jbpm.services.task.commands.CompleteTaskCommand;
import org.jbpm.services.task.commands.GetTasksByProcessInstanceIdCommand;
import org.jbpm.services.task.commands.GetTasksOwnedCommand;
import org.jbpm.services.task.commands.StartTaskCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.RemoteJmsRuntimeEngineFactory;
import org.kie.services.client.api.command.RemoteRuntimeException;
import org.kie.services.client.serialization.jaxb.JaxbCommandsRequest;
import org.kie.services.client.serialization.jaxb.JaxbCommandsResponse;
import org.kie.services.client.serialization.jaxb.JaxbSerializationProvider;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbProcessInstanceResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbTaskSummaryListResponse;

public class JmsIntegrationTestMethods extends AbstractIntegrationTestMethods {

    private static final String CONNECTION_FACTORY_NAME = "jms/RemoteConnectionFactory";
    private static final String KSESSION_QUEUE_NAME = "jms/queue/KIE.SESSION";
    private static final String TASK_QUEUE_NAME = "jms/queue/KIE.TASK";
    private static final String RESPONSE_QUEUE_NAME = "jms/queue/KIE.RESPONSE";

    private static final long QUALITY_OF_SERVICE_THRESHOLD_MS = 5 * 1000;

    private final String deploymentId;
    private final InitialContext remoteInitialContext;
    
    public JmsIntegrationTestMethods(String deploymentId) { 
        this.deploymentId = deploymentId;
        this.remoteInitialContext = getRemoteInitialContext();
    }
    // Helper methods ------------------------------------------------------------------------------------------------------------
    
    /**
     * JMS initial context
     */
    
    /**
     * Initializes a (remote) IntialContext instance.
     * 
     * @return a remote {@link InitialContext} instance
     */
    public static InitialContext getRemoteInitialContext() {
        Properties initialProps = new Properties();
        initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://localhost:4447");
        initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, USER );
        initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, PASSWORD );
        
        for (Object keyObj : initialProps.keySet()) {
            String key = (String) keyObj;
            System.setProperty(key, (String) initialProps.get(key));
        }
        try {
            return new InitialContext(initialProps);
        } catch (NamingException e) {
            throw new RuntimeException("Unable to create " + InitialContext.class.getSimpleName(), e);
        }
    }
    
    // Tests ----------------------------------------------------------------------------------------------------------------------
    
    public void startProcess(String user, String password) throws Exception {
        
        // send cmd
        Command<?> cmd = new StartProcessCommand("org.jbpm.humantask"); 
        JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
        JaxbCommandsResponse response = sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);
        
        // check response 
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses",  response.getResponses() != null && response.getResponses().size() > 0);
        JaxbCommandResponse<?> cmdResponse = response.getResponses().get(0);
        assertTrue( "response is not the proper class type : " + cmdResponse.getClass().getSimpleName(), cmdResponse instanceof JaxbProcessInstanceResponse );
        ProcessInstance procInst = (ProcessInstance) cmdResponse;
        long procInstId = procInst.getId();
       
        // send cmd
        cmd = new GetTasksByProcessInstanceIdCommand(procInstId);
        req = new JaxbCommandsRequest(deploymentId, cmd);
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        
        // check response 
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses",  response.getResponses() != null && response.getResponses().size() > 0);
        cmdResponse = response.getResponses().get(0);
        assertTrue( "response is not the proper class type : " + cmdResponse.getClass().getSimpleName(), cmdResponse instanceof JaxbLongListResponse );
        long taskId = ((JaxbLongListResponse) cmdResponse).getResult().get(0);
        
        // send cmd
        cmd = new StartTaskCommand(taskId, SALA_USER);
        req = new JaxbCommandsRequest(deploymentId, cmd);
        req.getCommands().add(new CompleteTaskCommand(taskId, SALA_USER, null));
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        
        // check response 
        assertNotNull("response was null.", response);
        assertTrue("response list was not empty", response.getResponses().size() == 0);
        
        // send cmd
        cmd = new GetTasksOwnedCommand(SALA_USER, "en-UK");
        req = new JaxbCommandsRequest(deploymentId, cmd);
        req.getCommands().add(new GetTasksOwnedCommand("bob", "fr-CA"));
        req.getCommands().add(new GetProcessInstanceCommand(procInstId));
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses",  response.getResponses() != null && response.getResponses().size() > 0);
        cmdResponse = response.getResponses().get(0);
        assertTrue( "response is not the proper class type : " + cmdResponse.getClass().getSimpleName(), cmdResponse instanceof JaxbTaskSummaryListResponse );
        List<TaskSummary> taskSummaries = ((JaxbTaskSummaryListResponse) cmdResponse).getResult();
        assertTrue( "task summary list is empty", taskSummaries.size() > 0);
        for( TaskSummary taskSum : taskSummaries ) { 
            if( taskSum.getId() == taskId ) { 
                assertTrue( "Task " + taskId + " should have completed.", taskSum.getStatus().equals(Status.Completed));
            }
        }
        
        cmdResponse = response.getResponses().get(1);
        assertTrue( "response is not the proper class type : " + cmdResponse.getClass().getSimpleName(), cmdResponse instanceof JaxbTaskSummaryListResponse );
        taskSummaries = ((JaxbTaskSummaryListResponse) cmdResponse).getResult();
        assertTrue( "task summary list should be empty, but has " + taskSummaries.size() + " elements", taskSummaries.size() == 0);
        cmdResponse = response.getResponses().get(2);
        assertNotNull(cmdResponse);
    }
    
    private JaxbCommandsResponse sendJmsJaxbCommandsRequest(String sendQueueName, JaxbCommandsRequest req, String USER, String PASSWORD) throws Exception { 
        ConnectionFactory factory = (ConnectionFactory) remoteInitialContext.lookup(CONNECTION_FACTORY_NAME);
        Queue jbpmQueue = (Queue) remoteInitialContext.lookup(sendQueueName);
        Queue responseQueue = (Queue) remoteInitialContext.lookup(RESPONSE_QUEUE_NAME);

        Connection connection = null;
        Session session = null;
        JaxbCommandsResponse cmdResponse = null;
        try {
            // setup
            connection = factory.createConnection(USER, PASSWORD);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer producer = session.createProducer(jbpmQueue);
            String corrId = UUID.randomUUID().toString();
            String selector = "JMSCorrelationID = '" + corrId + "'";
            MessageConsumer consumer = session.createConsumer(responseQueue, selector);

            connection.start();

            // Create msg
            BytesMessage msg = session.createBytesMessage();
            msg.setJMSCorrelationID(corrId);
            msg.setIntProperty("serialization", 1);
            String xmlStr = JaxbSerializationProvider.convertJaxbObjectToString(req);
            msg.writeUTF(xmlStr);
            
            // send
            producer.send(msg);
            
            // receive
            Message response = consumer.receive(QUALITY_OF_SERVICE_THRESHOLD_MS);
            
            // check
            assertNotNull("Response is empty.", response);
            assertEquals("Correlation id not equal to request msg id.", corrId, response.getJMSCorrelationID() );
            assertNotNull("Response from MDB was null!", response);
            xmlStr = ((BytesMessage) response).readUTF();
            cmdResponse = (JaxbCommandsResponse) JaxbSerializationProvider.convertStringToJaxbObject(xmlStr);
            assertNotNull("Jaxb Cmd Response was null!", cmdResponse);
        } finally {
            if (connection != null) {
                connection.close();
                session.close();
            }
        }
        return cmdResponse;
    }

    public void remoteApiHumanTaskProcess() throws Exception {
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext);
        internalRemoteApiHumanTaskProcess(remoteSessionFactory);
    }
    
    public void remoteApiHumanTaskProcess(String user, String password) throws Exception {
        RemoteJmsRuntimeEngineFactory remoteSessionFactory 
            = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext, user, password);
        internalRemoteApiHumanTaskProcess(remoteSessionFactory);
    }
    
    
    private void internalRemoteApiHumanTaskProcess(RemoteJmsRuntimeEngineFactory remoteSessionFactory) throws Exception {
        // create JMS request
        RuntimeEngine engine = remoteSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess("org.jbpm.humantask");
        
        logger.debug("Started process instance: " + processInstance + " " + (processInstance == null? "" : processInstance.getId()));
        
        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(SALA_USER, "en-UK");
        long taskId = findTaskId(processInstance.getId(), tasks);
        
        logger.debug("Found task " + taskId);
        Task task = taskService.getTaskById(taskId);
        logger.debug("Got task " + taskId + ": " + task );
        taskService.start(taskId, SALA_USER);
        taskService.complete(taskId, SALA_USER, null);
        
        logger.debug("Now expecting failure");
        try {
            taskService.complete(taskId, SALA_USER, null);
            fail( "Should not have been able to complete task " + taskId + " a second time.");
        } catch (Throwable t) {
            // do nothing
        }
        
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(processInstance.getId(), statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }
    
    public void remoteApiException(String user, String password) throws Exception {
        RemoteJmsRuntimeEngineFactory remoteSessionFactory 
            = new RemoteJmsRuntimeEngineFactory("non-existing-deployment", remoteInitialContext, user, password);
        
        // create JMS request
        RuntimeEngine engine = remoteSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        try { 
            ProcessInstance processInstance = ksession.startProcess("org.jbpm.humantask");
            fail("startProcess should fail!");
        } catch( RemoteRuntimeException rre) { 
            String errMsg = rre.getMessage();
            assertTrue( "Incorrect error message: " + errMsg, errMsg.startsWith("DomainNotFoundBadRequestException"));
        }
    }
        
}