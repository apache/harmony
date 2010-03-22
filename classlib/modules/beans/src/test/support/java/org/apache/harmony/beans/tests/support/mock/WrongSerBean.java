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

import java.io.Serializable;

/**
 * A bean class which .ser file is wrong.
 * Used for BeansTest.test_instantiate_withWrongSer()
 */
public class WrongSerBean implements Serializable{
	private int intField;
	private char charField;
	private String str;
	
	public WrongSerBean(){
		super();
	}
	
	public WrongSerBean(int intField, char charField){
		this.intField = intField;
		this.charField = charField;
		this.str = "test";
	}

	public char getCharField() {
		return charField;
	}

	public void setCharField(char charField) {
		this.charField = charField;
	}

	public int getIntField() {
		return intField;
	}

	public void setIntField(int intField) {
		this.intField = intField;
	}
	
	
}
