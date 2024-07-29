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
package in.bytehue.messaging.mqtt5.provider.helper;

import java.util.concurrent.Callable;

/**
 * A utility class that provides methods to safely execute tasks while managing
 * thread interruptions.
 *
 * <p>
 * The methods in this class ensure that the interrupt status of a thread is
 * restored after executing the task. Additionally, the priority of the
 * executing thread is temporarily increased to minimize preemption.
 * </p>
 */
public final class InterruptSafe {

    // Private constructor to prevent instantiation
    private InterruptSafe() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Executes a given task while handling thread interruptions and preserving the
     * original interrupt status.
     * 
     * @param <V>  the result type of the callable task
     * @param task the callable task to be executed
     * @return the result of the task, or {@code null} if the task throws an exception
     */
    public static <V> V execute(final Callable<V> task) {
        boolean wasInterrupted = Thread.interrupted(); // Clear the interrupt status and store the result
        final int originalPriority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(Math.min(originalPriority + 1, Thread.MAX_PRIORITY));

        try {
            // Yield to other threads if necessary
            Thread.yield();
            
            // Execute the task
            return task.call();
        } catch (final InterruptedException e) {
            // Re-interrupt if InterruptedException occurs during task execution
            Thread.currentThread().interrupt();
            return null;
        } catch (final Exception e) {
            // Handle all other exceptions by returning null
            return null;
        } finally {
            // Restore the original thread priority
            Thread.currentThread().setPriority(originalPriority);
            if (wasInterrupted) {
                // Restore the interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Executes a given runnable task while handling thread interruptions and preserving the
     * original interrupt status.
     * 
     * @param task the runnable task to be executed
     */
    public static void execute(final Runnable task) {
        execute(() -> {
            task.run();
            return null; // No result to return
        });
    }
}