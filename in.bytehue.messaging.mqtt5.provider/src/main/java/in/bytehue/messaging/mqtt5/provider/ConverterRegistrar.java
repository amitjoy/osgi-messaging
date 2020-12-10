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
package in.bytehue.messaging.mqtt5.provider;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

@Component
@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>=org.osgi.util.converter.Converter")
public final class ConverterRegistrar {

    private final ServiceRegistration<Converter> registration;

    @Activate
    public ConverterRegistrar(final BundleContext context) {
        final Converter converter = Converters.standardConverter();

        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("provider", "bytehue");

        registration = context.registerService(Converter.class, converter, properties);
    }

    @Deactivate
    void deactivate() {
        registration.unregister();
    }

}
