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

import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.formatProperties;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;

public class MessageHelperTest {

	@Test
	public void testAsStringWithSlicedBuffer() {
		// 1. Create a large backing array with "garbage" data
		byte[] largeArray = new byte[100];
		for (int i = 0; i < largeArray.length; i++) {
			largeArray[i] = 'X'; // Fill with 'X'
		}

		// 2. Place our actual content "HELLO" in the middle
		String content = "HELLO";
		byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
		int offset = 10;
		System.arraycopy(contentBytes, 0, largeArray, offset, contentBytes.length);

		// 3. Create a ByteBuffer that wraps the WHOLE array, but then slice it to just
		// look at "HELLO"
		ByteBuffer bigBuffer = ByteBuffer.wrap(largeArray);
		bigBuffer.position(offset);
		bigBuffer.limit(offset + contentBytes.length);

		// This slice represents what a network library might hand us:
		// a specific view window onto a larger shared buffer.
		ByteBuffer slicedBuffer = bigBuffer.slice();

		// Sanity check on the test setup
		assertThat(slicedBuffer.remaining()).isEqualTo(5);
		assertThat(slicedBuffer.array().length).isEqualTo(100); // The backing array is still huge

		// 4. Call the method under test
		String result = MessageHelper.asString(slicedBuffer);

		// 5. Assert that we ONLY got "HELLO", not "HELLOXXXXX..."
		assertThat(result).isEqualTo(content);
	}

	@Test
	public void formatProperties_withScalarValues_rendersCorrectly() {
		final Dictionary<String, Object> props = new Hashtable<>();
		props.put("#topic", 2);
		props.put("osgi.condition.id", "mqtt.subscription");

		final String result = formatProperties(props);

		assertThat(result).contains("#topic=2");
		assertThat(result).contains("osgi.condition.id=mqtt.subscription");
		assertThat(result).startsWith("{");
		assertThat(result).endsWith("}");
	}

	@Test
	public void formatProperties_withStringArray_expandsArrayInsteadOfObjectRef() {
		final Dictionary<String, Object> props = new Hashtable<>();
		props.put("topic", new String[] { "a/b", "c/d" });

		final String result = formatProperties(props);

		// Must NOT contain a raw object reference like [Ljava.lang.String;@...
		assertThat(result).doesNotContainPattern("\\[L.*@");
		assertThat(result).contains("topic=[a/b, c/d]");
	}

	@Test
	public void formatProperties_withIntegerArray_expandsArrayInsteadOfObjectRef() {
		final Dictionary<String, Object> props = new Hashtable<>();
		props.put("qos", new Integer[] { 0, 1, 2 });

		final String result = formatProperties(props);

		assertThat(result).doesNotContainPattern("\\[L.*@");
		assertThat(result).contains("qos=[0, 1, 2]");
	}

	@Test
	public void formatProperties_withEmptyDictionary_returnsEmptyBraces() {
		final Dictionary<String, Object> props = new Hashtable<>();

		final String result = formatProperties(props);

		assertThat(result).isEqualTo("{}");
	}

	@Test
	public void formatProperties_withMixedTypes_rendersAllEntries() {
		final Dictionary<String, Object> props = new Hashtable<>();
		props.put("#topic", 1);
		props.put("topic", new String[] { "sensor/temp" });
		props.put("[max]qos", 2);
		props.put("[min]qos", 0);

		final String result = formatProperties(props);

		assertThat(result).contains("#topic=1");
		assertThat(result).contains("topic=[sensor/temp]");
		assertThat(result).contains("[max]qos=2");
		assertThat(result).contains("[min]qos=0");
	}
}
