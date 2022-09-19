package in.bytehue.messaging.mqtt5.provider.helper;

import java.util.concurrent.Callable;

public final class InterruptSafe {

	private InterruptSafe() {
		throw new IllegalAccessError("do not instanciate");
	}

	/**
	 * Executes task with interrupt flag reset. Interrupt flag will be set again, if
	 * it was set before
	 *
	 * @param task
	 * @return task result
	 */
	public static <V> V execute(final Callable<V> task) {
		boolean interrupted = false;
		final int currPriority = Thread.currentThread().getPriority();
		Thread.currentThread().setPriority(Math.min(currPriority + 1, Thread.MAX_PRIORITY));

		try {
			Thread.sleep(0);
		} catch (final InterruptedException e) { // NOSONAR
			interrupted = true;
		}

		try {
			return task.call();
		} catch (final Exception e) {
			return null;
		} finally {
			Thread.currentThread().setPriority(currPriority);
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Executes task with interrupt flag reset. Interrupt flag will be set again, if
	 * it was set before
	 *
	 * @param task
	 */
	public static void execute(final Runnable task) {
		execute(() -> {
			task.run();
			return null;
		});
	}
}