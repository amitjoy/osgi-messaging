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
package in.bytehue.messaging.mqtt5.provider.command;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;
import static org.osgi.service.condition.Condition.CONDITION_ID;
import static org.osgi.service.condition.Condition.INSTANCE;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.condition.Condition;

@Component
public final class GogoCommandActivator {

	private static final String CONDITION_VALUE = "gogo-available";
	private static final String GOGO_PACKAGE = "org.apache.felix.service.command";

	private final BundleContext bundleContext;
	private final AtomicReference<ServiceRegistration<Condition>> ref = new AtomicReference<>();

	@Activate
	public GogoCommandActivator(final BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		if (isGogoPackageImported()) {
			registerCondition();
		}
	}

	private boolean isGogoPackageImported() {
		final BundleWiring wiring = bundleContext.getBundle().adapt(BundleWiring.class);
		// @formatter:off
        return wiring.getRequiredWires(PACKAGE_NAMESPACE)
                     .stream()
                     .map(wire -> (String) wire.getCapability()
                                               .getAttributes()
                                               .get(PACKAGE_NAMESPACE))
                     .anyMatch(GOGO_PACKAGE::equals);
        // @formatter:on
	}

	private void registerCondition() {
		final Map<String, Object> properties = new HashMap<>();
		properties.put(CONDITION_ID, CONDITION_VALUE);

		final ServiceRegistration<Condition> reg = bundleContext.registerService(Condition.class, INSTANCE,
				FrameworkUtil.asDictionary(properties));
		ref.set(reg);
	}

	@Deactivate
	private void deregisterCondition() {
		ref.getAndUpdate(reg -> {
			if (reg != null) {
				reg.unregister();
			}
			return null;
		});
	}

}
