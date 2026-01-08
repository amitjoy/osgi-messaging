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
package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.provider.LogMirrorService.PID;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import in.bytehue.messaging.mqtt5.provider.LogMirrorService.Config;

@Designate(ocd = Config.class)
@Component(service = LogMirrorService.class, immediate = true, configurationPid = PID)
public final class LogMirrorService {

	@ObjectClassDefinition(name = "Log Mirror Configuration", description = "Configuration for the console log mirror")
	public @interface Config {
		@AttributeDefinition(name = "Enabled", description = "Enable real-time log mirroring to System.out")
		boolean enabled() default false;
	}

	public static final String PID = "in.bytehue.mqtt.debug";
	private static final String PROP_ENABLED = "enabled";

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private ConfigurationAdmin cm;

	private volatile Config config;
	private Thread consumerThread;
	private volatile boolean isRunning;
	private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(5000);

	private final Runnable consumerTask = () -> {
		try {
			while (isRunning || !queue.isEmpty()) {
				final String message = queue.take(); // Blocks until a message is available
				// Check the flag *after* dequeuing. This ensures that
				// if the flag is turned off, we still drain the queue
				// but don't print anything.
				if (isMirrorEnabled()) {
					System.out.println(message);
				}
			}
		} catch (final InterruptedException e) {
			// We were told to stop.
			Thread.currentThread().interrupt();
		} finally {
			// Drain remaining logs
			while (!queue.isEmpty()) {
				if (isMirrorEnabled()) {
					System.out.println(queue.poll());
				} else {
					queue.poll();
				}
			}
		}
	};

	@Activate
	void activate(final Config config) {
		this.config = config;
		this.isRunning = true;
		this.consumerThread = new Thread(consumerTask);
		this.consumerThread.setName("mqtt-log-mirror");
		this.consumerThread.setDaemon(true);
		this.consumerThread.start();
		logger.info("Log mirror has been activated with enabled={}", config.enabled());
	}

	@Modified
	void modified(final Config config) {
		this.config = config;
		logger.info("Log mirror has been updated with enabled={}", config.enabled());
	}

	@Deactivate
	void deactivate() {
		this.isRunning = false;
		if (consumerThread != null) {
			consumerThread.interrupt();
		}
	}

	public boolean isMirrorEnabled() {
		final Config localConfig = config;
		return localConfig != null && localConfig.enabled();
	}

	public void enableMirror() {
		updateConfig(true);
	}

	public void disableMirror() {
		updateConfig(false);
	}

	public void mirror(final String formattedLogMessage) {
		if (!isRunning) {
			return;
		}
		queue.offer(formattedLogMessage);
	}

	private void updateConfig(final boolean enabled) {
		try {
			final Configuration cfg = cm.getConfiguration(PID, "?");
			Dictionary<String, Object> props = cfg.getProperties();
			if (props == null) {
				props = new Hashtable<>();
			}
			props.put(PROP_ENABLED, enabled);
			cfg.updateIfDifferent(props);
		} catch (final IOException e) {
			logger.error("Failed to update log mirror configuration (PID: {})", PID, e);
		}
	}
}