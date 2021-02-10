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
package in.bytehue.messaging.mqtt5.provider.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Table {

    private static final String HORIZONTAL_SEP = "-";
    private String verticalSep;
    private String joinSep;
    private String[] headers;
    private final List<String[]> rows = new ArrayList<>();
    private boolean rightAlign;

    public Table() {
        setShowVerticalLines(false);
    }

    public void setRightAlign(final boolean rightAlign) {
        this.rightAlign = rightAlign;
    }

    public void setShowVerticalLines(final boolean showVerticalLines) {
        verticalSep = showVerticalLines ? "|" : "";
        joinSep = showVerticalLines ? "+" : " ";
    }

    public void setHeaders(final String... headers) {
        this.headers = headers;
    }

    public void addRow(final String... cells) {
        rows.add(cells);
    }

    public void print() {
        int[] maxWidths = headers != null ? Arrays.stream(headers).mapToInt(String::length).toArray() : null;

        for (final String[] cells : rows) {
            if (maxWidths == null) {
                maxWidths = new int[cells.length];
            }
            if (cells.length != maxWidths.length) {
                throw new IllegalArgumentException("Number of row-cells and headers should be consistent");
            }
            for (int i = 0; i < cells.length; i++) {
                maxWidths[i] = Math.max(maxWidths[i], cells[i].length());
            }
        }

        if (headers != null) {
            printLine(maxWidths);
            printRow(headers, maxWidths);
            printLine(maxWidths);
        }
        for (final String[] cells : rows) {
            printRow(cells, maxWidths);
        }
        if (headers != null) {
            printLine(maxWidths);
        }
    }

    private void printLine(final int[] columnWidths) {
        for (int i = 0; i < columnWidths.length; i++) {
            final String line = String.join("",
                    Collections.nCopies(columnWidths[i] + verticalSep.length() + 1, HORIZONTAL_SEP));
            System.out.print(joinSep + line + (i == columnWidths.length - 1 ? joinSep : ""));
        }
        System.out.println();
    }

    private void printRow(final String[] cells, final int[] maxWidths) {
        for (int i = 0; i < cells.length; i++) {
            final String s = cells[i];
            final String verStrTemp = i == cells.length - 1 ? verticalSep : "";
            if (rightAlign) {
                System.out.printf("%s %" + maxWidths[i] + "s %s", verticalSep, s, verStrTemp);
            } else {
                System.out.printf("%s %-" + maxWidths[i] + "s %s", verticalSep, s, verStrTemp);
            }
        }
        System.out.println();
    }

}
