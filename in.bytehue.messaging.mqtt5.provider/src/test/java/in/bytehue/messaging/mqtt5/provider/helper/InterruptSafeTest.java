/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.provider.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class InterruptSafeTest {

	@Test
	public void testExecuteCallableReturnsResult() {
		final Callable<String> task = () -> "hello";
		final String result = InterruptSafe.execute(task);
		assertEquals("hello", result);
	}

	@Test
	public void testExecuteCallableReturnsNullOnException() {
		final Callable<String> task = () -> {
			throw new RuntimeException("fail");
		};
		final String result = InterruptSafe.execute(task);
		assertNull("Should return null when callable throws", result);
	}

	@Test
	public void testExecuteRunnable() {
		final AtomicBoolean executed = new AtomicBoolean(false);
		final Runnable task = () -> executed.set(true);

		InterruptSafe.execute(task);

		assertTrue("Runnable should have been executed", executed.get());
	}

	@Test
	public void testInterruptFlagPreservedForCallable() {
		// Set interrupt flag before execution
		Thread.currentThread().interrupt();

		final Callable<String> task = () -> "result";
		final String result = InterruptSafe.execute(task);

		assertEquals("result", result);
		assertTrue("Interrupt flag should be restored", Thread.currentThread().isInterrupted());

		// Clear the interrupt flag for subsequent tests
		Thread.interrupted();
	}

	@Test
	public void testInterruptFlagPreservedForRunnable() {
		// Set interrupt flag before execution
		Thread.currentThread().interrupt();

		final AtomicBoolean executed = new AtomicBoolean(false);
		final Runnable task = () -> executed.set(true);

		InterruptSafe.execute(task);

		assertTrue("Runnable should have been executed", executed.get());
		assertTrue("Interrupt flag should be restored", Thread.currentThread().isInterrupted());

		// Clear the interrupt flag for subsequent tests
		Thread.interrupted();
	}

	@Test
	public void testInterruptFlagNotSetWhenNotInterrupted() {
		final Callable<String> task = () -> "result";
		InterruptSafe.execute(task);

		assertFalse("Interrupt flag should not be set", Thread.currentThread().isInterrupted());
	}

	@Test(expected = IllegalAccessError.class)
	public void testNonInstantiable() throws Throwable {
		final Constructor<InterruptSafe> constructor = InterruptSafe.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		try {
			constructor.newInstance();
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
