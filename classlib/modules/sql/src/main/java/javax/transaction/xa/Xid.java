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

package javax.transaction.xa;

/**
 * An interface which provides a mapping for the X/Open XID transaction
 * identifier structure. The Xid interface is used by the Transaction Manager
 * and the Resource managers. It is not typically used by application programs.
 */
public interface Xid {

    /**
     * The maximum number of bytes which will be returned by
     * getGlobaltransaction Id
     */
    public static final int MAXGTRIDSIZE = 64;

    /**
     * The maximum number of bytes which will be returned by getBranchQualifier
     */
    public static final int MAXBQUALSIZE = 64;

    /**
     * Gets the transaction branch qualifier component of the XID.
     * 
     * @return an array of bytes containing the transaction branch qualifier.
     */
    public byte[] getBranchQualifier();

    /**
     * Gets the format identifier component of the XID.
     * 
     * @return an integer containing the format identifier. 0 means the OSI CCR
     *         format.
     */
    public int getFormatId();

    /**
     * Gets the global transaction identifier component of the XID.
     * 
     * @return an array of bytes containing the global transaction identifier.
     */
    public byte[] getGlobalTransactionId();
}
