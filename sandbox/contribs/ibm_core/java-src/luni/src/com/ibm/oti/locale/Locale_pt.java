/* Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.ibm.oti.locale;
public class Locale_pt extends java.util.ListResourceBundle {
protected Object[][] getContents() {
	Object[][] contents = {
		{"First_Day",new java.lang.Integer(2),},
		{"months",new String[]{"janeiro","fevereiro","mar\u00e7o","abril","maio","junho","julho","agosto","setembro","outubro","novembro","dezembro","",},
},
		{"shortMonths",new String[]{"jan","fev","mar","abr","mai","jun","jul","ago","set","out","nov","dez","",},
},
		{"weekdays",new String[]{"","domingo","segunda-feira","ter\u00e7a-feira","quarta-feira","quinta-feira","sexta-feira","s\u00e1bado",},
},
		{"shortWeekdays",new String[]{"","dom","seg","ter","qua","qui","sex","s\u00e1b",},
},
		{"Date_SHORT","dd-MM-yyyy",},
		{"Date_MEDIUM","d/MMM/yyyy",},
		{"Date_LONG","d' de 'MMMM' de 'yyyy",},
		{"Date_FULL","EEEE, d' de 'MMMM' de 'yyyy",},
		{"Time_SHORT","H:mm",},
		{"Time_MEDIUM","H:mm:ss",},
		{"Time_LONG","H:mm:ss z",},
		{"Time_FULL","HH'H'mm'm' z",},
		{"DecimalPatternChars","0#,.;%\u2030E,-",},
	};
return contents;
}
}
