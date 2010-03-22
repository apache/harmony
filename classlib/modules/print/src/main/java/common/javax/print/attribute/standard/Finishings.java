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

package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/*
 * Table values are obtained from RFC2911: Internet Printing Protocol/1.1: 
 * Model and Semantics, section 4.2.6, http://ietf.org/rfc/rfc2911.txt?number=2911
 */
public class Finishings extends EnumSyntax implements DocAttribute, PrintJobAttribute,
        PrintRequestAttribute {
    private static final long serialVersionUID = -627840419548391754L;

    public static final Finishings NONE = new Finishings(3);

    public static final Finishings STAPLE = new Finishings(4);

    public static final Finishings COVER = new Finishings(6);

    public static final Finishings BIND = new Finishings(7);

    public static final Finishings SADDLE_STITCH = new Finishings(8);

    public static final Finishings EDGE_STITCH = new Finishings(9);

    public static final Finishings STAPLE_TOP_LEFT = new Finishings(20);

    public static final Finishings STAPLE_BOTTOM_LEFT = new Finishings(21);

    public static final Finishings STAPLE_TOP_RIGHT = new Finishings(22);

    public static final Finishings STAPLE_BOTTOM_RIGHT = new Finishings(23);

    public static final Finishings EDGE_STITCH_LEFT = new Finishings(24);

    public static final Finishings EDGE_STITCH_TOP = new Finishings(25);

    public static final Finishings EDGE_STITCH_RIGHT = new Finishings(26);

    public static final Finishings EDGE_STITCH_BOTTOM = new Finishings(27);

    public static final Finishings STAPLE_DUAL_LEFT = new Finishings(28);

    public static final Finishings STAPLE_DUAL_TOP = new Finishings(29);

    public static final Finishings STAPLE_DUAL_RIGHT = new Finishings(30);

    public static final Finishings STAPLE_DUAL_BOTTOM = new Finishings(31);

    private static final Finishings[] enumValueTable = { NONE, STAPLE, null, COVER, BIND,
            SADDLE_STITCH, EDGE_STITCH, null, null, null, null, null, null, null, null, null,
            null, STAPLE_TOP_LEFT, STAPLE_BOTTOM_LEFT, STAPLE_TOP_RIGHT, STAPLE_BOTTOM_RIGHT,
            EDGE_STITCH_LEFT, EDGE_STITCH_TOP, EDGE_STITCH_RIGHT, EDGE_STITCH_BOTTOM,
            STAPLE_DUAL_LEFT, STAPLE_DUAL_TOP, STAPLE_DUAL_RIGHT, STAPLE_DUAL_BOTTOM };

    private static final String[] stringTable = { "none", "staple", null, "cover", "bind",
            "saddle-stitch", "edge-stitch", null, null, null, null, null, null, null, null,
            null, null, "staple-top-left", "staple-bottom-left", "staple-top-right",
            "staple-bottom-right", "edge-stitch-left", "edge-stitch-top", "edge-stitch-right",
            "edge-stitch-bottom", "staple-dual-left", "staple-dual-top", "staple-dual-right",
            "staple-dual-bottom" };

    protected Finishings(int value) {
        super(value);
    }

    public final Class<? extends Attribute> getCategory() {
        return Finishings.class;
    }

    @Override
    protected EnumSyntax[] getEnumValueTable() {
        return enumValueTable.clone();
    }

    public final String getName() {
        return "finishings";
    }

    @Override
    protected int getOffset() {
        return 3;
    }

    @Override
    protected String[] getStringTable() {
        return stringTable.clone();
    }
}
