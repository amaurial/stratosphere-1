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

package eu.stratosphere.pact.runtime.hash;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import eu.stratosphere.nephele.services.iomanager.IOManager;
import eu.stratosphere.nephele.services.memorymanager.MemoryAllocationException;
import eu.stratosphere.nephele.services.memorymanager.MemoryManager;
import eu.stratosphere.nephele.services.memorymanager.MemorySegment;
import eu.stratosphere.nephele.services.memorymanager.spi.DefaultMemoryManager;
import eu.stratosphere.nephele.template.AbstractInvokable;
import eu.stratosphere.pact.common.type.KeyValuePair;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.runtime.test.util.DummyInvokable;
import eu.stratosphere.pact.runtime.test.util.RegularlyGeneratedInputGenerator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 *
 * @author Stephan Ewen (stephan.ewen@tu-berlin.de)
 */
public class HashJoinTest
{
	
	private static final AbstractInvokable MEM_OWNER = new DummyInvokable();
	
	@Test
	public void testIOBufferCountComputation()
	{
		assertEquals(1, HashJoin.getNumWriteBehindBuffers(32));
		assertEquals(1, HashJoin.getNumWriteBehindBuffers(33));
		assertEquals(1, HashJoin.getNumWriteBehindBuffers(40));
		assertEquals(1, HashJoin.getNumWriteBehindBuffers(64));
		assertEquals(1, HashJoin.getNumWriteBehindBuffers(127));
		assertEquals(2, HashJoin.getNumWriteBehindBuffers(128));
		assertEquals(2, HashJoin.getNumWriteBehindBuffers(129));
		assertEquals(2, HashJoin.getNumWriteBehindBuffers(511));
		assertEquals(3, HashJoin.getNumWriteBehindBuffers(512));
		assertEquals(3, HashJoin.getNumWriteBehindBuffers(513));
		assertEquals(3, HashJoin.getNumWriteBehindBuffers(2047));
		assertEquals(4, HashJoin.getNumWriteBehindBuffers(2048));
		assertEquals(4, HashJoin.getNumWriteBehindBuffers(2049));
		assertEquals(4, HashJoin.getNumWriteBehindBuffers(8191));
		assertEquals(5, HashJoin.getNumWriteBehindBuffers(8192));
		assertEquals(5, HashJoin.getNumWriteBehindBuffers(8193));
		assertEquals(5, HashJoin.getNumWriteBehindBuffers(32767));
		assertEquals(6, HashJoin.getNumWriteBehindBuffers(32768));
		assertEquals(6, HashJoin.getNumWriteBehindBuffers(Integer.MAX_VALUE));
	}
	
	@Test
	public void testLog2Computation()
	{
		assertEquals(0, HashJoin.log2floor(1));
		assertEquals(1, HashJoin.log2floor(2));
		assertEquals(1, HashJoin.log2floor(3));
		assertEquals(2, HashJoin.log2floor(4));
		assertEquals(2, HashJoin.log2floor(5));
		assertEquals(2, HashJoin.log2floor(7));
		assertEquals(3, HashJoin.log2floor(8));
		assertEquals(3, HashJoin.log2floor(9));
		assertEquals(4, HashJoin.log2floor(16));
		assertEquals(4, HashJoin.log2floor(17));
		assertEquals(13, HashJoin.log2floor((0x1 << 13) + 1));
		assertEquals(30, HashJoin.log2floor(Integer.MAX_VALUE));
		assertEquals(31, HashJoin.log2floor(-1));
		
		try {
			HashJoin.log2floor(0);
			fail();
		}
		catch (ArithmeticException aex) {}
	}
	
	
	@Test
	public void testHashTableBuilding()
	{
		// create a build input that gives 3 million pairs with 3 values sharing the same key
		Iterator<KeyValuePair<PactInteger, PactInteger>> buildInput = new RegularlyGeneratedInputGenerator(1000000, 3, false);

		// create a probe input that gives 10 million pairs with 10 values sharing a key
		Iterator<KeyValuePair<PactInteger, PactInteger>> probeInput = new RegularlyGeneratedInputGenerator(1000000, 10, false);
		
		// allocate the memory for the HashTable
		MemoryManager memMan; 
		List<MemorySegment> memSegments;
		
		try {
			memMan = new DefaultMemoryManager(32 * 1024 * 1024);
			memSegments = memMan.allocate(MEM_OWNER, 28 * 1024 * 1024, 896, 4096 * 8);
		}
		catch (MemoryAllocationException maex) {
			fail("Memory for the Join could not be provided.");
			return;
		}
		
		// create the I/O access for spilling
		IOManager ioManager = new IOManager();
		
		// ----------------------------------------------------------------------------------------
		
		HashJoin<PactInteger, PactInteger> join = new HashJoin<PactInteger, PactInteger>(buildInput, probeInput, memSegments, ioManager);
		join.buildInitialTable(buildInput);
		
		
		// ----------------------------------------------------------------------------------------
		
		// shut down I/O manager and Memory Manager and verify the correct shutdown
		ioManager.shutdown();
		if (!ioManager.isProperlyShutDown()) {
			fail("I/O manager was not property shut down.");
		}
		if (!memMan.verifyEmpty()) {
			fail("Not all memory was properly released to the memory manager --> Memory Leak.");
		}
	}
}