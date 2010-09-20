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
package javax.lang.model.util;

import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;

public class TypeKindVisitor6<R, P> extends SimpleTypeVisitor6<R, P> {
    protected TypeKindVisitor6() {
        DEFAULT_VALUE = null;
    }

    protected TypeKindVisitor6(R defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    public R visitNoType(NoType t, P p) {
        TypeKind type = t.getKind();
        switch (type) {
        case VOID:
            return visitNoTypeAsVoid(t, p);
        case PACKAGE:
            return visitNoTypeAsPackage(t, p);
        case NONE:
            return visitNoTypeAsNone(t, p);
        }
        return null;
    }

    public R visitNoTypeAsNone(NoType t, P p) {
        return defaultAction(t, p);
    }

    public R visitNoTypeAsPackage(NoType t, P p) {
        return defaultAction(t, p);
    }

    public R visitNoTypeAsVoid(NoType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitive(PrimitiveType t, P p) {
        TypeKind type = t.getKind();
        switch (type) {
        case BOOLEAN:
            return visitPrimitiveAsBoolean(t, p);
        case BYTE:
            return visitPrimitiveAsByte(t, p);
        case CHAR:
            return visitPrimitiveAsChar(t, p);
        case DOUBLE:
            return visitPrimitiveAsDouble(t, p);
        case FLOAT:
            return visitPrimitiveAsFloat(t, p);
        case INT:
            return visitPrimitiveAsInt(t, p);
        case LONG:
            return visitPrimitiveAsLong(t, p);
        case SHORT:
            return visitPrimitiveAsShort(t, p);
        }
        return null;
    }

    public R visitPrimitiveAsBoolean(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitiveAsByte(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitiveAsChar(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitiveAsDouble(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitiveAsFloat(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitiveAsInt(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitiveAsLong(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    public R visitPrimitiveAsShort(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }
}
