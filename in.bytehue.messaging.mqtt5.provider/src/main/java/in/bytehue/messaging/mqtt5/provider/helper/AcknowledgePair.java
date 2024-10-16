/*******************************************************************************
 * Copyright 2020-2025 Amit Kumar Mondal
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

public final class AcknowledgePair<B> {

	private B concrete;
	private String serviceFilter;

	private Class<?> clazz;

	public static <B> AcknowledgePair<B> emptyOf(final Class<?> clazz) {
		return new AcknowledgePair<>(clazz);
	}

	public static <B> AcknowledgePair<B> of(final String serviceFilter, final B second, final Class<?> clazz) {
		return new AcknowledgePair<>(serviceFilter, second, clazz);
	}

	public AcknowledgePair(final Class<?> clazz) {
		this(null, null, clazz);
	}

	public AcknowledgePair(final String serviceFilter, final B concrete, final Class<?> clazz) {
		this.clazz = clazz;
		this.concrete = concrete;
		this.serviceFilter = serviceFilter;
	}

	public String serviceFilter() {
		return serviceFilter;
	}

	public B concrete() {
		return concrete;
	}

	public void setServiceFilter(final String first) {
		this.serviceFilter = first;
	}

	public void setConcrete(final B concrete) {
		this.concrete = concrete;
	}

	@SuppressWarnings("unchecked")
	public B findEffective(final BundleContext context, final Logger logger) {
		B effective = null;
		if (serviceFilter != null) {
			effective = (B) getOptionalService(clazz, serviceFilter, context, logger).orElse(null);
		}
		return effective == null ? concrete : effective;
	}
}
