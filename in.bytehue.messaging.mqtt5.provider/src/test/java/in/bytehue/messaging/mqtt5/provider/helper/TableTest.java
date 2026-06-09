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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TableTest {

	@Test
	public void testPrintWithHeaders() {
		final Table table = new Table();
		table.setHeaders("Name", "Value");
		table.addRow("key1", "val1");
		table.addRow("key2", "val2");

		final String output = table.print();

		assertNotNull("Output should not be null", output);
		assertTrue("Output should contain header 'Name'", output.contains("Name"));
		assertTrue("Output should contain header 'Value'", output.contains("Value"));
		assertTrue("Output should contain row 'key1'", output.contains("key1"));
		assertTrue("Output should contain row 'val2'", output.contains("val2"));
		assertTrue("Output should contain separator lines", output.contains("-"));
	}

	@Test
	public void testPrintWithoutHeaders() {
		final Table table = new Table();
		table.addRow("a", "b");
		table.addRow("c", "d");

		final String output = table.print();

		assertNotNull("Output should not be null", output);
		assertTrue("Output should contain row data 'a'", output.contains("a"));
		assertTrue("Output should contain row data 'd'", output.contains("d"));
	}

	@Test
	public void testPrintWithVerticalLines() {
		final Table table = new Table();
		table.setShowVerticalLines(true);
		table.setHeaders("Col1", "Col2");
		table.addRow("x", "y");

		final String output = table.print();

		assertNotNull("Output should not be null", output);
		assertTrue("Output should contain vertical separator '|'", output.contains("|"));
		assertTrue("Output should contain join separator '+'", output.contains("+"));
	}

	@Test
	public void testRightAlign() {
		final Table table = new Table();
		table.setRightAlign(true);
		table.setHeaders("Number", "Description");
		table.addRow("1", "First");
		table.addRow("100", "Hundredth");

		final String output = table.print();

		assertNotNull("Output should not be null", output);
		assertTrue("Output should contain 'First'", output.contains("First"));
		assertTrue("Output should contain 'Hundredth'", output.contains("Hundredth"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInconsistentColumnCountThrows() {
		final Table table = new Table();
		table.setHeaders("A", "B");
		table.addRow("1", "2", "3"); // 3 cells but 2 headers

		table.print();
	}

	@Test
	public void testEmptyTable() {
		final Table table = new Table();
		final String output = table.print();

		assertNotNull("Output should not be null", output);
		assertFalse("Output should not contain separator", output.contains("-"));
	}

}
