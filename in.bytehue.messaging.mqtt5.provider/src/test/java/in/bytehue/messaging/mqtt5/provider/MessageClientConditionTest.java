/*******************************************************************************
 * Copyright 2021 Amit Kumar Mondal
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

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.ConfigurationPid.CLIENT;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import in.bytehue.messaging.mqtt5.api.TargetCondition;

@RunWith(LaunchpadRunner.class)
public final class MessageClientConditionTest {

    @Service
    private Launchpad launchpad;

    @Service
    private ConfigurationAdmin configAdmin;

    static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").export("sun.misc");

    @Test
    public void test_default_condition() throws Exception {
        await().atMost(3, SECONDS).until(() -> launchpad.getService(MessageClientProvider.class).isPresent());
    }

    @Test
    public void test_unsatisfiable_condition() throws IOException {
        final Configuration config = configAdmin.getConfiguration(CLIENT, "?");

        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("condition.put", "(a=b)");

        config.update(properties);

        await().atMost(3, SECONDS).until(() -> !launchpad.getService(MessageClientProvider.class).isPresent());
    }

    @Test
    public void test_satisfiable_condition() throws IOException {
        final Configuration config = configAdmin.getConfiguration(CLIENT, "?");

        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("condition.put", "(a=b)");

        config.update(properties);

        await().atMost(3, SECONDS).until(() -> !launchpad.getService(MessageClientProvider.class).isPresent());

        final TargetCondition condition = new TargetCondition() {
        };

        launchpad.register(TargetCondition.class, condition, "a", "b");

        await().atMost(3, SECONDS).until(() -> launchpad.getService(MessageClientProvider.class).isPresent());
    }

}
