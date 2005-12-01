/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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
public class Locale_ro extends java.util.ListResourceBundle {
protected Object[][] getContents() {
	Object[][] contents = {
		{"LocalPatternChars","GanjkHmsSEDFwWxhKzZ",},
		{"eras",new String[]{"d.C.","\u00ee.d.C.",},
},
		{"months",new String[]{"ianuarie","februarie","martie","aprilie","mai","iunie","iulie","august","septembrie","octombrie","noiembrie","decembrie","",},
},
		{"shortMonths",new String[]{"Ian","Feb","Mar","Apr","Mai","Iun","Iul","Aug","Sep","Oct","Nov","Dec","",},
},
		{"weekdays",new String[]{"","duminic\u0103","luni","mar\u0163i","miercuri","joi","vineri","s\u00eemb\u0103t\u0103",},
},
		{"shortWeekdays",new String[]{"","D","L","Ma","Mi","J","V","S",},
},
		{"Date_SHORT","dd.MM.yyyy",},
		{"Date_MEDIUM","dd.MM.yyyy",},
		{"Date_LONG","dd MMMM yyyy",},
		{"Date_FULL","dd MMMM yyyy",},
		{"Time_SHORT","HH:mm",},
		{"Time_MEDIUM","HH:mm:ss",},
		{"Time_LONG","HH:mm:ss z",},
		{"Time_FULL","HH:mm:ss z",},
		{"DecimalPatternChars","0#,.;%\u2030E,-",},
	};
return contents;
}
}
