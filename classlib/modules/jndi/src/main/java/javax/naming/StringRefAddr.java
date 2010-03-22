/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.naming;

/**
 * A <code>StringRefAddr</code> refers to an address which is represented by a
 * string such as a URL or hostname.
 */
public class StringRefAddr extends RefAddr {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object.
     */
    private static final long serialVersionUID = -8913762495138505527L;

    /**
     * The address itself. For StringRefAddr the address is a string such as a
     * URL or hostname.
     * 
     * @serial
     */
    private String contents;

    /**
     * Constructs a <code>StringRefAddr</code> object using the supplied
     * address type and address.
     * 
     * @param type
     *            the address type which cannot be null
     * @param address
     *            the address itself which may be null
     */
    public StringRefAddr(String type, String address) {
        super(type);
        this.contents = address;
    }

    /**
     * Get the string containing this address.
     * 
     * @return a string containing this address which may be null
     */
    @Override
    public Object getContent() {
        return contents;
    }

}
