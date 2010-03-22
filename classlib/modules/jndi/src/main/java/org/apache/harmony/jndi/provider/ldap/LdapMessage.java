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

package org.apache.harmony.jndi.provider.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.ldap.Control;

import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Decodable;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1ChoiceWrap.ChosenValue;
import org.apache.harmony.security.asn1.ASN1Integer;

/**
 * Ldap Message. Refer to {@link http://www.rfc-editor.org/rfc/rfc2251.txt} for
 * detailed information
 * 
 */
public class LdapMessage implements ASN1Encodable, ASN1Decodable {

    /**
     * operation request which could be encoded using ASN.1 BER
     */
    private ASN1Encodable requestOp;

    /**
     * operation response operation which could be decoded using ASN.1 BER
     */
    private ASN1Decodable responseOp;

    /**
     * controls for this message
     */
    private Control[] controls;

    /**
     * index of the operation, determine which operation is encapsulated in this
     * message.
     */
    private int opIndex;


    /**
     * unique request id for each session
     */
    private int messageId;

    private static int nextMessageId = 1;

    /**
     * Get next unique message id
     * 
     * @return the next unique message id
     */
    public static synchronized int getNextMessageId() {
        return nextMessageId++;
    }

    /**
     * Get message id of this message
     * 
     * @return id of this message
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * Construct a request message. <code>op</code> may not be
     * <code>null</code>. <code>controls</code> is <code>null</code> or a
     * array of zero length means there is no control for this message.
     * 
     * @param opIndex
     *            request index to indicate which operation is encapsulated
     * @param op
     *            encodable operation
     * @param controls
     *            message controls
     */
    public LdapMessage(int opIndex, ASN1Encodable op, Control[] controls) {
        this.opIndex = opIndex;
        requestOp = op;
        this.controls = controls;
        messageId = getNextMessageId();
    }

    /**
     * Construct a response message. <code>op</code> indicate which operation
     * to be used, and the message would be initialized after calling
     * <code>decode(byte[])</code> or <code>decode(InputStream)</code>
     * method.
     * 
     * @param op
     *            response index to indicate which operation to be encapsulated
     */
    public LdapMessage(ASN1Decodable op) {
        responseOp = op;
        opIndex = -1;
        messageId = -1;
    }

    /**
     * Encode this message using ASN.1 Basic Encoding Rules (BER)
     * 
     * @return the encoded values of this <code>LdapMessage</code> instance
     */
    public byte[] encode() {
        return LdapASN1Constant.LDAPMessage.encode(this);
    }

    /**
     * Decode values from <code>InputStream</code> using ASN.1 BER, and the
     * decoded values will initialize this <code>LdapMessage</code> instance.
     * 
     * @param in
     * 
     * @throws IOException
     *             error occurs when decoding
     */
    public void decode(InputStream in) throws IOException {
        Object[] values = (Object[]) LdapASN1Constant.LDAPMessage.decode(in);
        decodeValues(values);
    }

    /**
     * Return controls of the message, if there is no control, <code>null</code>
     * will be returned.
     * 
     * @return controls of the message
     */
    public Control[] getControls() {
        return controls;
    }

    @SuppressWarnings("unchecked")
    public void decodeValues(Object[] values) {
        messageId = ASN1Integer.toIntValue(values[0]);
        if (values[1] == null) {
            return;
        }

        ChosenValue chosen = (ChosenValue) values[1];
        opIndex = chosen.getIndex();
        // failed to retrieve responseOp
        responseOp = getResponseOp();
        if (responseOp == null) {
            return;
        }

        if (opIndex == LdapASN1Constant.OP_SEARCH_RESULT_DONE
                || opIndex == LdapASN1Constant.OP_SEARCH_RESULT_ENTRY
                || opIndex == LdapASN1Constant.OP_SEARCH_RESULT_REF) {
            /*
             * we use LdapSearchResult to decode all types of search responses,
             * so we need index to determine which type of response to decode
             */
            responseOp.decodeValues(new Object[] { chosen });
        } else {
            responseOp.decodeValues((Object[]) chosen.getValue());
        }

        if (values[2] != null) {
            Collection<Object[]> list = (Collection<Object[]>) values[2];
            controls = new Control[list.size()];
            int index = 0;
            for (Object[] objects : list) {
                LdapControl lcontrol = new LdapControl();
                lcontrol.decodeValues(objects);
                controls[index++] = lcontrol.getControl();
            }
        }
    }

    public void encodeValues(Object[] values) {
        values[0] = ASN1Integer.fromIntValue(messageId);
        // ABANDON, UNBIND and DELETE request are ASN.1 primitive
        if (opIndex == LdapASN1Constant.OP_ABANDON_REQUEST
                || opIndex == LdapASN1Constant.OP_DEL_REQUEST
                || opIndex == LdapASN1Constant.OP_UNBIND_REQUEST
                || opIndex == LdapASN1Constant.OP_SEARCH_RESULT_REF) {
            Object[] objs = new Object[1];
            requestOp.encodeValues(objs);
            values[1] = new ChosenValue(opIndex, objs[0]);
        } else {
            values[1] = new ChosenValue(opIndex, requestOp);
        }

        // encode controls, wrap to LdapControl, so it could be encoded
        if (controls != null) {
            List<LdapControl> list = new ArrayList<LdapControl>(controls.length);
            for (int i = 0; i < controls.length; ++i) {
                list.add(new LdapControl(controls[i]));
            }
            values[2] = list;
        }
    }

    /**
     * Get index of the operation, determine which operation is encapsulated in
     * this message. If this <code>LdapMessage</code> instance is not initial,
     * <code>-1</code> will be returned.
     * 
     * @return index of the operation encapsulated in the message
     */
    public int getOperationIndex() {
        return opIndex;
    }

    public ASN1Decodable getResponseOp() {
        return responseOp;
    }

    public ASN1Encodable getRequestOp() {
        return requestOp;
    }

}
