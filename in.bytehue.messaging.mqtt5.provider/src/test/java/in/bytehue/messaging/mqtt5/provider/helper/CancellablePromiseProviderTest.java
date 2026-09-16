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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import in.bytehue.messaging.mqtt5.api.CancellablePromise;

public class CancellablePromiseProviderTest {

	@Test
	public void test_cancel_invokes_supplier() {
		final AtomicBoolean cancelled = new AtomicBoolean(false);
		final Promise<String> delegate = Promises.resolved("value");

		final CancellablePromise<String> promise = new CancellablePromiseProvider<>(delegate, () -> {
			cancelled.set(true);
			return true;
		});

		final boolean result = promise.cancel();

		assertThat(result).isTrue();
		assertThat(cancelled.get()).isTrue();
	}

	@Test
	public void test_successful_promise_delegation() throws Exception {
		final Promise<String> delegate = Promises.resolved("hello-world");
		final CancellablePromise<String> promise = new CancellablePromiseProvider<>(delegate, () -> true);

		assertThat(promise.isDone()).isTrue();
		assertThat(promise.getValue()).isEqualTo("hello-world");
		assertThat(promise.getFailure()).isNull();

		final AtomicReference<String> callbackValue = new AtomicReference<>();
		promise.onSuccess(callbackValue::set);
		assertThat(callbackValue.get()).isEqualTo("hello-world");

		final Promise<String> mapped = promise.map(String::toUpperCase);
		assertThat(mapped.getValue()).isEqualTo("HELLO-WORLD");
	}

	@Test
	public void test_failed_promise_delegation() throws Exception {
		final Exception error = new IllegalStateException("operation-failed");
		final Promise<String> delegate = Promises.failed(error);
		final CancellablePromise<String> promise = new CancellablePromiseProvider<>(delegate, () -> false);

		assertThat(promise.isDone()).isTrue();
		assertThat(promise.getFailure()).isEqualTo(error);
		assertThatThrownBy(promise::getValue).isInstanceOf(InvocationTargetException.class);

		final AtomicReference<Throwable> failureRef = new AtomicReference<>();
		promise.onFailure(failureRef::set);
		assertThat(failureRef.get()).isEqualTo(error);
	}

	@Test
	public void test_deferred_resolution_and_callbacks() throws Exception {
		final Deferred<String> deferred = new Deferred<>();
		final AtomicBoolean cancelCalled = new AtomicBoolean(false);
		final CancellablePromise<String> promise = new CancellablePromiseProvider<>(deferred.getPromise(), () -> {
			cancelCalled.set(true);
			return true;
		});

		assertThat(promise.isDone()).isFalse();

		final AtomicBoolean resolved = new AtomicBoolean(false);
		promise.onResolve(() -> resolved.set(true));

		deferred.resolve("deferred-result");

		assertThat(promise.isDone()).isTrue();
		assertThat(promise.getValue()).isEqualTo("deferred-result");
		assertThat(resolved.get()).isTrue();
	}

	@Test
	public void test_to_completion_stage() {
		final Promise<Integer> delegate = Promises.resolved(42);
		final CancellablePromise<Integer> promise = new CancellablePromiseProvider<>(delegate, () -> true);

		final CompletableFuture<Integer> future = promise.toCompletionStage().toCompletableFuture();

		assertThat(future.isDone()).isTrue();
		assertThat(future.join()).isEqualTo(42);
	}

}
