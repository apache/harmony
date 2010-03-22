/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Anton Avtamonov
 */
package javax.swing.table;

import javax.swing.BasicSwingTestCase;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class BasicSwingTableTestCase extends BasicSwingTestCase {
    public BasicSwingTableTestCase(final String name) {
        super(name);
    }

    protected class TestTableModelListener implements TableModelListener {
        private TableModelEvent event;

        public void tableChanged(final TableModelEvent e) {
            event = e;
        }

        public TableModelEvent getEvent() {
            return event;
        }

        public boolean eventOccured() {
            return event != null;
        }

        public void reset() {
            event = null;
        }
    }

    protected abstract class ArrayIndexOutOfBoundsExceptionalCase extends ExceptionalCase {
        @SuppressWarnings("unchecked")
        @Override
        public Class expectedExceptionClass() {
            return ArrayIndexOutOfBoundsException.class;
        }
    }
}
