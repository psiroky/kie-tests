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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.util.Base64;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.drools.core.command.runtime.process.GetProcessIdsCommand;
import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.spi.ReaderException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.process.audit.AuditLogService;
import org.jbpm.process.audit.JPAAuditLogService;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.audit.VariableInstanceLog;
import org.jbpm.process.audit.event.AuditEvent;
import org.jbpm.services.task.commands.CompleteTaskCommand;
import org.jbpm.services.task.commands.GetTaskCommand;
import org.jbpm.services.task.commands.GetTasksByProcessInstanceIdCommand;
import org.jbpm.services.task.commands.StartTaskCommand;
import org.jbpm.services.task.impl.model.xml.JaxbContent;
import org.jbpm.services.task.impl.model.xml.JaxbTask;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.junit.Assume;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.deployment.DeploymentUnit.RuntimeStrategy;
import org.kie.services.client.api.RemoteRestRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRestRuntimeFactory;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.api.RestRequestHelper;
import org.kie.services.client.api.command.RemoteRuntimeEngine;
import org.kie.services.client.serialization.JaxbSerializationProvider;
import org.kie.services.client.serialization.JsonSerializationProvider;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandsRequest;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandsResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.audit.AbstractJaxbHistoryObject;
import org.kie.services.client.serialization.jaxb.impl.audit.JaxbHistoryLogList;
import org.kie.services.client.serialization.jaxb.impl.audit.JaxbVariableInstanceLog;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentJobResult;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit.JaxbDeploymentStatus;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnitList;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.services.client.serialization.jaxb.impl.task.JaxbTaskSummaryListResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbExceptionResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbGenericResponse;
import org.kie.tests.wb.base.services.data.JaxbProcessInstanceSummary;
import org.kie.tests.wb.base.test.objects.MyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestIntegrationTestMethods extends AbstractIntegrationTestMethods {

    private static Logger logger = LoggerFactory.getLogger(RestIntegrationTestMethods.class);

    private static final String taskUserId = "salaboy";

    private final String deploymentId;
    private final KModuleDeploymentUnit deploymentUnit;
    private boolean useFormBasedAuth = false;
    private boolean testWithHttpUrlConnection = true;
    private RuntimeStrategy strategy = RuntimeStrategy.SINGLETON;

    private MediaType mediaType;
    private final int timeout;
    private static final int DEFAULT_TIMEOUT = 10;
    
    
    public RestIntegrationTestMethods(String deploymentId, MediaType mediaType, int timeout, Boolean tomcatInstance, RuntimeStrategy strategy) {
        if( mediaType == null ) { 
            mediaType = MediaType.APPLICATION_XML_TYPE;
        }
        if( tomcatInstance == null ) { 
           tomcatInstance = false; 
        }
        if( strategy != null ) { 
            this.strategy = strategy;
        }
        
        this.deploymentId = deploymentId;
        this.deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);
        assertEquals( "Deployment unit information", deploymentId, deploymentUnit.getIdentifier());
        this.mediaType = mediaType;
        this.timeout = timeout;
        this.useFormBasedAuth = tomcatInstance;
        this.testWithHttpUrlConnection = ! this.useFormBasedAuth;
    }
    
    public RestIntegrationTestMethods(String deploymentId, MediaType mediaType, Boolean useFormBasedAuth, RuntimeStrategy strategy) {
       this(deploymentId, mediaType, DEFAULT_TIMEOUT, useFormBasedAuth, strategy);
    }
    
    public RestIntegrationTestMethods(String deploymentId, MediaType mediaType, Boolean useFormBasedAuth) {
       this(deploymentId, mediaType, DEFAULT_TIMEOUT, useFormBasedAuth, null);
    }
    
    public RestIntegrationTestMethods(String deploymentId, MediaType mediaType, int timeout) {
       this(deploymentId, mediaType, timeout, null, null);
    }
    
    public RestIntegrationTestMethods(String deploymentId, MediaType mediaType) {
        this(deploymentId, mediaType, DEFAULT_TIMEOUT, null, null);
    }

    public RestIntegrationTestMethods(String deploymentId) {
        this(deploymentId, null, DEFAULT_TIMEOUT, null, null);
    }

    public RestIntegrationTestMethods(KModuleDeploymentUnit deploymentUnit) {
        this(deploymentUnit.getIdentifier(), null, DEFAULT_TIMEOUT, null, null);
    }

    private JaxbSerializationProvider jaxbSerializationProvider = new JaxbSerializationProvider();
    {
        jaxbSerializationProvider.addJaxbClasses(MyType.class);
    }
    private JsonSerializationProvider jsonSerializationProvider = new JsonSerializationProvider();

    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private long restCallDurationLimit = 2000;

    private long sleep = 10*1000;


    /**
     * Helper methods
     */

    private ClientResponse<?> checkResponse(ClientResponse<?> responseObj) throws Exception {
        return checkResponse(responseObj, 200);
    }

    private ClientResponse<?> checkResponse(ClientResponse<?> responseObj, int status) throws Exception {
        responseObj.resetStream();
        int reqStatus = responseObj.getStatus();
        if (reqStatus != status) {
            logger.warn("Response with exception:\n" + responseObj.getEntity(String.class));
            fail("Incorrect status: " + reqStatus);
        }
        String contentType = (String) responseObj.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if( contentType != null ) { 
            if( ! (contentType.startsWith(MediaType.APPLICATION_XML)) && ! (contentType.startsWith(MediaType.APPLICATION_JSON)) ) { 
               logger.warn("Incorrect format for response: " + contentType + "\n" + responseObj.getEntity(String.class) );
               fail("Incorrect response media type: " + contentType );
            }
        }
        return responseObj;
    }

    private ClientResponse<?> get(ClientRequest restRequest) throws Exception {
        setAcceptHeader(restRequest);
        logger.debug(">> [GET " + restRequest.getHeaders().getFirst(HttpHeaderNames.ACCEPT) + "] " + restRequest.getUri());
        return checkResponse(restRequest.get());
    }

    private ClientResponse<?> post(ClientRequest restRequest) throws Exception {
        setAcceptHeader(restRequest);
        logger.debug(">> [POST " + restRequest.getHeaders().getFirst(HttpHeaderNames.ACCEPT) + "] " + restRequest.getUri());
        return checkResponse(restRequest.post());
    }
    
    private void setAcceptHeader(ClientRequest restRequest) { 
        restRequest.getHeaders().putSingle(HttpHeaderNames.ACCEPT, this.mediaType.getType() + "/" + this.mediaType.getSubtype());
        assertEquals( 1, restRequest.getHeaders().get(HttpHeaderNames.ACCEPT).size());
    }

    private ClientResponse<?> checkResponsePostTime(ClientRequest restRequest, int status) throws Exception {
        setAcceptHeader(restRequest);
        
        long before, after;
        logger.debug("BEFORE: " + sdf.format((before = System.currentTimeMillis())));
        ClientResponse<?> responseObj = checkResponse(restRequest.post(), status);
        logger.debug("AFTER: " + sdf.format((after = System.currentTimeMillis())));
        long duration = (after - before);
        assertTrue("Call took longer than " + restCallDurationLimit / 1000 + " seconds: " + duration + "ms", duration < restCallDurationLimit);
        return responseObj;
    }

    private void addToRequestBody(ClientRequest restRequest, Object obj) throws Exception {
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            String body = jaxbSerializationProvider.serialize(obj);
            restRequest.body(mediaType, body);
        } else if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            String body = jsonSerializationProvider.serialize(obj);
            restRequest.body(mediaType, body);
        }
    }

    private RestRequestHelper getRestRequestHelper(URL deploymentUrl, String user, String password) { 
        return RestRequestHelper.newInstance(deploymentUrl, user, password, timeout, mediaType, useFormBasedAuth);
    }
   
    private RemoteRuntimeEngineFactory getRemoteRuntimeFactory(URL deploymentUrl, String user, String password) { 
       return getRemoteRuntimeFactory(deploymentId, deploymentUrl, user, password);
    }
    
    private RemoteRuntimeEngineFactory getRemoteRuntimeFactory(String deploymentId, URL deploymentUrl, String user, String password) { 
        RemoteRestRuntimeEngineFactory r3eFactory = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password)
                .useFormBasedAuth(useFormBasedAuth)
                .build();
        return r3eFactory;
    }
    
    /**
     * Test methods
     */

    public static boolean checkDeployFlagFile() throws Exception { 
        Properties props = new Properties();
        props.load(RestIntegrationTestMethods.class.getResourceAsStream("/test.properties"));
        String buildDir = (String) props.get("build.dir");

        File deployFlag = new File(buildDir + "/deployed");
        if (!deployFlag.exists()) {
            PrintWriter output = null;
            try  {
                output = new PrintWriter(buildDir + "/deployed");
                String date = sdf.format(new Date());
                output.println(date);
            } finally { 
                if( output != null ) { 
                    output.close();
                }
            }
            return false;
        } else {
            String string = FileUtils.readFileToString(deployFlag);
            logger.debug("Deployed on " + string );
            return true;
        } 
    }

    /**
     * Tests deploy and undeploy methods..
     * 
     * @param deploymentUrl
     * @param user
     * @param password
     * @param mediaType
     * @param undeploy Whether or not to test the undeploy operation
     * @throws Exception if anything goes wrong
     */
    public void urlsDeployModuleForOtherTests(URL deploymentUrl, String user, String password) throws Exception {
        Assume.assumeFalse(checkDeployFlagFile());
        
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
    
        // Check list of deployments 
        ClientRequest restRequest = requestHelper.createRequest("deployment/");
        ClientResponse<?> responseObj = get(restRequest);
        JaxbDeploymentUnitList depList = responseObj.getEntity(JaxbDeploymentUnitList.class);
        assertNotNull( "Null answer!", depList);
        assertNotNull( "Null deployment list!", depList.getDeploymentUnitList() );
   
        // Check deployment 
        String deploymentId = (new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION)).getIdentifier();
    
        restRequest = requestHelper.createRequest("deployment/" + deploymentId + "/");
    
        if (isUndeployed(deploymentUnit, restRequest.get())) {
            // Deploy
            deploy(deploymentUnit, user, password, deploymentUrl);
        } 
    }

    private JaxbDeploymentJobResult deploy(KModuleDeploymentUnit depUnit, String user, String password, URL appUrl) throws Exception {
        logger.info("deploy");
        // This code has been refactored but is essentially the same as the org.jboss.qa.bpms.rest.wb.RestWorkbenchClient code
    
        // Create request
        String url = appUrl.toExternalForm() + "rest/deployment/" + depUnit.getIdentifier() + "/deploy";
        if (strategy.equals(RuntimeStrategy.SINGLETON)) {
            url += "?strategy=" + strategy.toString();
        }
        ClientRequest request;
        if( useFormBasedAuth ) { 
            ClientRequestFactory factory = RestRequestHelper.createRequestFactory(appUrl, user, password, useFormBasedAuth);
            request = factory.createRequest(url);
        } else { 
            String AUTH_HEADER = "Basic " + Base64.encodeBase64String(String.format("%s:%s", user, password).getBytes()).trim();

            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                    new UsernamePasswordCredentials(user, password));
            ClientExecutor clientExecutor = new ApacheHttpClient4Executor(httpClient);
            ClientRequestFactory factory = new ClientRequestFactory(clientExecutor, ResteasyProviderFactory.getInstance());

            request = factory.createRequest(url).header("Authorization", AUTH_HEADER).accept(mediaType)
                    .followRedirects(true);
        } 
    
        // ADDED CODE TO CHECK RESPONSE TIME
        long before, after;
        logger.debug("BEFORE POST: " + sdf.format((before = System.currentTimeMillis())));
    
        // POST request
        JaxbDeploymentJobResult result = null;
        ClientResponse<JaxbDeploymentJobResult> response = null;
        try {
            response = request.post(JaxbDeploymentJobResult.class);
        } catch (Exception ex) {
            logger.error("POST operation failed.", ex);
            fail("POST operation failed.");
        }
        assertNotNull("Response is null!", response);
    
        // ADDED CODE TO CHECK RESPONSE TIME
        logger.debug("AFTER POST:  " + sdf.format((after = System.currentTimeMillis())));
        assertTrue("Call took longer than " + restCallDurationLimit / 1000 + " seconds", (after - before) < restCallDurationLimit);
    
        // Retrieve request
        try {
            String contentType = response.getHeaders().getFirst("Content-Type");
            if (MediaType.APPLICATION_JSON.equals(contentType) || MediaType.APPLICATION_XML.equals(contentType)) {
                result = response.getEntity();
            } else {
                logger.error("Response body: {}",
                // get response as string
                        response.getEntity(String.class)
                        // improve HTML readability
                                .replaceAll("><", ">\n<"));
                fail("Unexpected content-type: " + contentType);
            }
        } catch (ReaderException ex) {
            response.resetStream();
            logger.error("Bad entity: [{}]", response.getEntity(String.class));
            fail("Bad entity: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unmarshalling failed.", ex);
            fail("Unmarshalling entity failed: " + ex.getMessage());
        }
   
        assertNotNull("Null response!", result);
        assertTrue("The deployment unit was not created successfully.", result.isSuccess());
      
        // wait for deploy to succeed
        RestRequestHelper requestHelper = getRestRequestHelper(appUrl, user, password);
        waitForDeploymentJobToSucceed(depUnit, true, appUrl, requestHelper);
    
        return result;
    }

    private void undeploy(KModuleDeploymentUnit kDepUnit, URL deploymentUrl, RestRequestHelper requestHelper) throws Exception {
        logger.info("undeploy");
        // Exists, so undeploy
        ClientRequest restRequest = requestHelper.createRequest("deployment/" + kDepUnit.getIdentifier() + "/undeploy");
    
        ClientResponse<?> responseObj = checkResponsePostTime(restRequest, 202);
    
        JaxbDeploymentJobResult jaxbJobResult = responseObj.getEntity(JaxbDeploymentJobResult.class);
        assertEquals("Undeploy operation", jaxbJobResult.getOperation(), "UNDEPLOY");
        logger.info("UNDEPLOY : [" + jaxbJobResult.getDeploymentUnit().getStatus().toString() + "]"
                + jaxbJobResult.getExplanation());
   
        waitForDeploymentJobToSucceed(kDepUnit, false, deploymentUrl, requestHelper);
    }

    private void waitForDeploymentJobToSucceed(KModuleDeploymentUnit kDepUnit, boolean deploy, URL deploymentUrl, RestRequestHelper requestHelper) throws Exception {
        boolean success = false;
        int tries = 0;
        while (!success && tries++ < MAX_TRIES) {
            ClientRequest restRequest = requestHelper.createRequest("deployment/" + kDepUnit.getIdentifier() + "/");
            logger.debug(">> " + restRequest.getUri());
            ClientResponse<?> responseObj = restRequest.get();
            success = isDeployRequestComplete(kDepUnit, deploy, responseObj);
            if (!success) {
                logger.info("Sleeping for " + sleep / 1000 + " seconds");
                Thread.sleep(sleep);
            }
        }
        assertTrue( "No result after " + MAX_TRIES + " checks.", tries < MAX_TRIES );
    }

    private boolean isDeployed(KModuleDeploymentUnit kDepUnit, ClientResponse<?> responseObj) {
       return isDeployRequestComplete(kDepUnit, true, responseObj);
    }
    
    private boolean isUndeployed(KModuleDeploymentUnit kDepUnit, ClientResponse<?> responseObj) {
       return isDeployRequestComplete(kDepUnit, false, responseObj);
    }
    
    private boolean isDeployRequestComplete(KModuleDeploymentUnit kDepUnit, boolean deploy, ClientResponse<?> responseObj) {
        try {
            int status = responseObj.getStatus();
            if (status == 200) {
                JaxbDeploymentUnit jaxbDepUnit = responseObj.getEntity(JaxbDeploymentUnit.class);
                JaxbDeploymentStatus jaxbDepStatus = checkJaxbDeploymentUnitAndGetStatus(kDepUnit, jaxbDepUnit);
                if( deploy && jaxbDepStatus == JaxbDeploymentStatus.DEPLOYED) {
                    return true;
                } else if( ! deploy && jaxbDepStatus.equals(JaxbDeploymentStatus.UNDEPLOYED) ) { 
                    return true;
                } else { 
                   return false; 
                }
            } else {  
                if( status < 500 ) { 
                    if( deploy ) { 
                        return false;
                    } else { 
                        return true;
                    }
                } else { 
                    throw new IllegalStateException();
                }
            }
        } catch( Exception e ) { 
           logger.error( "Unable to check if deployed: " + responseObj.getEntity(String.class)); 
           return false;
        } finally {
            responseObj.releaseConnection();
        }
    }

    private JaxbDeploymentStatus checkJaxbDeploymentUnitAndGetStatus(KModuleDeploymentUnit expectedDepUnit, JaxbDeploymentUnit jaxbDepUnit) {
        assertEquals("GroupId", expectedDepUnit.getGroupId(), jaxbDepUnit.getGroupId());
        assertEquals("ArtifactId", expectedDepUnit.getArtifactId(), jaxbDepUnit.getArtifactId());
        assertEquals("Version", expectedDepUnit.getVersion(), jaxbDepUnit.getVersion());
        return jaxbDepUnit.getStatus();
    }

    /**
     * Test human task REST operations (pure REST API)
     * 
     * @param deploymentUrl
     * @param user
     * @param password
     * @throws Exception
     */
    public void urlsStartHumanTaskProcess(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        RestRequestHelper queryRequestHelper = getRestRequestHelper(deploymentUrl, JOHN_USER, JOHN_PASSWORD);

        // Start process
        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + HUMAN_TASK_PROCESS_ID + "/start");
        ClientResponse<?> responseObj = post(restRequest);
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj
                .getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // query tasks for associated task Id
        restRequest = queryRequestHelper.createRequest("task/query?processInstanceId=" + procInstId);
        responseObj = get(restRequest);

        JaxbTaskSummaryListResponse taskSumlistResponse = (JaxbTaskSummaryListResponse) responseObj
                .getEntity(JaxbTaskSummaryListResponse.class);
        TaskSummary taskSum = findTaskSummary(procInstId, taskSumlistResponse.getResult());
        long taskId = taskSum.getId();
        assertNotNull( "Null actual owner", taskSum.getActualOwner() );

        // get task info
        restRequest = requestHelper.createRequest("task/" + taskId);
        responseObj = get(restRequest);
        JaxbTask task = (JaxbTask) responseObj.getEntity(JaxbTask.class);
        assertEquals("Incorrect task id", taskId, task.getId().longValue());

        // start task
        restRequest = requestHelper.createRequest("task/" + taskId + "/start");
        responseObj = post(restRequest);
        JaxbGenericResponse resp = (JaxbGenericResponse) responseObj.getEntity(JaxbGenericResponse.class);
        assertNotNull("Response from task start is null.", resp);

        // get task info
        restRequest = requestHelper.createRequest("task/" + taskId);
        responseObj = get(restRequest);
        assertNotNull("Task response is null.", responseObj); 
    }

    /**
     * Test the /execute and command objects when starting processes and managing tasks
     * 
     * @param deploymentUrl
     * @param user
     * @param password
     * @throws Exception
     */
    public void commandsStartProcess(URL deploymentUrl, String user, String password) throws Exception {
        MediaType originalType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        RestRequestHelper helper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        String executeOp = "runtime/" + deploymentId + "/execute";
        ClientRequest restRequest = helper.createRequest(executeOp);
        JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(deploymentId, new StartProcessCommand(HUMAN_TASK_PROCESS_ID));
        addToRequestBody(restRequest, commandMessage);

        logger.debug(">> [startProcess] " + restRequest.getUri());
        ClientResponse<?> responseObj = post(restRequest);

        JaxbCommandsResponse cmdsResp = (JaxbCommandsResponse) responseObj.getEntity(JaxbCommandsResponse.class);
        assertFalse("Exception received!", cmdsResp.getResponses().get(0) instanceof JaxbExceptionResponse);
        long procInstId = ((ProcessInstance) cmdsResp.getResponses().get(0)).getId();

        // query tasks
        restRequest = helper.createRequest(executeOp);
        commandMessage = new JaxbCommandsRequest(deploymentId, new GetTasksByProcessInstanceIdCommand(procInstId));
        addToRequestBody(restRequest, commandMessage);

        logger.debug(">> [getTasksByProcessInstanceId] " + restRequest.getUri());
        responseObj = post(restRequest);
        JaxbCommandsResponse cmdResponse = (JaxbCommandsResponse) responseObj.getEntity(JaxbCommandsResponse.class);
        List<?> list = (List<?>) cmdResponse.getResponses().get(0).getResult();
        long taskId = (Long) list.get(0);

        // start task

        logger.debug(">> [startTask] " + restRequest.getUri());
        restRequest = helper.createRequest(executeOp);
        commandMessage = new JaxbCommandsRequest(new StartTaskCommand(taskId, taskUserId));
        addToRequestBody(restRequest, commandMessage);

        // Get response
        responseObj = post(restRequest);
        responseObj.releaseConnection();

        restRequest = helper.createRequest("task/execute");
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("myType", new MyType("serialization", 3224950));
        commandMessage = new JaxbCommandsRequest(new CompleteTaskCommand(taskId, taskUserId, results));

        addToRequestBody(restRequest, commandMessage);

        // Get response
        logger.debug(">> [completeTask] " + restRequest.getUri());
        responseObj = post(restRequest);
        JaxbCommandsResponse jaxbResp = responseObj.getEntity(JaxbCommandsResponse.class);
        assertNotNull( "Response is null", jaxbResp);
        
        // TODO: check that above has completed?
        this.mediaType = originalType;
    }

    /**
     * Test Java Remote API for starting processes and managing tasks
     *  
     * @param deploymentUrl
     * @param user
     * @param password
     * @throws Exception
     */
    public void remoteApiHumanTaskProcess(URL deploymentUrl, String user, String password) throws Exception {
        // create REST request
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull( "Null ProcessInstance!", processInstance);
        long procInstId = processInstance.getId();
        
        logger.debug("Started process instance: " + processInstance + " " + procInstId);

        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(taskUserId, "en-UK");
        long taskId = findTaskId(procInstId, tasks);

        logger.debug("Found task " + taskId);
        Task task = taskService.getTaskById(taskId);
        logger.debug("Got task " + taskId + ": " + task);
        taskService.start(taskId, taskUserId);
        taskService.complete(taskId, taskUserId, null);

        logger.debug("Now expecting failure");
        try {
            taskService.complete(taskId, taskUserId, null);
            fail("Should not be able to complete task " + taskId + " a second time.");
        } catch (Throwable t) {
            logger.info("The above exception was an expected part of the test.");
            // do nothing
        }

        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(procInstId, statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }

    public void commandsTaskCommands(URL deploymentUrl, String user, String password) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;
        
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine runtimeEngine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);

        long processInstanceId = processInstance.getId();
        JaxbCommandResponse<?> response = executeCommand(deploymentUrl, user, password, deploymentId,
                new GetTasksByProcessInstanceIdCommand(processInstanceId));

        long taskId = ((JaxbLongListResponse) response).getResult().get(0);
        assertTrue("task id is less than 0", taskId > 0);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", taskUserId);
        
        this.mediaType = origType;
    }

    private JaxbCommandResponse<?> executeCommand(URL appUrl, String user, String password, String deploymentId, Command<?> command) throws Exception {
        MediaType originalMediaType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        RestRequestHelper requestHelper = getRestRequestHelper(appUrl, user, password);

        List<Command> commands = new ArrayList<Command>();
        commands.add(command);

        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/execute");

        JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(commands);
        assertNotNull("Commands are null!", commandMessage.getCommands());
        assertTrue("Commands are empty!", commandMessage.getCommands().size() > 0);

        addToRequestBody(restRequest, commandMessage);

        logger.debug(">> [" + command.getClass().getSimpleName() + "] " + restRequest.getUri());
        ClientResponse<JaxbCommandsResponse> responseObj = restRequest.post(JaxbCommandsResponse.class);
        checkResponse(responseObj);

        JaxbCommandsResponse cmdsResp = responseObj.getEntity();

        this.mediaType = originalMediaType;
        return cmdsResp.getResponses().get(0);
    }

    public void urlsHistoryLogs(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper helper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        ClientRequest restRequest = helper.createRequest("runtime/" + deploymentId + "/process/" + SCRIPT_TASK_VAR_PROCESS_ID + "/start?map_x=initVal");
        logger.debug(">> " + restRequest.getUri());
        ClientResponse<?> responseObj = post(restRequest);
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj
                .getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // instances/
        {
            String histOp = "/history/instances";
            restRequest = helper.createRequest("runtime/" + deploymentId + histOp);
            logger.debug(">> [history] " + restRequest.getUri());
            responseObj = get(restRequest);
            JaxbHistoryLogList runtimeResult = (JaxbHistoryLogList) responseObj.getEntity(JaxbHistoryLogList.class);
            List<AuditEvent> runtimeLogList = runtimeResult.getResult();

            restRequest = helper.createRequest(histOp);
            logger.debug(">> [runtime] " + restRequest.getUri());
            responseObj = get(restRequest);
            JaxbHistoryLogList historyResult = (JaxbHistoryLogList) responseObj.getEntity(JaxbHistoryLogList.class);
            List<AuditEvent> historyLogList = runtimeResult.getResult();

            assertEquals("command name", historyResult.getCommandName(), runtimeResult.getCommandName());
            assertEquals("list size", historyLogList.size(), runtimeLogList.size());

            for (AuditEvent event : historyLogList) {
                assertTrue("ProcessInstanceLog", event instanceof ProcessInstanceLog);
                ProcessInstanceLog procLog = (ProcessInstanceLog) event;
                Object[][] out = { { procLog.getDuration(), "duration" }, { procLog.getEnd(), "end date" },
                        { procLog.getExternalId(), "externalId" }, { procLog.getId(), "id" },
                        { procLog.getIdentity(), "identity" }, { procLog.getOutcome(), "outcome" },
                        { procLog.getParentProcessInstanceId(), "parent proc id" }, { procLog.getProcessId(), "process id" },
                        { procLog.getProcessInstanceId(), "process instance id" }, { procLog.getProcessName(), "process name" },
                        { procLog.getProcessVersion(), "process version" }, { procLog.getStart(), "start date" },
                        { procLog.getStatus(), "status" } };
                for (int i = 0; i < out.length; ++i) {
                    // System.out.println(out[i][1] + ": " + out[i][0]);
                }
            }
        }
        // instance/{procInstId}

        // instance/{procInstId}/child

        // instance/{procInstId}/node

        // instance/{procInstId}/variable

        // instance/{procInstId}/node/{nodeId}

        // instance/{procInstId}/variable/{variable}
        restRequest = helper.createRequest("runtime/" + deploymentId + "/history/instance/" + procInstId + "/variable/x");
        logger.debug(">> [runtime]" + restRequest.getUri());
        responseObj = get(restRequest);
        JaxbHistoryLogList historyLogList = (JaxbHistoryLogList) responseObj.getEntity(JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> historyVarLogList = historyLogList.getHistoryLogList();
        
        restRequest = helper.createRequest("runtime/" + deploymentId + "/history/instance/" + procInstId + "/variable/x");
        logger.debug(">> [runtime]" + restRequest.getUri());
        responseObj = get(restRequest);
        JaxbHistoryLogList runtimeLogList = (JaxbHistoryLogList) responseObj.getEntity(JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> runtimeVarLogList = runtimeLogList.getHistoryLogList();
        assertTrue("Incorrect number of variable logs: " + runtimeVarLogList.size(), 4 <= runtimeVarLogList.size());

        assertEquals( "runtime/history list size", historyVarLogList.size(), runtimeVarLogList.size());
        
        for(int i = 0; i < runtimeVarLogList.size(); ++i) {
            JaxbVariableInstanceLog varLog = (JaxbVariableInstanceLog) runtimeVarLogList.get(i);
            JaxbVariableInstanceLog historyVarLog = (JaxbVariableInstanceLog) historyVarLogList.get(i);
            assertEquals(historyVarLog.getValue(), varLog.getValue());
            assertEquals("Incorrect variable id", "x", varLog.getVariableId());
            assertEquals("Incorrect process id", SCRIPT_TASK_VAR_PROCESS_ID, varLog.getProcessId());
            assertEquals("Incorrect process instance id", procInstId, varLog.getProcessInstanceId().longValue());
        }

        // process/{procDefId}

        // variable/{varId}

        // variable/{varId}/{value}

        // runtime/{depId}/history/variable/{varId}/instances

        // runtime/{depId}/history/variable/{varId}/value/{val}/instances

    }

    public void urlsDataServiceCoupling(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/"
                + SCRIPT_TASK_VAR_PROCESS_ID + "/start?map_x=initVal");

        // Get and check response
        logger.debug(">> " + restRequest.getUri());
        ClientResponse<?> responseObj = post(restRequest);
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj
                .getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        restRequest = requestHelper.createRequest("data/process/instance/" + procInstId);
        logger.debug(">> [data/process instance]" + restRequest.getUri());
        responseObj = get(restRequest);
        JaxbProcessInstanceSummary summary = (JaxbProcessInstanceSummary) responseObj.getEntity(JaxbProcessInstanceSummary.class);
        assertEquals("Incorrect initiator.", user, summary.getInitiator());
    }

    public void urlsJsonJaxbStartProcess(URL deploymentUrl, String user, String password) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        
        // XML
        String startProcessOper = "runtime/" + deploymentId + "/process/org.jbpm.humantask/start";
        ClientRequest restRequest = requestHelper.createRequest(startProcessOper);
        logger.debug(">> " + restRequest.getUri());
        ClientResponse<?> responseObj = post(restRequest);
        String result = (String) responseObj.getEntity(String.class);
        assertTrue("Doesn't start like a JAXB string!", result.startsWith("<"));

        // JSON
        this.mediaType = MediaType.APPLICATION_JSON_TYPE;
        requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        restRequest = requestHelper.createRequest(startProcessOper);
        logger.debug(">> " + restRequest.getUri());
        responseObj = post(restRequest);
        result = (String) responseObj.getEntity(String.class);
        if( ! result.startsWith("{") ) { 
            logger.error( "Should be JSON:\n" + result );
            fail("Doesn't start like a JSON string!");
        }
        
        this.mediaType = origType;
    }

    public void urlsHumanTaskWithFormVariableChange(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + HUMAN_TASK_VAR_PROCESS_ID
                + "/start?map_userName=John");
        ClientResponse<?> responseObj = post(restRequest);
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj
                .getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // query tasks for associated task Id
        restRequest = requestHelper.createRequest("task/query?processInstanceId=" + procInstId);
        responseObj = get(restRequest);

        JaxbTaskSummaryListResponse taskSumlistResponse = (JaxbTaskSummaryListResponse) responseObj
                .getEntity(JaxbTaskSummaryListResponse.class);
        TaskSummary taskSum = findTaskSummary(procInstId, taskSumlistResponse.getResult());
        long taskId = taskSum.getId();

        // start task
        restRequest = requestHelper.createRequest("task/" + taskId + "/start");
        responseObj = post(restRequest);
        JaxbGenericResponse resp = (JaxbGenericResponse) responseObj.getEntity(JaxbGenericResponse.class);
        assertNotNull("Response from task start operation is null.", resp);

        // complete task
        String georgeVal = "George";
        restRequest = requestHelper.createRequest("task/" + taskId + "/complete?map_outUserName=" + georgeVal);
        responseObj = post(restRequest);
        resp = (JaxbGenericResponse) responseObj.getEntity(JaxbGenericResponse.class);

        restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/history/instance/" + procInstId + "/variable/userName");
        responseObj = get(restRequest);
        responseObj.releaseConnection();
        
        restRequest = requestHelper.createRequest("history/instance/" + procInstId + "/variable/userName");
        responseObj = get(restRequest);

        JaxbHistoryLogList histResp = (JaxbHistoryLogList) responseObj.getEntity(JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> histList = histResp.getHistoryLogList();
        boolean georgeFound = false;
        for (AbstractJaxbHistoryObject<VariableInstanceLog> absVarLog : histList) {
            VariableInstanceLog varLog = ((JaxbVariableInstanceLog) absVarLog).getResult();
            if ("userName".equals(varLog.getVariableId()) && georgeVal.equals(varLog.getValue())) {
                georgeFound = true;
            }
        }
        assertTrue("'userName' var with value '" + georgeVal + "' not found!", georgeFound);
    }

    public void urlsHttpURLConnectionAcceptHeaderIsFixed(URL deploymentUrl, String user, String password) throws Exception {
        URL url = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/runtime/" + deploymentId + "/process/"
                + SCRIPT_TASK_PROCESS_ID + "/start");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String authString = user + ":" + password;
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        connection.setRequestMethod("POST");

        logger.debug(">> [POST] " + url.toExternalForm());
        connection.connect();
        if (200 != connection.getResponseCode()) {
            logger.warn(connection.getContent().toString());
        }
        assertEquals(200, connection.getResponseCode());
    }

    public void remoteApiSerialization(URL deploymentUrl, String user, String password) throws Exception {
        // setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
       
        // start process
        ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        Collection<ProcessInstance> processInstances = ksession.getProcessInstances();
        assertNotNull("Null process instance list!", processInstances );
        assertTrue("No process instances started.", processInstances.size() > 0);
    }

    public void remoteApiExtraJaxbClasses(URL deploymentUrl, String user, String password) throws Exception {
       runRemoteApiExtraJaxbClassesTest(deploymentId, deploymentUrl, user, password); 
    }

    private void runRemoteApiExtraJaxbClassesTest(String deploymentId, URL deploymentUrl, String user, String password) throws Exception { 
        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        // test
        testExtraJaxbClassSerialization(engine);
    }
    
    public void remoteApiRuleTaskProcess(URL deploymentUrl, String user, String password) {
        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine runtimeEngine = restSessionFactory.newRuntimeEngine();

        // runTest
        runRuleTaskProcess(runtimeEngine.getKieSession(), runtimeEngine.getAuditLogService());
    }

    public void remoteApiGetTaskInstance(URL deploymentUrl, String user, String password) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;
        
        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();

        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = null;
        try {
            processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        } catch (Exception e) {
            fail("Unable to start process: " + e.getMessage());
        }
        
        assertNotNull("Null processInstance!", processInstance);
        long procInstId = processInstance.getId();

        TaskService taskService = engine.getTaskService();
        List<Long> tasks = taskService.getTasksByProcessInstanceId(procInstId);
        assertEquals("Incorrect number of tasks for started process: ", 1, tasks.size());
        long taskId = tasks.get(0);

        // Get it via the command
        JaxbCommandResponse<?> response = executeCommand(deploymentUrl, user, password, deploymentId, new GetTaskCommand(taskId));
        Task task = (Task) response.getResult();
        checkReturnedTask(task, taskId);

        // Get it via the URL
        this.mediaType = origType;
        ClientRequest restRequest = getRestRequestHelper(deploymentUrl, user, password).createRequest("task/" + taskId);
        ClientResponse<?> responseObj = get(restRequest);
        JaxbTask jaxbTask = responseObj.getEntity(JaxbTask.class);
        checkReturnedTask((Task) jaxbTask, taskId);

        // Get it via the remote API
        task = engine.getTaskService().getTaskById(taskId);
        checkReturnedTask(task, taskId);
    }

    private void checkReturnedTask(Task task, long taskId) {
        assertNotNull("Could not retrietve task " + taskId, task);
        assertEquals("Incorrect task retrieved", taskId, task.getId().longValue());
        TaskData taskData = task.getTaskData();
        assertNotNull(taskData);
    }

    public void urlsStartScriptProcess(URL deploymentUrl, String user, String password) throws Exception {
        // Remote API setup
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);

        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + SCRIPT_TASK_PROCESS_ID
                + "/start");

        // Start process
        ClientResponse<?> responseObj = post(restRequest);
        ProcessInstance procInst = responseObj.getEntity(JaxbProcessInstanceResponse.class).getResult();

        int procStatus = procInst.getState();
        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_COMPLETED, procStatus);
    }

    
    public void urlsGetTaskAndTaskContent(URL deploymentUrl, String user, String password) throws Exception {
        // Remote API setup
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);

        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + TASK_CONTENT_PROCESS_ID + "/start");

        // Start process
        ClientResponse<?> responseObj = post(restRequest);
        ProcessInstance procInst = responseObj.getEntity(JaxbProcessInstanceResponse.class).getResult();

        int procStatus = procInst.getState();
        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_ACTIVE, procStatus);

        // Get taskId
        restRequest = requestHelper.createRequest("task/query?processInstanceId=" + procInst.getId());
        responseObj = get(restRequest);
        JaxbTaskSummaryListResponse taskSumList = responseObj.getEntity(JaxbTaskSummaryListResponse.class);
        assertFalse( "No tasks found!", taskSumList.getResult().isEmpty() );
        TaskSummary taskSum = taskSumList.getResult().get(0);
        long taskId = taskSum.getId();
        
        // get task content
        restRequest = requestHelper.createRequest("task/" + taskId + "/content");
        responseObj = get(restRequest);
        JaxbContent content = responseObj.getEntity(JaxbContent.class);
        assertNotNull( "No content retrieved!", content.getContentMap() );
        assertEquals( "reviewer", content.getContentMap().get("GroupId"));
        
        // get (JSON) task
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_JSON_TYPE;
        restRequest = getRestRequestHelper(deploymentUrl, user, password).createRequest("task/" + taskId);
        responseObj = get(restRequest);
        JaxbTask jsonTask = responseObj.getEntity(JaxbTask.class);
        assertNotNull( "No task retrieved!", jsonTask);
        assertEquals( "task id", taskId, jsonTask.getId().intValue());
        this.mediaType = origType;
    }
    
    public void urlsVariableHistory(URL deploymentUrl, String user, String password) throws Exception {
        // Remote API setup
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
       
        String varId = "myobject";
        ClientRequest restRequest 
            = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=10");
        ClientResponse<?> responseObj = post(restRequest);
        JaxbProcessInstanceResponse procInstResp = responseObj.getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();
       
        // var
        restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/history/variable/" + varId);
        responseObj = get(restRequest);
        JaxbHistoryLogList jhll = responseObj.getEntity(JaxbHistoryLogList.class);
        List<VariableInstanceLog> viLogs = new ArrayList<VariableInstanceLog>();
        if (jhll != null) {
            List<AuditEvent> history = jhll.getResult();
            for (AuditEvent ae : history) {
                viLogs.add((VariableInstanceLog) ae);
            }
        }

        assertNotNull("Empty VariableInstanceLog list.", viLogs);
        assertEquals("VariableInstanceLog list size",  4, viLogs.size());
        VariableInstanceLog vil = viLogs.get(0);
        assertNotNull("Empty VariableInstanceLog instance.", vil);
        assertEquals("Process instance id", vil.getProcessInstanceId(), procInstId);
        assertEquals("Variable id", vil.getVariableId(), "myobject");
        assertEquals("Variable value", vil.getValue(), "10"); 
       
        // proc log
        restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/history/variable/" + varId + "/instances");
        responseObj = get(restRequest);
        jhll = responseObj.getEntity(JaxbHistoryLogList.class);
        
        assertNotNull("Empty ProcesInstanceLog list", jhll);
        List<ProcessInstanceLog> piLogs = new ArrayList<ProcessInstanceLog>();
        if (jhll != null) {
            List<AuditEvent> history = jhll.getResult();
            for (AuditEvent ae : history) {
                piLogs.add((ProcessInstanceLog) ae);
            }
        }
        assertNotNull("Empty ProcesInstanceLog list", piLogs);
        assertEquals("ProcessInstanceLog list size", piLogs.size(), 1);
        ProcessInstanceLog pi = piLogs.get(0);
        assertNotNull(pi);
        assertEquals(procInstId, pi.getId());
    }

    public void urlsGetDeployments(URL deploymentUrl, String user, String password) throws Exception {
        // test with normal RestRequestHelper
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
      
        ClientRequest restRequest = requestHelper.createRequest("deployment/");
        ClientResponse<?> responseObj = get(restRequest);
        JaxbDeploymentUnitList depList = responseObj.getEntity(JaxbDeploymentUnitList.class);
        assertNotNull( "Null answer!", depList);
        assertNotNull( "Null deployment list!", depList.getDeploymentUnitList() );
        assertTrue( "Empty deployment list!", depList.getDeploymentUnitList().size() > 0);
       
        String deploymentId = depList.getDeploymentUnitList().get(0).getIdentifier();
        restRequest = requestHelper.createRequest("deployment/" + deploymentId);
        responseObj = get(restRequest);
        JaxbDeploymentUnit dep = responseObj.getEntity(JaxbDeploymentUnit.class);
        assertNotNull( "Null answer!", dep);
        assertNotNull( "Null deployment list!", dep);
        assertEquals( "Empty status!", JaxbDeploymentStatus.DEPLOYED, dep.getStatus());
        
        // test with HttpURLConnection
        if( testWithHttpUrlConnection ) { 
            URL url = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/deployment/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            String authString = user + ":" + password;
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);
            connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            connection.setRequestMethod("GET");

            logger.debug(">> [GET] " + url.toExternalForm());
            connection.connect();
            int respCode = connection.getResponseCode();
            if (200 != respCode) { 
                logger.warn(connection.getContent().toString());
            }
            assertEquals(200, respCode);
            
            JaxbSerializationProvider jaxbSerializer = new JaxbSerializationProvider();
            String xmlStrObj = getConnectionContent(connection.getContent());
            logger.info( "Output: |" + xmlStrObj + "|");
            depList = (JaxbDeploymentUnitList) jaxbSerializer.deserialize(xmlStrObj);
            
            assertNotNull( "Null answer!", depList);
            assertNotNull( "Null deployment list!", depList.getDeploymentUnitList() );
            assertTrue( "Empty deployment list!", depList.getDeploymentUnitList().size() > 0);
        }
    }
   
    private String getConnectionContent(Object content) throws Exception { 
        InputStreamReader in = new InputStreamReader((InputStream) content);
        BufferedReader buff = new BufferedReader(in);
        StringBuffer text = new StringBuffer();
        String line = buff.readLine();
        while( line != null ) { 
            text.append(line);
            line = buff.readLine();
        }
        return text.toString();
    }
    
    public void urlsGetRealProcessVariable(URL deploymentUrl, String user, String password) throws Exception { 
        // Setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        logger.info( "deployment url: " + deploymentUrl.toExternalForm());
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        
        // Start process
        MyType param = new MyType("variable", 29);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("myobject", param);
        long procInstId = engine.getKieSession().startProcess(OBJECT_VARIABLE_PROCESS_ID, parameters).getId();
        
        /**
         * Check that MyType was correctly deserialized on server side
         */
        String varName = "myobject";
        List<VariableInstanceLog> varLogList = engine.getAuditLogService().findVariableInstancesByName(varName, false);
        VariableInstanceLog thisProcInstVarLog = null;
        for( VariableInstanceLog varLog : varLogList ) {
            if( varLog.getProcessInstanceId() == procInstId ) { 
                thisProcInstVarLog = varLog;
                break;
            }
        }
        assertNotNull( "No VariableInstanceLog found!", thisProcInstVarLog);
        assertEquals( varName, thisProcInstVarLog.getVariableId() );
        Object procInstVar = thisProcInstVarLog.getValue();
        assertNotNull("Null process instance variable!", procInstVar);
        assertEquals( "De/serialization of Kjar type did not work.", param.toString(), procInstVar );
        
        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/instance/" + procInstId );
        ClientResponse<?> response = get(restRequest);
        JaxbProcessInstanceResponse jaxbProcInstResp = response.getEntity(JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();
        assertNotNull( procInst );
        assertEquals( "Unequal process instance id.", procInstId, procInst.getId());
       
        restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/instance/" + procInstId + "/variable/" + varName );
        response = get(restRequest);
      
        String xmlStr = response.getEntity(String.class);
        MyType retrievedVar = (MyType) jaxbSerializationProvider.deserialize(xmlStr);
        assertNotNull( "Expected filled variable.", retrievedVar);
        assertEquals("Data integer doesn't match: ", retrievedVar.getData(), param.getData());
        assertEquals("Text string doesn't match: ", retrievedVar.getText(), param.getText());
    }
    

    public void urlsCloneAndDeployJbpmPlaygroundEvaluationProject() {
        // https://github.com/droolsjbpm/jbpm-playground.git

    }
    
    public void remoteApiHumanTaskGroupIdTest(URL deploymentUrl) { 
       RemoteRuntimeEngineFactory krisRemoteEngineFactory 
           = new RemoteRestRuntimeFactory(deploymentId, deploymentUrl, KRIS_USER, KRIS_PASSWORD, useFormBasedAuth );
       RemoteRuntimeEngineFactory maryRemoteEngineFactory 
           = new RemoteRestRuntimeFactory(deploymentId, deploymentUrl, MARY_USER, MARY_PASSWORD, useFormBasedAuth);
       RemoteRuntimeEngineFactory johnRemoteEngineFactory 
           = new RemoteRestRuntimeFactory(deploymentId, deploymentUrl, JOHN_USER, JOHN_PASSWORD, useFormBasedAuth);
       runHumanTaskGroupIdTest(krisRemoteEngineFactory.newRuntimeEngine(), 
               johnRemoteEngineFactory.newRuntimeEngine(), 
               maryRemoteEngineFactory.newRuntimeEngine());
    }
    
    public void remoteApiGroupAssignmentTest(URL deploymentUrl) throws Exception { 
        RestRequestHelper maryReqHelper = RestRequestHelper.newInstance(deploymentUrl, MARY_USER, MARY_PASSWORD);
        RestRequestHelper johnReqHelper = RestRequestHelper.newInstance(deploymentUrl, JOHN_USER, JOHN_PASSWORD);
       
        ClientRequest request = maryReqHelper.createRequest("runtime/" + deploymentId + "/process/" + GROUP_ASSSIGNMENT_PROCESS_ID + "/start");
        ClientResponse<?> response = post(request);
        JaxbProcessInstanceResponse procInstResp = response.getEntity(JaxbProcessInstanceResponse.class);
        assertEquals(ProcessInstance.STATE_ACTIVE, procInstResp.getState());
        long procInstId = procInstResp.getId();

        // assert the task
        TaskSummary taskSummary = getTaskSummary(maryReqHelper, procInstId, Status.Ready);
        long taskId = taskSummary.getId();
        assertNull(taskSummary.getActualOwner());
        assertNull(taskSummary.getPotentialOwners());
        assertEquals("Task 1", taskSummary.getName());

        // complete 'Task 1' as mary
        request = maryReqHelper.createRequest("task/"+ taskId + "/claim");
        response = post(request);
        response.releaseConnection();
        
        request = maryReqHelper.createRequest("task/"+ taskId + "/start");
        response = post(request);
        response.releaseConnection();
        request = maryReqHelper.createRequest("task/"+ taskId + "/complete");
        response = post(request);
        response.releaseConnection();

        // now make sure that the next task has been assigned to the
        // correct person. it should be mary.
        taskSummary = getTaskSummary(maryReqHelper, procInstId, Status.Reserved);
        assertEquals("Task 2", taskSummary.getName());
        assertEquals(MARY_USER, taskSummary.getActualOwner().getId());
        taskId = taskSummary.getId();

        // complete 'Task 2' as john
        request = maryReqHelper.createRequest("task/"+ taskId + "/release");
        response = post(request);
        response.releaseConnection();
        request = johnReqHelper.createRequest("task/"+ taskId + "/start");
        response = post(request);
        response.releaseConnection();
        request = johnReqHelper.createRequest("task/"+ taskId + "/complete");
        response = post(request);
        response.releaseConnection();

        // now make sure that the next task has been assigned to the
        // correct person. it should be john.
        taskSummary = getTaskSummary(johnReqHelper, procInstId, Status.Reserved);
        assertEquals("Task 3", taskSummary.getName());
        assertEquals(JOHN_USER, taskSummary.getActualOwner().getId());
        taskId = taskSummary.getId();
        
        // complete 'Task 3' as john
        request = johnReqHelper.createRequest("task/"+ taskId + "/start");
        response = post(request);
        response.releaseConnection();
        request = johnReqHelper.createRequest("task/"+ taskId + "/complete");
        response = post(request);
        response.releaseConnection();

        // assert process finished
        request = maryReqHelper.createRequest("history/instance/" + procInstId);
        response = get(request);
        JaxbHistoryLogList logList = response.getEntity(JaxbHistoryLogList.class);
        List<AuditEvent> eventLogList = logList.getResult();
        assertEquals("Expected one history (procInst) object", 1, eventLogList.size());
        ProcessInstanceLog procInstLog = (ProcessInstanceLog) eventLogList.get(0);
        assertEquals( "Process instance has not completed!", ProcessInstance.STATE_COMPLETED, procInstLog.getStatus().intValue());
    }
   
    private TaskSummary getTaskSummary(RestRequestHelper requestHelper, long processInstanceId, Status status) throws Exception {
        ClientRequest request = requestHelper.createRequest("task/query?processInstanceId=" + processInstanceId+ "&status=" + status.toString() );
        ClientResponse<?> response = get(request);
        JaxbTaskSummaryListResponse taskSumListResp = response.getEntity(JaxbTaskSummaryListResponse.class);
        List<TaskSummary> taskSumList = taskSumListResp.getResult();
        assertEquals(1, taskSumList.size());
        return taskSumList.get(0);
    }
    
    public void urlsWorkItemTest(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper helper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        ClientRequest restRequest = helper.createRequest("runtime/" + deploymentId + "/workitem/200" );
        logger.debug(">> " + restRequest.getUri());
        ClientResponse<?> responseObj = get(restRequest);
    }
  
    public void remoteApiHumanTaskGroupVarAssignTest(URL deploymentUrl) { 
        RemoteRuntimeEngineFactory maryRemoteEngineFactory 
            = RemoteRestRuntimeEngineFactory.newBuilder()
            .addDeploymentId(deploymentId)
            .addUserName(MARY_USER)
            .addPassword(MARY_PASSWORD)
            .addUrl(deploymentUrl)
            .build();

        RuntimeEngine runtimeEngine = maryRemoteEngineFactory.newRuntimeEngine();

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("taskOwnerGroup", "HR");
        params.put("taskName", "Mary's Task");
        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(GROUP_ASSSIGN_VAR_PROCESS_ID, params);
        assertNotNull( "No ProcessInstance!", pi);
        long procInstId = pi.getId();
        
        List<Long> taskIds = runtimeEngine.getTaskService().getTasksByProcessInstanceId(procInstId);
        assertEquals( 1, taskIds.size());

        List<String> processIds = runtimeEngine.getKieSession().execute(new GetProcessIdsCommand());
        for( String procId : processIds ) { 
            System.out.println( "Process: " + procId);
        }
     }
    
    public void remoteApiHumanTaskOwnTypeTest(URL deploymentUrl) { 
        RemoteRuntimeEngineFactory maryRemoteEngineFactory 
            = RemoteRestRuntimeEngineFactory.newBuilder()
            .addDeploymentId(deploymentId)
            .addUserName(JOHN_USER)
            .addPassword(JOHN_PASSWORD)
            .addUrl(deploymentUrl)
            .addExtraJaxbClasses(MyType.class)
            .build();

        RemoteRuntimeEngine runtimeEngine = maryRemoteEngineFactory.newRuntimeEngine();
        runRemoteApiHumanTaskOwnTypeTest(runtimeEngine, runtimeEngine.getAuditLogService());
     }
    
    public void runRemoteApiHumanTaskOwnTypeTest(RuntimeEngine runtimeEngine, AuditLogService auditLogService) { 
        MyType myType = new MyType("wacky", 123);

        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(HUMAN_TASK_OWN_TYPE_ID);
        assertNotNull(pi);
        assertEquals( ProcessInstance.STATE_ACTIVE, pi.getState());

        TaskService taskService = runtimeEngine.getTaskService();
        List<Long> taskIds = taskService.getTasksByProcessInstanceId(pi.getId());
        assertFalse(taskIds.isEmpty());
        long taskId = taskIds.get(0);

        taskService.start(taskId, JOHN_USER);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("outMyObject", myType);
        taskService.complete(taskId, JOHN_USER, data);

        Task task = taskService.getTaskById(taskId);
        assertEquals( Status.Completed, task.getTaskData().getStatus());

        List<VariableInstanceLog> vill = auditLogService.findVariableInstances(pi.getId(), "myObject");
        assertNotNull(vill);
        assertEquals(myType.toString(), vill.get(0).getValue());
    }
    
    public void urlsCreateMemoryLeakOnTomcat(URL deploymentUrl, String user, String password, long timeout) throws Exception { 
        long origCallDurationLimit = this.restCallDurationLimit;
        this.restCallDurationLimit = timeout; 
        
        // Remote API setup
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        try { 
            for( int i = 0; i < 20; ++i ) { 
                logger.info( i + " process started.");
                startProcessWithUserDefinedClass(requestHelper); 
            }
        } finally { 
            this.restCallDurationLimit = origCallDurationLimit;
        }
     }
    
    private void startProcessWithUserDefinedClass(RestRequestHelper requestHelper) throws Exception {
        String varId = "myobject";
        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=10");
        ClientResponse<?> responseObj = checkResponsePostTime(restRequest, 200);
        JaxbProcessInstanceResponse procInstResp = responseObj.getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();;
        assertTrue( "Process instance should be larger than 0: " + procInstId, procInstId > 0 );
    }
    
    public void remoteApiDeploymentRedeployClassPathTest(URL deploymentUrl, String user, String password) throws Exception  {
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        
        KModuleDeploymentUnit kDepUnit = new KModuleDeploymentUnit(GROUP_ID, CLASSPATH_ARTIFACT_ID, VERSION);
        String classpathDeploymentId = kDepUnit.getIdentifier();
        
        // Check that project is not deployed
        ClientRequest restRequest = requestHelper.createRequest("deployment/" + classpathDeploymentId + "/");
        setAcceptHeader(restRequest);
  
        // Run test the first time
        if ( ! isDeployed(kDepUnit, restRequest.get()) ) { 
            // Deploy
            deploy(kDepUnit, user, password, deploymentUrl);
        }
            
        // Run process
        RemoteRuntimeEngine runtimeEngine = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(classpathDeploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password)
                .build().newRuntimeEngine();
                
        runClassPathProcessTest(runtimeEngine);
           
        // undeploy..
        undeploy(kDepUnit, deploymentUrl, requestHelper);
            
        // .. and (re)deploy
        deploy(kDepUnit, user, password, deploymentUrl);

        logger.info( "Rerunning test.. is there a CNFE?");
        // Rerun process
        runClassPathProcessTest(runtimeEngine);
    }
 
    private void runClassPathProcessTest(RemoteRuntimeEngine runtimeEngine) { 
        KieSession ksession = runtimeEngine.getKieSession();

        Map<String, Object> params = new HashMap<String, Object>();
        String varId = "myobject";
        String text = UUID.randomUUID().toString();
        params.put(varId, new MyType(text, 10));
        ProcessInstance procInst = ksession.startProcess(TestConstants.CLASSPATH_OBJECT_PROCESS_ID, params);
        long processInstanceId = procInst.getId();

        AuditLogService auditLogService = runtimeEngine.getAuditLogService();
        List<VariableInstanceLog> varLogList = auditLogService.findVariableInstances(processInstanceId);
        
        assertNotNull("Null variable instance found.", varLogList);
        for (VariableInstanceLog varLog : varLogList ) { 
            logger.debug(varLog.getVariableId() + " (" + varLog.getValue() + ") " );
        }

        List<VariableInstanceLog> varLogs = runtimeEngine.getAuditLogService().findVariableInstancesByName(varId, false);
        assertTrue(varLogs.size() > 0);
        assertEquals(varId, varLogs.get(0).getVariableId());

        procInst = ksession.getProcessInstance(processInstanceId);
        assertNull(procInst);
    }
}
