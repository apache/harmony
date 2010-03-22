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

package org.apache.harmony.beans.tests.support.mock;

import java.lang.reflect.Field;
import java.util.Collection;

public class MockBean4CodecPrimitives {

    boolean bool = true;

    byte b = 1;

    char c = 'a';

    double d = 3.14;

    float f = 3.14F;

    int i = 4;

    long l = 8;

    short s = 2;

    Boolean boolobj = Boolean.FALSE;

    Byte bobj = new Byte((byte) 11);

    Character cobj = new Character('A');

    Double dobj = new Double(6.28);

    Float fobj = new Float(6.28F);

    Integer iobj = new Integer(44);

    Long lobj = new Long(88);

    Short sobj = new Short((short) 22);

    Object nill = "start with not null";

    Class<?> clazz = Collection.class;

    int zarr[] = { 1, 2, 3, 4 };

    String zarrarr[][] = { { "1", "2" }, { "3", "4", "5" } };

    public MockBean4CodecPrimitives() {
        super();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        try {
            Field fields[] = getClass().getDeclaredFields();
            for (Field element : fields) {
                Object mine = element.get(this);
                Object other = element.get(obj);
                if (mine == null ? other != null : !mine.equals(other)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("impossible!!");
        }
    }

    @Override
    public int hashCode() {
        try {
            int code = 0;
            Field fields[] = getClass().getDeclaredFields();
            for (Field element : fields) {
                Object mine = element.get(this);
                if (mine instanceof MockBean4CodecPrimitives) {
                    continue;
                }
                if (mine != null) {
                    code += mine.hashCode();
                }
            }
            return code;
        } catch (Exception e) {
            throw new RuntimeException("impossible!!");
        }
    }

    /**
     * @return Returns the b.
     */
    public byte getB() {
        return b;
    }

    /**
     * @param b
     *            The b to set.
     */
    public void setB(byte b) {
        this.b = b;
    }

    /**
     * @return Returns the bobj.
     */
    public Byte getBobj() {
        return bobj;
    }

    /**
     * @param bobj
     *            The bobj to set.
     */
    public void setBobj(Byte bobj) {
        this.bobj = bobj;
    }

    /**
     * @return Returns the bool.
     */
    public boolean isBool() {
        return bool;
    }

    /**
     * @param bool
     *            The bool to set.
     */
    public void setBool(boolean bool) {
        this.bool = bool;
    }

    /**
     * @return Returns the boolobj.
     */
    public Boolean getBoolobj() {
        return boolobj;
    }

    /**
     * @param boolobj
     *            The boolobj to set.
     */
    public void setBoolobj(Boolean boolobj) {
        this.boolobj = boolobj;
    }

    /**
     * @return Returns the c.
     */
    public char getC() {
        return c;
    }

    /**
     * @param c
     *            The c to set.
     */
    public void setC(char c) {
        this.c = c;
    }

    /**
     * @return Returns the cobj.
     */
    public Character getCobj() {
        return cobj;
    }

    /**
     * @param cobj
     *            The cobj to set.
     */
    public void setCobj(Character cobj) {
        this.cobj = cobj;
    }

    /**
     * @return Returns the d.
     */
    public double getD() {
        return d;
    }

    /**
     * @param d
     *            The d to set.
     */
    public void setD(double d) {
        this.d = d;
    }

    /**
     * @return Returns the dobj.
     */
    public Double getDobj() {
        return dobj;
    }

    /**
     * @param dobj
     *            The dobj to set.
     */
    public void setDobj(Double dobj) {
        this.dobj = dobj;
    }

    /**
     * @return Returns the f.
     */
    public float getF() {
        return f;
    }

    /**
     * @param f
     *            The f to set.
     */
    public void setF(float f) {
        this.f = f;
    }

    /**
     * @return Returns the fobj.
     */
    public Float getFobj() {
        return fobj;
    }

    /**
     * @param fobj
     *            The fobj to set.
     */
    public void setFobj(Float fobj) {
        this.fobj = fobj;
    }

    /**
     * @return Returns the i.
     */
    public int getI() {
        return i;
    }

    /**
     * @param i
     *            The i to set.
     */
    public void setI(int i) {
        this.i = i;
    }

    /**
     * @return Returns the iobj.
     */
    public Integer getIobj() {
        return iobj;
    }

    /**
     * @param iobj
     *            The iobj to set.
     */
    public void setIobj(Integer iobj) {
        this.iobj = iobj;
    }

    /**
     * @return Returns the l.
     */
    public long getL() {
        return l;
    }

    /**
     * @param l
     *            The l to set.
     */
    public void setL(long l) {
        this.l = l;
    }

    /**
     * @return Returns the lobj.
     */
    public Long getLobj() {
        return lobj;
    }

    /**
     * @param lobj
     *            The lobj to set.
     */
    public void setLobj(Long lobj) {
        this.lobj = lobj;
    }

    /**
     * @return Returns the nill.
     */
    public Object getNill() {
        return nill;
    }

    /**
     * @param nill
     *            The nill to set.
     */
    public void setNill(Object nill) {
        this.nill = nill;
    }

    /**
     * @return Returns the s.
     */
    public short getS() {
        return s;
    }

    /**
     * @param s
     *            The s to set.
     */
    public void setS(short s) {
        this.s = s;
    }

    /**
     * @return Returns the sobj.
     */
    public Short getSobj() {
        return sobj;
    }

    /**
     * @param sobj
     *            The sobj to set.
     */
    public void setSobj(Short sobj) {
        this.sobj = sobj;
    }

    /**
     * @return Returns the clazz.
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * @param clazz
     *            The clazz to set.
     */
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * @return Returns the zarr.
     */
    public int[] getZarr() {
        return zarr;
    }

    /**
     * @param arr
     *            The zarr to set.
     */
    public void setZarr(int[] arr) {
        this.zarr = arr;
    }

    /**
     * @return Returns the zarrarr.
     */
    public String[][] getZarrarr() {
        return zarrarr;
    }

    /**
     * @param arrarr
     *            The zarrarr to set.
     */
    public void setZarrarr(String[][] arrarr) {
        this.zarrarr = arrarr;
    }
}
