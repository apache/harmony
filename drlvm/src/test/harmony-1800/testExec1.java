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

import junit.framework.TestCase;
import java.io.*;
public class testExec1 extends TestCase {
	    
	    public void testExec() {
		            String [] cmdL = new String[5];
			            cmdL[0] = System.getProperty("java.home")+File.separator+"bin"+File.separator+"java";
				            cmdL[1] = "-classpath";
					            cmdL[2] = ".";
						            cmdL[3] = "testExec1_App";
							            cmdL[4] = null;
								            try {
										                Process p = Runtime.getRuntime().exec(cmdL);
												            p.waitFor();
													                int ans = p.exitValue();
															            InputStream is = p.getErrorStream();
																                int toRead = is.available();
																		            byte[] out = new byte[100];
																			                int sz = 0;
																					            while (true) {
																							                    int r = is.read();
																									                    if (r == -1) {
																												                        break;
																															                }
																											                    out[sz] = (byte)r;
																													                    sz++;
																															                    if (sz == 100) {
																																		                        break;
																																					                }
																																	                }
																						                System.out.println("========Application error message======");
																								            System.out.println(new String (out, 0, sz));
																									                System.out.println("=======================================");

																											            fail("NullPointerException was not thrown. exitValue = " + ans);
																												            } catch (NullPointerException e) {
																														            } catch (Exception e) {
																																                e.printStackTrace();
																																		            fail("Unexpected exception was thrown: " + e);
																																			            }
									         }
}

