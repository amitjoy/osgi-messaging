/*******************************************************************************
 * Copyright 2020 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.getOptionalService;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.Logger;

/**
 * Represents a mathematical ordered pair of objects.
 * The order in which the objects appear in the pair is significant:
 * the ordered pair {@code (a, b)} is different from the ordered pair
 * {@code (b, a)} unless {@code a} and {@code b} are equal.
 *
 * @param <A> the type of the first element of the pair
 * @param <B> the type of the second element of the pair
 */
public final class Pair<A, B> {

    private A first;
    private B second;
    private Class<?> clazz;

    public static <A, B> Pair<A, B> emptyOf(final Class<?> clazz) {
        return new Pair<>(clazz);
    }

    /**
     * <p>
     * Creates a mutable pair of two objects inferring the generic types.
     * </p>
     *
     * <p>
     * This factory allows the pair to be created using inference to
     * obtain the generic types.
     * </p>
     *
     * @param <L> the first element type
     * @param <R> the second element type
     * @param first the first element, may be null
     * @param second the second element, may be null
     * @param clazz the effective type to be used
     * @return a pair formed from the two parameters, not null
     */
    public static <A, B> Pair<A, B> of(final A first, final B second, final Class<?> clazz) {
        return new Pair<>(first, second, clazz);
    }

    /**
     * Returns a new pair of {@code (null, null)}.
     */
    public Pair(final Class<?> clazz) {
        this(null, null, clazz);
    }

    /**
     * Returns a new ordered pair, containing the given objects.
     *
     * @param first the first member of the ordered pair
     * @param second the second member of the ordered pair
     * @param clazz the effective type to be used
     */
    public Pair(final A first, final B second, final Class<?> clazz) {
        this.first = first;
        this.second = second;
        this.clazz = clazz;
    }

    /**
     * Returns the first member of this ordered pair.
     *
     * @return the first member of the pair
     */
    public A first() {
        return first;
    }

    /**
     * Returns the second member of this ordered pair.
     *
     * @return the second member of the pair
     */
    public B second() {
        return second;
    }

    /**
     * Sets the left element of the pair.
     *
     * @param left the new value of the left element, may be null
     */
    public void setFirst(final A first) {
        this.first = first;
    }

    /**
     * Sets the right element of the pair.
     *
     * @param right the new value of the right element, may be null
     */
    public void setSecond(final B second) {
        this.second = second;
    }

    @SuppressWarnings("unchecked")
    public B findEffective(final BundleContext context, final Logger logger) {
        B effective = null;
        if (first != null) {
            effective = (B) getOptionalService(clazz, first.toString(), context, logger).orElse(null);
        }
        return effective == null ? second : effective;
    }
}
