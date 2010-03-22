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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Alexei Y. Zakharov
 */

package org.apache.harmony.jndi.provider.dns;

import java.util.Vector;
import java.util.Enumeration;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This class represents a domain protocol message.
 * 
 * @see RFC 1035
 */
public class Message {

    // header section fields

    /** an ID */
    private int id = 0;

    /** QR; false stands for QUERY request, true for RESPONSE */
    private boolean qr = ProviderConstants.QR_QUERY;

    /** OPCODE; QUERY or IQUERY or STATUS */
    private int opCode = 0;

    /** AA, Authoritative Answer */
    private boolean aa = false;

    /** TC, TrunCation */
    private boolean tc = false;

    /** RD, Recursion Desired */
    private boolean rd = false;

    /** RA, Recursion Available */
    private boolean ra = false;

    /** Z, always should be zero */
    // private int z;
    /** RCODE, Response CODE */
    private int rCode = 0;

    /** QDCOUNT, number of records in question section */
    private int qdCount = 0;

    /** ANCOUNT, number of records in answer section */
    private int anCount = 0;

    /** NSCOUNT, number of records in authority records section */
    private int nsCount = 0;

    /** ARCOUNT, number of records in additional section */
    private int arCount = 0;

    private Vector<QuestionRecord> questionRecords = null;

    private Vector<ResourceRecord> answerRRs = null;

    private Vector<ResourceRecord> authorityRRs = null;

    private Vector<ResourceRecord> additionalRRs = null;

    /** */
    public Message() {
        questionRecords = new Vector<QuestionRecord>();
        answerRRs = new Vector<ResourceRecord>();
        authorityRRs = new Vector<ResourceRecord>();
        additionalRRs = new Vector<ResourceRecord>();
    }

    /**
     * Constructs Message object from given parameters
     * 
     * @param id
     *            ID
     * @param qr
     *            QR
     * @param opCode
     *            OPCODE
     * @param aa
     *            AA
     * @param tc
     *            TC
     * @param rd
     *            RA
     * @param ra
     *            RA
     * @param rCode
     *            RCODE
     * @param qdCount
     *            QDCOUNT
     * @param anCount
     *            ANCOUNT
     * @param nsCount
     *            NSCOUNT
     * @param arCount
     *            ARCOUNT
     */
    public Message(int id, boolean qr, int opCode, boolean aa, boolean tc,
            boolean rd, boolean ra, int rCode, int qdCount, int anCount,
            int nsCount, int arCount) {
        this.id = id;
        this.qr = qr;
        this.opCode = opCode;
        this.aa = aa;
        this.tc = tc;
        this.rd = rd;
        this.ra = ra;
        this.rCode = rCode;
        this.qdCount = qdCount;
        this.anCount = anCount;
        this.nsCount = nsCount;
        this.arCount = arCount;
        questionRecords = new Vector<QuestionRecord>();
        answerRRs = new Vector<ResourceRecord>();
        authorityRRs = new Vector<ResourceRecord>();
        additionalRRs = new Vector<ResourceRecord>();
    }

    /**
     * Generates sequence of bytes that represents the message.
     * 
     * @param buffer
     *            the buffer to write bytes into
     * @param startIdx
     *            the index of <code>buffer</code> to start writing at
     * @return updated index of the <code>buffer</code>
     * @throws DomainProtocolException
     *             if something went wrong
     */
    public int writeBytes(byte[] buffer, int startIdx)
            throws DomainProtocolException {
        int idx = startIdx;
        int tmp = 0;

        // basic check
        if (buffer == null) {
            // jndi.32=buffer is null
            throw new DomainProtocolException(Messages.getString("jndi.32")); //$NON-NLS-1$
        }
        // ID
        idx = ProviderMgr.write16Int(id, buffer, idx);
        // QR
        tmp = ProviderMgr.setBit(tmp, ProviderConstants.QR_MASK, qr);
        // OPCODE
        tmp &= ~ProviderConstants.OPCODE_MASK;
        tmp |= (opCode & 0xf) << ProviderConstants.OPCODE_SHIFT;
        // AA
        tmp = ProviderMgr.setBit(tmp, ProviderConstants.AA_MASK, aa);
        // TC
        tmp = ProviderMgr.setBit(tmp, ProviderConstants.TC_MASK, tc);
        // RD
        tmp = ProviderMgr.setBit(tmp, ProviderConstants.RD_MASK, rd);
        // RA
        tmp = ProviderMgr.setBit(tmp, ProviderConstants.RA_MASK, ra);
        // Z, drop all those bits
        tmp &= ~ProviderConstants.Z_MASK;
        // RCODE
        tmp &= ~ProviderConstants.RCODE_MASK;
        tmp |= (rCode & 0xf) << ProviderConstants.RCODE_SHIFT;
        // write to buffer
        idx = ProviderMgr.write16Int(tmp, buffer, idx);
        // QDCOUNT
        idx = ProviderMgr.write16Int(qdCount, buffer, idx);
        // ANCOUNT
        idx = ProviderMgr.write16Int(anCount, buffer, idx);
        // NSCOUNT
        idx = ProviderMgr.write16Int(nsCount, buffer, idx);
        // ARCOUNT
        idx = ProviderMgr.write16Int(arCount, buffer, idx);
        // question section
        for (int i = 0; i < questionRecords.size(); i++) {
            QuestionRecord qr = questionRecords.elementAt(i);

            idx = qr.writeBytes(buffer, idx);
        }
        // answer section
        for (int i = 0; i < answerRRs.size(); i++) {
            ResourceRecord rr = answerRRs.elementAt(i);

            idx = rr.writeBytes(buffer, idx);
        }
        // authority section
        for (int i = 0; i < authorityRRs.size(); i++) {
            ResourceRecord rr = answerRRs.elementAt(i);

            idx = rr.writeBytes(buffer, idx);
        }
        // additional section
        for (int i = 0; i < additionalRRs.size(); i++) {
            ResourceRecord rr = answerRRs.elementAt(i);

            idx = rr.writeBytes(buffer, idx);
        }
        return idx;
    }

    /**
     * Parses given sequence of bytes and constructs a message object from it.
     * 
     * @param mesBytes
     *            the byte array that should be parsed
     * @param startIdx
     *            an index of <code>mesBytes</code> array to start the parsing
     *            at
     * @param mes
     *            an object to write a result to, should already be created
     * @return updated index of <code>mesBytes</code> array
     * @throws DomainProtocolException
     *             if some error has occurred
     */
    public static int parseMessage(byte[] mesBytes, int startIdx, Message mesObj)
            throws DomainProtocolException {
        int idx = startIdx;
        int tmp, tmp2;
        int qdCnt;
        int anCnt;
        int nsCnt;
        int arCnt;

        if (mesObj == null) {
            // jndi.58=The value of parameter mesObj is null
            throw new DomainProtocolException(Messages.getString("jndi.58")); //$NON-NLS-1$
        }
        // header section
        // ID
        mesObj.setId(ProviderMgr.parse16Int(mesBytes, idx));
        idx += 2;
        // QR & opCode & AA & TC & RD & RA & Z & rCode
        tmp = ProviderMgr.parse16Int(mesBytes, idx);
        idx += 2;
        // QR
        mesObj.setQR(ProviderMgr.checkBit(tmp, ProviderConstants.QR_MASK));
        // OPCODE
        tmp2 = (tmp & ProviderConstants.OPCODE_MASK) >> ProviderConstants.OPCODE_SHIFT;
        mesObj.setOpCode(tmp2);
        // AA
        mesObj.setAA(ProviderMgr.checkBit(tmp, ProviderConstants.AA_MASK));
        // TC
        mesObj.setTc(ProviderMgr.checkBit(tmp, ProviderConstants.TC_MASK));
        // RD
        mesObj.setRD(ProviderMgr.checkBit(tmp, ProviderConstants.RD_MASK));
        // RA
        mesObj.setRA(ProviderMgr.checkBit(tmp, ProviderConstants.RA_MASK));
        // RCODE
        tmp2 = (tmp & ProviderConstants.RCODE_MASK) >> ProviderConstants.RCODE_SHIFT;
        mesObj.setRCode(tmp2);
        // QDCOUNT
        qdCnt = ProviderMgr.parse16Int(mesBytes, idx);
        mesObj.setQDCount(qdCnt);
        idx += 2;
        // ANCOUNT
        anCnt = ProviderMgr.parse16Int(mesBytes, idx);
        mesObj.setANCount(anCnt);
        idx += 2;
        // NSCOUNT
        nsCnt = ProviderMgr.parse16Int(mesBytes, idx);
        mesObj.setNSCount(nsCnt);
        idx += 2;
        // ARCOUNT
        arCnt = ProviderMgr.parse16Int(mesBytes, idx);
        mesObj.setARCount(arCnt);
        idx += 2;
        // question section
        for (int i = 0; i < qdCnt; i++) {
            QuestionRecord qr = new QuestionRecord();
            idx = QuestionRecord.parseRecord(mesBytes, idx, qr);
            mesObj.addQuestionRecord(qr);
        }
        // answer section
        for (int i = 0; i < anCnt; i++) {
            ResourceRecord rr = new ResourceRecord();
            idx = ResourceRecord.parseRecord(mesBytes, idx, rr);
            mesObj.addAnswerRR(rr);
        }
        // authority section
        for (int i = 0; i < nsCnt; i++) {
            ResourceRecord rr = new ResourceRecord();
            idx = ResourceRecord.parseRecord(mesBytes, idx, rr);
            mesObj.addAuthorityRR(rr);
        }
        // additional section
        for (int i = 0; i < arCnt; i++) {
            ResourceRecord rr = new ResourceRecord();
            idx = ResourceRecord.parseRecord(mesBytes, idx, rr);
            mesObj.addAdditionalRR(rr);
        }
        return idx;
    }

    /**
     * @return string representation of this message
     */
    @Override
    @SuppressWarnings("nls")
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ID=" + id + "\n");
        if (qr) {
            sb.append(" QR");
        }
        sb.append(" OPCODE=" + opCode);
        if (aa) {
            sb.append(" AA");
        }
        if (tc) {
            sb.append(" TC");
        }
        if (rd) {
            sb.append(" RD");
        }
        if (ra) {
            sb.append(" RA");
        }
        sb.append(" RCODE=" + rCode);
        sb.append("\n");
        sb.append("QDCOUNT=" + qdCount);
        for (int i = 0; i < questionRecords.size(); i++) {
            sb.append("\n");
            sb.append(questionRecords.elementAt(i).toString());
        }
        sb.append("\n");
        sb.append(" ANCOUNT=" + anCount);
        for (int i = 0; i < answerRRs.size(); i++) {
            sb.append("\n");
            sb.append(answerRRs.elementAt(i).toString());
        }
        sb.append("\n");
        sb.append(" NSCOUNT=" + nsCount);
        for (int i = 0; i < authorityRRs.size(); i++) {
            sb.append("\n");
            sb.append(authorityRRs.elementAt(i).toString());
        }
        sb.append("\n");
        sb.append(" ARCOUNT=" + arCount);
        for (int i = 0; i < additionalRRs.size(); i++) {
            sb.append("\n");
            sb.append(additionalRRs.elementAt(i).toString());
        }
        return sb.toString();
    }

    /**
     * @return Returns the AA.
     */
    public boolean isAA() {
        return aa;
    }

    /**
     * @param aa
     *            The AA to set.
     */
    public void setAA(boolean aa) {
        this.aa = aa;
    }

    /**
     * @return Returns the anCount.
     */
    public int getANCount() {
        return anCount;
    }

    /**
     * @param anCount
     *            The anCount to set.
     */
    public void setANCount(int anCount) {
        this.anCount = anCount;
    }

    /**
     * @return Returns the arCount.
     */
    public int getARCount() {
        return arCount;
    }

    /**
     * @param arCount
     *            The arCount to set.
     */
    public void setARCount(int arCount) {
        this.arCount = arCount;
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        return id;
    }

    /**
     * @param id
     *            The id to set.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return Returns the nsCount.
     */
    public int getNSCount() {
        return nsCount;
    }

    /**
     * @param nsCount
     *            The nsCount to set.
     */
    public void setNSCount(int nsCount) {
        this.nsCount = nsCount;
    }

    /**
     * @return Returns the opCode.
     */
    public int getOpCode() {
        return opCode;
    }

    /**
     * @param opCode
     *            The opCode to set.
     */
    public void setOpCode(int opCode) {
        this.opCode = opCode;
    }

    /**
     * @return Returns the qdCount.
     */
    public int getQDCount() {
        return qdCount;
    }

    /**
     * @param qdCount
     *            The qdCount to set.
     */
    public void setQDCount(int qdCount) {
        this.qdCount = qdCount;
    }

    /**
     * @return Returns the QR.
     */
    public boolean getQR() {
        return qr;
    }

    /**
     * @param qr
     *            The QR to set.
     */
    public void setQR(boolean qr) {
        this.qr = qr;
    }

    /**
     * @return Returns the RA.
     */
    public boolean isRA() {
        return ra;
    }

    /**
     * @param ra
     *            The RA to set.
     */
    public void setRA(boolean ra) {
        this.ra = ra;
    }

    /**
     * @return Returns the rCode.
     */
    public int getRCode() {
        return rCode;
    }

    /**
     * @param code
     *            The rCode to set.
     */
    public void setRCode(int code) {
        rCode = code;
    }

    /**
     * @return Returns the RD.
     */
    public boolean isRD() {
        return rd;
    }

    /**
     * @param rd
     *            The RD to set.
     */
    public void setRD(boolean rd) {
        this.rd = rd;
    }

    /**
     * @return Returns the TC.
     */
    public boolean isTc() {
        return tc;
    }

    /**
     * @param tc
     *            The TC to set.
     */
    public void setTc(boolean tc) {
        this.tc = tc;
    }

    /**
     * @return question records that are contained by the current message.
     */
    public Enumeration<QuestionRecord> getQuestionRecords() {
        return questionRecords.elements();
    }

    /**
     * Adds a new question record to the message.
     * 
     * @param qr
     *            a record to add
     */
    public void addQuestionRecord(QuestionRecord qr) {
        questionRecords.addElement(qr);
    }

    /**
     * @return available answer resource records
     */
    public Enumeration<ResourceRecord> getAnswerRRs() {
        return answerRRs.elements();
    }

    /**
     * Adds a new question record to the message.
     * 
     * @param rr
     *            a record to add
     */
    public void addAnswerRR(ResourceRecord rr) {
        answerRRs.addElement(rr);
    }

    /**
     * @return available authority resource records
     */
    public Enumeration<ResourceRecord> getAuthorityRRs() {
        return authorityRRs.elements();
    }

    /**
     * Adds a new question record to the message.
     * 
     * @param rr
     *            a record to add
     */
    public void addAuthorityRR(ResourceRecord rr) {
        authorityRRs.addElement(rr);
    }

    /**
     * @return available additional resource records
     */
    public Enumeration<ResourceRecord> getAdditionalRRs() {
        return additionalRRs.elements();
    }

    /**
     * Adds a new question record to the message.
     * 
     * @param rr
     *            a record to add
     */
    public void addAdditionalRR(ResourceRecord rr) {
        additionalRRs.addElement(rr);
    }

}
