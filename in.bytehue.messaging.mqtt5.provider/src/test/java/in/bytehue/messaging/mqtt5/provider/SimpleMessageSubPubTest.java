package in.bytehue.messaging.mqtt5.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;

public final class SimpleMessageSubPubTest {

    private final LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun").debug();

    @Before
    public void setup() {
    }

    @After
    public void teardown() {
    }

    @Test
    public void test() throws Exception {
        try (Launchpad launchpad = builder.set("ABC", "DEF").create()) {
            assertThat("AMIT").isEqualTo("AMIT");
        }
    }

}
