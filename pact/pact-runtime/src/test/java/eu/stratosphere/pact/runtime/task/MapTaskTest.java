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

package eu.stratosphere.pact.runtime.task;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import eu.stratosphere.pact.common.generic.GenericMapper;
import eu.stratosphere.pact.common.stubs.Collector;
import eu.stratosphere.pact.common.stubs.MapStub;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.runtime.test.util.DriverTestBase;
import eu.stratosphere.pact.runtime.test.util.InfiniteInputIterator;
import eu.stratosphere.pact.runtime.test.util.NirvanaOutputList;
import eu.stratosphere.pact.runtime.test.util.UniformPactRecordGenerator;
import eu.stratosphere.pact.runtime.test.util.TaskCancelThread;

public class MapTaskTest extends DriverTestBase<GenericMapper<PactRecord, PactRecord>>
{
	private static final Log LOG = LogFactory.getLog(MapTaskTest.class);
	
	private List<PactRecord> outList;
	
	
	public MapTaskTest() {
		super(0);
		this.outList = new ArrayList<PactRecord>();
	}
	
	@Test
	public void testMapTask()
	{
		final int keyCnt = 100;
		final int valCnt = 20;
		
		addInput(new UniformPactRecordGenerator(keyCnt, valCnt, false));
		addOutput(this.outList);
		
		final MapDriver<PactRecord, PactRecord> testDriver = new MapDriver<PactRecord, PactRecord>();
		
		try {
			testDriver(testDriver, MockMapStub.class);
		} catch (Exception e) {
			LOG.debug(e);
			Assert.fail("Invoke method caused exception.");
		}
		
		Assert.assertTrue(this.outList.size() == keyCnt*valCnt);
		
	}
	
	@Test
	public void testFailingMapTask()
	{
		final int keyCnt = 100;
		final int valCnt = 20;
		
		addInput(new UniformPactRecordGenerator(keyCnt, valCnt, false));
		addOutput(this.outList);
		
		final MapDriver<PactRecord, PactRecord> testTask = new MapDriver<PactRecord, PactRecord>();
		boolean stubFailed = false;
		
		try {
			testDriver(testTask, MockFailingMapStub.class);
		} catch (Exception e) {
			stubFailed = true;
		}
		
		Assert.assertTrue("Stub exception was not forwarded.", stubFailed);
		
	}
	
	@Test
	public void testCancelMapTask()
	{
		addInput(new InfiniteInputIterator());
		addOutput(new NirvanaOutputList());
		
		final MapDriver<PactRecord, PactRecord> testTask = new MapDriver<PactRecord, PactRecord>();
		
		
		
		Thread taskRunner = new Thread() {
			@Override
			public void run() {
				try {
					testDriver(testTask, MockMapStub.class);
				} catch (Exception ie) {
					ie.printStackTrace();
					Assert.fail("Task threw exception although it was properly canceled");
				}
			}
		};
		taskRunner.start();
		
		TaskCancelThread tct = new TaskCancelThread(1, taskRunner, this);
		tct.start();
		
		try {
			tct.join();
			taskRunner.join();		
		} catch(InterruptedException ie) {
			Assert.fail("Joining threads failed");
		}
				
	}
	
	public static class MockMapStub extends MapStub
	{
		@Override
		public void map(PactRecord record, Collector<PactRecord> out) throws Exception {
			out.collect(record);
		}
		
	}
	
	public static class MockFailingMapStub extends MapStub {

		int cnt = 0;
		
		@Override
		public void map(PactRecord record, Collector<PactRecord> out) throws Exception {
			if(++this.cnt>=10) {
				throw new RuntimeException("Expected Test Exception");
			}
			out.collect(record);
		}
		
	}
	
}
