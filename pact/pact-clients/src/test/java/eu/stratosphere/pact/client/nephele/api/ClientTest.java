/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package eu.stratosphere.pact.client.nephele.api;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.matchers.Any;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import eu.stratosphere.nephele.client.JobClient;
import eu.stratosphere.nephele.client.JobSubmissionResult;
import eu.stratosphere.nephele.client.AbstractJobResult.ReturnCode;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.jobgraph.JobGraph;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.whenNew;



/**
 * Simple and maybe stupid test to check the {@link Client} class.
 * However, the use of mocks can be copied copied easily from this example.
 * @author Mathias Peters <mathias.peters@informatik.hu-berlin.de>
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Client.class)
public class ClientTest {

	@Mock
	Configuration configMock;
	@Mock
	PactProgram program;
	@Mock
	JobGraph jobGraphMock;
	@Mock 
	File mockJarFile;
	@Mock
	JobClient jobClientMock;
	@Mock
	JobSubmissionResult jobSubmissionResultMock;
	
	@Before
	public void setUp() throws Exception
	{
		initMocks(this);
		when(program.getCompiledPlan()).thenReturn(jobGraphMock);
		when(program.getJarFile()).thenReturn(mockJarFile);
		when(mockJarFile.getAbsolutePath()).thenReturn("mockFilePath");
		whenNew(JobClient.class).withArguments(any(JobGraph.class), any(Configuration.class)).thenReturn(this.jobClientMock);
		when(this.jobClientMock.submitJob()).thenReturn(jobSubmissionResultMock);
	}
	
	@Test
	public void shouldSubmitToJobClient() throws ProgramInvocationException, ErrorInPlanAssemblerException, IOException
	{
		when(jobSubmissionResultMock.getReturnCode()).thenReturn(ReturnCode.SUCCESS);
		Client out = new Client(configMock);
		out.run(program);
		verify(this.jobClientMock, times(1)).submitJob();
	}
	
	/**
	 * @throws Exception
	 */
	@Test(expected=ProgramInvocationException.class)
	public void shouldThrowException() throws Exception
	{
		when(jobSubmissionResultMock.getReturnCode()).thenReturn(ReturnCode.ERROR);
		Client out = new Client(configMock);
		out.run(program);
		verify(this.jobClientMock).submitJob();
	}
}