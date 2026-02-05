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

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

import org.osgi.util.function.Consumer;
import org.osgi.util.function.Function;
import org.osgi.util.function.Predicate;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import in.bytehue.messaging.mqtt5.api.CancellablePromise;

public final class CancellablePromiseProvider<T> implements CancellablePromise<T> {

	private final Promise<T> delegate;
	private final BooleanSupplier cancel;

	public CancellablePromiseProvider(final Promise<T> delegate, final BooleanSupplier cancel) {
		this.delegate = delegate;
		this.cancel = cancel;
	}

	@Override
	public boolean cancel() {
		return cancel.getAsBoolean();
	}

	@Override
	public boolean isDone() {
		return delegate.isDone();
	}

	@Override
	public T getValue() throws InvocationTargetException, InterruptedException {
		return delegate.getValue();
	}

	@Override
	public Throwable getFailure() throws InterruptedException {
		return delegate.getFailure();
	}

	@Override
	public Promise<T> onResolve(final Runnable callback) {
		return delegate.onResolve(callback);
	}

	@Override
	public <R> Promise<R> then(final Success<? super T, ? extends R> success, final Failure failure) {
		return delegate.then(success, failure);
	}

	@Override
	public <R> Promise<R> then(final Success<? super T, ? extends R> success) {
		return delegate.then(success);
	}

	@Override
	public <R> Promise<R> flatMap(final Function<? super T, Promise<? extends R>> mapper) {
		return delegate.flatMap(mapper);
	}

	@Override
	public Promise<T> filter(final Predicate<? super T> predicate) {
		return delegate.filter(predicate);
	}

	@Override
	public <R> Promise<R> map(final Function<? super T, ? extends R> mapper) {
		return delegate.map(mapper);
	}

	@Override
	public Promise<T> recover(final Function<Promise<?>, ? extends T> recovery) {
		return delegate.recover(recovery);
	}

	@Override
	public Promise<T> recoverWith(final Function<Promise<?>, Promise<? extends T>> recovery) {
		return delegate.recoverWith(recovery);
	}

	@Override
	public Promise<T> fallbackTo(final Promise<? extends T> fallback) {
		return delegate.fallbackTo(fallback);
	}

	@Override
	public Promise<T> timeout(final long millis) {
		return delegate.timeout(millis);
	}

	@Override
	public CompletionStage<T> toCompletionStage() {
		return delegate.toCompletionStage();
	}

	@Override
	public Promise<T> thenAccept(Consumer<? super T> consumer) {
		return delegate.thenAccept(consumer);
	}

	@Override
	public Promise<T> onSuccess(Consumer<? super T> success) {
		return delegate.onSuccess(success);
	}

	@Override
	public Promise<T> onFailure(Consumer<? super Throwable> failure) {
		return delegate.onFailure(failure);
	}

	@Override
	public Promise<T> delay(long milliseconds) {
		return delegate.delay(milliseconds);
	}

	@Override
	public <F> Promise<T> onFailure(Consumer<? super F> failure, Class<? extends F> failureType) {
		return delegate.onFailure(failure, failureType);
	}

	@Override
	public Promise<T> recover(Function<Promise<?>, ? extends T> recovery, Class<?> failureType) {
		return delegate.recover(recovery, failureType);
	}

	@Override
	public Promise<T> recoverWith(Function<Promise<?>, Promise<? extends T>> recovery, Class<?> failureType) {
		return delegate.recoverWith(recovery, failureType);
	}

	@Override
	public Promise<T> fallbackTo(Promise<? extends T> fallback, Class<?> failureType) {
		return delegate.fallbackTo(fallback, failureType);
	}

}
