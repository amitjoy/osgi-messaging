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
