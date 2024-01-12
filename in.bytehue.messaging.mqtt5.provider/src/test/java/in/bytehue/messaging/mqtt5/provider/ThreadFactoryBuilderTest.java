/*******************************************************************************
 * Copyright 2020-2024 Amit Kumar Mondal
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
package in.bytehue.messaging.mqtt5.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import in.bytehue.messaging.mqtt5.provider.helper.ThreadFactoryBuilder;

public class ThreadFactoryBuilderTest {

	@Test
	public void canCreateThreadsTest() {
		final Runnable doNothing = () -> {
		};

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
		final ThreadFactory threadFactory = builder.build();
		final Thread thread = threadFactory.newThread(doNothing);

		assertNotNull("Thread must not be null", thread);
	}

	@Test
	public void startsWithFactoryNameTests() {
		final String factoryName = "someName";
		final Runnable doNothing = () -> {
		};

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder() //
				.setThreadFactoryName(factoryName);
		final ThreadFactory threadFactory = builder.build();

		for (int i = 0; i < 100; i++) {
			final Thread thread = threadFactory.newThread(doNothing);
			assertNotNull("Thread must not be null", thread);
			assertTrue("Thread name does not start with 'factoryName'", thread.getName().startsWith(factoryName));
		}
	}

	@Test
	public void uniqueThreadNameTest() {
		final String factoryName = "someName";
		final Runnable doNothing = () -> {
		};

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder() //
				.setThreadFactoryName(factoryName);

		final ThreadFactory threadFactory = builder.build();

		final Set<String> threadNames = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			final Thread thread = threadFactory.newThread(doNothing);
			assertNotNull("Thread must not be null", thread);
			assertTrue("Thread name does not start with 'factoryName'", thread.getName().startsWith(factoryName));
			assertFalse("Thread name is not unique", threadNames.contains(thread.getName()));
			threadNames.add(thread.getName());
		}
	}

	@Test
	public void setThreadNameFormatTest() {
		final String threadNameFormat = "my-thread-%d";
		final Runnable doNothing = () -> {
		};

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder() //
				.setThreadNameFormat(threadNameFormat);

		final ThreadFactory threadFactory = builder.build();

		for (int i = 0; i < 100; i++) {
			final String expectedSuffix = String.format(threadNameFormat, i);
			final Thread thread = threadFactory.newThread(doNothing);
			final String threadName = thread.getName();
			assertTrue("Thread name is not long enough", expectedSuffix.length() < threadName.length());

			final String actualSuffix = threadName.substring(threadName.length() - expectedSuffix.length());
			assertEquals("Thread name does not end with expected suffix", expectedSuffix, actualSuffix);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void setThreadNameFormatNoopTest() {
		// we expect the format to result in a different output on each call
		// this one won't do
		final String noopThreadNameFormat = "format";

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();

		builder.setThreadNameFormat(noopThreadNameFormat);
	}

	@Test(expected = NullPointerException.class)
	public void setThreadNameFormatNullTest() {
		final String nullThreadNameFormat = null;

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();

		builder.setThreadNameFormat(nullThreadNameFormat);
	}

	@Test
	public void createDaemonThreadsTest() {
		final String factoryName = "someName";
		final Runnable doNothing = () -> {
		};

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder() //
				.setThreadFactoryName(factoryName) //
				.setDaemon(true);

		final ThreadFactory threadFactory = builder.build();

		final Set<String> threadNames = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			final Thread thread = threadFactory.newThread(doNothing);
			assertNotNull("Thread must not be null", thread);
			assertTrue("Thread name does not start with 'factoryName'", thread.getName().startsWith(factoryName));
			assertFalse("Thread name is not unique", threadNames.contains(thread.getName()));
			assertTrue("Thread name is not marked as daemon", thread.isDaemon());
			threadNames.add(thread.getName());
		}
	}

	@Test
	public void createNonDaemonThreadsTest() {
		final String factoryName = "someName";
		final Runnable doNothing = () -> {
		};

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder() //
				.setThreadFactoryName(factoryName) //
				.setDaemon(false);

		final ThreadFactory threadFactory = builder.build();

		final Set<String> threadNames = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			final Thread thread = threadFactory.newThread(doNothing);
			assertNotNull("Thread must not be null", thread);
			assertTrue("Thread name does not start with 'factoryName'", thread.getName().startsWith(factoryName));
			assertFalse("Thread name is not unique", threadNames.contains(thread.getName()));
			assertFalse("Thread name is marked as daemon", thread.isDaemon());
			threadNames.add(thread.getName());
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPriorityTooLowTest() {
		final int priorityTooLow = Thread.MIN_PRIORITY - 1;

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();

		builder.setPriority(priorityTooLow);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPriorityTooHighTest() {
		final int priorityTooHigh = Thread.MAX_PRIORITY + 1;

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();

		builder.setPriority(priorityTooHigh);
	}

	@Test
	public void setPriorityTest() {
		final String factoryName = "someName";
		final Runnable doNothing = () -> {
		};

		for (int priority = Thread.MIN_PRIORITY; priority <= Thread.MAX_PRIORITY; priority++) {
			final ThreadFactoryBuilder builder = new ThreadFactoryBuilder() //
					.setThreadFactoryName(factoryName) //
					.setPriority(priority);

			final ThreadFactory threadFactory = builder.build();

			final Set<String> threadNames = new HashSet<>();
			for (int i = 0; i < 100; i++) {
				final Thread thread = threadFactory.newThread(doNothing);
				assertNotNull("Thread must not be null", thread);
				assertTrue("Thread name does not start with 'factoryName'", thread.getName().startsWith(factoryName));
				assertFalse("Thread name is not unique", threadNames.contains(thread.getName()));
				assertEquals("Thread has not correct priority", priority, thread.getPriority());
				threadNames.add(thread.getName());
			}
		}
	}

	@Test
	public void threadGroupTest() {
		final Runnable doNothing = () -> {
		};

		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();

		final ThreadFactory threadFactory = builder.build();
		final Thread thread = threadFactory.newThread(doNothing);

		assertEquals(Thread.currentThread().getThreadGroup(), thread.getThreadGroup());
	}

	@Test
	public void setThreadGroupTest() {
		final Runnable doNothing = () -> {
		};

		final String prefix = getClass().getSimpleName();
		final ThreadGroup threadGroup = new ThreadGroup(prefix + "-thread-group");

		final ThreadFactoryBuilder customBuilder = new ThreadFactoryBuilder() //
				.setThreadGroup(threadGroup);

		final ThreadFactory customThreadFactory = customBuilder.build();
		final Thread thread = customThreadFactory.newThread(doNothing);

		assertEquals(threadGroup, thread.getThreadGroup());
	}

	@Test(expected = NullPointerException.class)
	public void setThreadGroupNullTest() {
		final ThreadGroup nullThreadGroup = null;

		final ThreadFactoryBuilder customBuilder = new ThreadFactoryBuilder();

		customBuilder.setThreadGroup(nullThreadGroup);
	}

	@Test
	public void threadGroupIsInheritedCorrectlyTest() throws Throwable {
		final String factoryName = getClass().getSimpleName();
		final ThreadGroup threadGroup = new ThreadGroup(factoryName + "-thread-group");

		final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
		final CountDownLatch doneLatch = new CountDownLatch(1);

		final Runnable doNothing = () -> {
		};

		final Runnable testRunnable = () -> {
			try {
				// testing without a SecurityManager
				{
					assertNull(System.getSecurityManager());

					final ThreadFactory threadFactory = new ThreadFactoryBuilder().build();

					final Thread thread = threadFactory.newThread(doNothing);

					// inherited the ThreadGroup from the Thread we are running in (testingThread)
					assertEquals(threadGroup, thread.getThreadGroup());
				}

				// testing without a SecurityManager (SecurityManager has been unset)
				{
					assertNull(System.getSecurityManager());

					final ThreadFactory threadFactory = new ThreadFactoryBuilder().build();

					final Thread thread = threadFactory.newThread(doNothing);

					// inherited the ThreadGroup from the Thread we are running in (testingThread)
					assertEquals(threadGroup, thread.getThreadGroup());
				}
			} finally {
				doneLatch.countDown();
				System.setSecurityManager(null);
			}
		};

		// we use a custom thread for testing so that we have non-default ThreadGroup to
		// inherit
		final Thread testingThread = new Thread(threadGroup, testRunnable);
		testingThread.setUncaughtExceptionHandler((thread, throwable) -> throwableRef.compareAndSet(null, throwable));

		testingThread.start();

		if (!doneLatch.await(500, TimeUnit.MILLISECONDS)) {
			fail("Testing thread didn't finish in time");
		}

		if (null != throwableRef.get()) {
			throw throwableRef.get();
		}
	}

}
