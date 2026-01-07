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
package in.bytehue.messaging.mqtt5.remote.annotations;

import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.REMOTE_RESOURCE_MANAGEMENT_IMPLEMENTATION;
import static in.bytehue.messaging.mqtt5.remote.api.MqttRemoteConstants.REMOTE_RESOURCE_MANAGEMENT_VERSION;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;

/**
 * This annotation can be used to require the MQTT Remote Resource Management
 * implementation. It can be used directly, or as a meta-annotation.
 *
 * <p>
 * For example:
 *
 * <pre>
 * &#64;ProvideMQTTRemoteResourceManagement
 * </pre>
 *
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests or otherwise process the package.
 *
 * @since 1.0
 */
//@formatter:off
@Documented
@Retention(CLASS)
@Target({ TYPE, PACKAGE })
@Requirement(
        namespace = IMPLEMENTATION_NAMESPACE,
        name = REMOTE_RESOURCE_MANAGEMENT_IMPLEMENTATION,
        version = REMOTE_RESOURCE_MANAGEMENT_VERSION)
public @interface RequireMQTTRemoteResourceManagement {
    // This is a marker annotation.
}
