package in.bytehue.messaging.mqtt5.provider.helper;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.osgi.service.log.Logger;

public class LogHelperTest {

	@Test
	public void testDebugThrottled() throws InterruptedException {
		// Setup Logger Stub
		final List<String> debugLogs = new ArrayList<>();

		final InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
				if (method.getName().equals("getName")) {
					return "TestLogger";
				}
				if (method.getName().equals("debug")) {
					// LogHelper calls logger.debug(message, args)
					// This typically maps to debug(String, Object...)
					if (args != null && args.length > 0 && args[0] instanceof String) {
						debugLogs.add((String) args[0]);
					}
				}
				return null; // void methods return null
			}
		};

		final Logger logger = (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(),
				new Class<?>[] { Logger.class }, handler);

		// Instantiate LogHelper
		final LogHelper logHelper = new LogHelper(logger, null);

		final String key = "throttle-key";
		final String msg = "Test Message";
		final long throttleTime = 15_000L;

		// First call - should log
		logHelper.debugThrottled(key, throttleTime, MILLISECONDS, msg);
		assertThat(debugLogs).hasSize(1);
		assertThat(debugLogs.get(0)).isEqualTo(msg);

		// Logging periodically - should be throttled
		logHelper.debugThrottled(key, throttleTime, MILLISECONDS, msg);
		Thread.sleep(2000);

		logHelper.debugThrottled(key, throttleTime, MILLISECONDS, msg);
		Thread.sleep(2000);

		logHelper.debugThrottled(key, throttleTime, MILLISECONDS, msg);
		Thread.sleep(2000);

		logHelper.debugThrottled(key, throttleTime, MILLISECONDS, msg);
		Thread.sleep(2000);

		logHelper.debugThrottled(key, throttleTime, MILLISECONDS, msg);
		Thread.sleep(2000);

		logHelper.debugThrottled(key, throttleTime, MILLISECONDS, msg);

		// Should still be 1 log
		assertThat(debugLogs).hasSize(1);

		// Wait for throttle interval to pass
		// Sleep a bit more than throttleTime to ensure expiration
		Thread.sleep(throttleTime);

		// Next call - should log with suppressed count
		logHelper.debugThrottled(key, throttleTime, MILLISECONDS, msg);

		assertThat(debugLogs).hasSize(2);
		final String secondLog = debugLogs.get(1);
		assertThat(secondLog).startsWith(msg);
		assertThat(secondLog).contains("suppressed 6");
	}
}
