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

public class MockBean4Codec extends MockBean4CodecPrimitives {

    public static MockBean4Codec getInstanceOfManyChanges() {
        MockBean4Codec bean = new MockBean4Codec();
        bean.setB((byte) 127);
        bean.setBackRef(bean);
        bean.setBobj(new Byte((byte) 127));
        bean.setBool(false);
        bean.setBoolobj(Boolean.TRUE);
        bean.getBornFriend().setClazz(Exception.class);
        bean.getBornFriend().getZarr()[0] = 888;
        bean.setC('Z');
        bean.setClazz(String.class);
        bean.setCobj(new Character('z'));
        bean.setD(123.456);
        bean.setDobj(new Double(123.456));
        bean.setF(12.34F);
        bean.setFobj(new Float(12.34F));
        bean.setFriend(new MockBean4Codec());
        bean.getFriend().setClazz(MockBean4Codec.class);
        bean.setI(999);
        bean.setIobj(new Integer(999));
        bean.setL(8888888);
        bean.setLobj(new Long(8888888));
        bean.setName("Li Yang");
        bean.setNill(null);
        bean.setS((short) 55);
        bean.setSobj(new Short((short) 55));
        bean.setZarr(new int[] { 3, 2, 1 });
        bean.setZarrarr(new String[][] { { "6", "6", "6" } });
        return bean;
    }

    public static MockBean4Codec getInstanceOfManyChanges2() {
        MockBean4Codec bean = new MockBean4Codec();
        bean.getBornFriend().setClazz(Exception.class);
        return bean;
    }

    String name;

    MockBean4Codec friend;

    MockBean4CodecPrimitives bornFriend = new MockBean4CodecPrimitives();

    MockBean4Codec backRef;

    public MockBean4Codec() {
        super();
    }

    /**
     * @return Returns the friend.
     */
    public MockBean4Codec getFriend() {
        return friend;
    }

    /**
     * @param friend
     *            The friend to set.
     */
    public void setFriend(MockBean4Codec friend) {
        this.friend = friend;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the backRef.
     */
    public MockBean4Codec getBackRef() {
        return backRef;
    }

    /**
     * @param backRef
     *            The backRef to set.
     */
    public void setBackRef(MockBean4Codec backRef) {
        this.backRef = backRef;
    }

    /**
     * @return Returns the bornFriend.
     */
    public MockBean4CodecPrimitives getBornFriend() {
        return bornFriend;
    }

    /**
     * @param bornFriend
     *            The bornFriend to set.
     */
    public void setBornFriend(MockBean4CodecPrimitives bornFriend) {
        this.bornFriend = bornFriend;
    }
}
