/*******************************************************************************
 * Copyright 2020-2023 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.provider.command.MqttCommand.PID;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component
public final class GogoCommandActivator {

	private static final String GOGO_PACKAGE = "org.apache.felix.service.command";

	private final BundleContext bundleContext;
	private final ConfigurationAdmin configAdmin;

	@Activate
	public GogoCommandActivator(
	// @formatter:off
            final BundleContext bundleContext,
            @Reference
            final ConfigurationAdmin configAdmin
    // @formatter:on
	) {
		this.configAdmin = configAdmin;
		this.bundleContext = bundleContext;

		if (isGogoPackageImported()) {
			createGogoCommandConfig();
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
                     .anyMatch(pkg -> GOGO_PACKAGE.equals(pkg));
        // @formatter:on
	}

	private void createGogoCommandConfig() {
		try {
			final Configuration configuration = configAdmin.getConfiguration(PID, "?");
			configuration.updateIfDifferent(new Hashtable<>());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Deactivate
	private void deleteGogoCommandConfig() {
		try {
			final Configuration configuration = configAdmin.getConfiguration(PID, "?");
			configuration.delete();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}
