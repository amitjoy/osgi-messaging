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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

        // 3. Create a ByteBuffer that wraps the WHOLE array, but then slice it to just look at "HELLO"
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
}
