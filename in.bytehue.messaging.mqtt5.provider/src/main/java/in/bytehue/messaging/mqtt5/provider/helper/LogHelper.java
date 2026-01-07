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

import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.stackTraceToString;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.osgi.service.log.Logger;

import in.bytehue.messaging.mqtt5.provider.LogMirrorService;

public final class LogHelper {

	private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	private final Logger logger;
	private final String loggerName;
	private final LogMirrorService logMirror;

	public LogHelper(final Logger logger, final LogMirrorService logMirror) {
		this.logger = Objects.requireNonNull(logger, "logger");
		this.loggerName = logger.getName();
		this.logMirror = logMirror; // Null-safe
	}

	public void debug(final String message, final Object... args) {
		logger.debug(message, args);
		if (logMirror != null && logMirror.isMirrorEnabled()) {
			mirror("DEBUG", message, args, null);
		}
	}

	public void info(final String message, final Object... args) {
		logger.info(message, args);
		if (logMirror != null && logMirror.isMirrorEnabled()) {
			mirror("INFO", message, args, null);
		}
	}

	public void warn(final String message, final Object... args) {
		logger.warn(message, args);
		if (logMirror != null && logMirror.isMirrorEnabled()) {
			mirror("WARN", message, args, null);
		}
	}

	public void error(final String message, final Throwable t) {
		logger.error(message, t);
		if (logMirror != null && logMirror.isMirrorEnabled()) {
			mirror("ERROR", message, null, t);
		}
	}

	public void error(final String message, final Object... args) {
		logger.error(message, args);

		if (logMirror != null && logMirror.isMirrorEnabled()) {
			// Split trailing Throwable (if present) to print stacktrace.
			Throwable trailing = null;
			Object[] fmtArgs = args;
			if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable) {
				trailing = (Throwable) args[args.length - 1];
				final int n = args.length - 1;
				fmtArgs = new Object[n];
				if (n > 0) {
					System.arraycopy(args, 0, fmtArgs, 0, n);
				}
			}
			mirror("ERROR", message, fmtArgs, trailing);
		}
	}

	private void mirror(final String level, final String message, final Object[] args, final Throwable t) {
		// This is where the expensive formatting work happens,
		// only *after* we know the mirror is enabled.
		final String formatted = format(message, args);
		final String line = prefix(level) + formatted;

		logMirror.mirror(line);

		if (t != null) {
			logMirror.mirror(stackTraceToString(t));
		}
	}

	/**
	 * Creates the standard log prefix. Format: <ts> <threadId> <LEVEL>
	 * [<threadName>,<loggerName>]
	 */
	private String prefix(final String level) {
		final String ts = ZonedDateTime.now().format(TS_FMT);
		final Thread t = Thread.currentThread();
		return new StringBuilder(128).append(ts).append("   ").append(t.getId()).append(' ').append(level).append("  [")
				.append(t.getName()).append(',').append(loggerName).append("] ").toString();
	}

	/** Simple SLF4J-style {} formatter; null-safe. */
	private static String format(final String message, final Object[] args) {
		if (message == null) {
			return "null";
		}
		if (args == null || args.length == 0) {
			return message;
		}
		final StringBuilder sb = new StringBuilder(message.length() + 32);
		int argIndex = 0;
		int last = 0;
		while (argIndex < args.length) {
			final int idx = message.indexOf("{}", last);
			if (idx < 0) {
				break;
			}
			sb.append(message, last, idx);
			sb.append(String.valueOf(args[argIndex++]));
			last = idx + 2;
		}
		if (last < message.length()) {
			sb.append(message, last, message.length());
		}
		return sb.toString();
	}

}