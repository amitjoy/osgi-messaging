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
package in.bytehue.messaging.mqtt5.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.Converting;
import org.osgi.util.converter.Functioning;

@Component(service = ConverterAdapter.class)
public final class ConverterAdapter implements Converter {

	private final Converter converter;

	@Activate
	public ConverterAdapter() {
		converter = Converters.standardConverter();
	}

	@Override
	public Converting convert(final Object obj) {
		return converter.convert(obj);
	}

	@Override
	public Functioning function() {
		return converter.function();
	}

	@Override
	public ConverterBuilder newConverterBuilder() {
		return converter.newConverterBuilder();
	}

}
