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
/**
 * @author Hugo Beilis
 * @author Leonardo Soler
 * @author Gabriel Miretti
 * @version 1.0
 */
package org.apache.harmony.jndi.tests.javax.naming.ldap.extras;

import javax.naming.InvalidNameException;
import javax.naming.ldap.Rdn;
import junit.framework.TestCase;

/**
 * <p>Test case to test the class Rdn when it receives an UTF-8 string that happens when is escaped with the 
 * blackslash ('\' ASCII 92).</p>
 *
 */
/**
 * <p>Test method for 'javax.naming.ldap.Rdn.Rdn(String)'</p>
 * <p>Here we are testing combinations of UTF8</p>
 * <p>The expected result for tests between 001 to 005 is an instance of Rdn, in the case of
 * 006 should raise an exception.</p>
 * 
 *  Decimal   Octal   Hex    Binary     Value
   -------   -----   ---    ------     -----
     000      000    000   00000000      NUL    (Null char.)
     001      001    001   00000001      SOH    (Start of Header)
     002      002    002   00000010      STX    (Start of Text)
     003      003    003   00000011      ETX    (End of Text)
     004      004    004   00000100      EOT    (End of Transmission)
     005      005    005   00000101      ENQ    (Enquiry)
     006      006    006   00000110      ACK    (Acknowledgment)
     007      007    007   00000111      BEL    (Bell)
     008      010    008   00001000       BS    (Backspace)
     009      011    009   00001001       HT    (Horizontal Tab)
     010      012    00A   00001010       LF    (Line Feed)
     011      013    00B   00001011       VT    (Vertical Tab)
     012      014    00C   00001100       FF    (Form Feed)
     013      015    00D   00001101       CR    (Carriage Return)
     014      016    00E   00001110       SO    (Shift Out)
     015      017    00F   00001111       SI    (Shift In)
     016      020    010   00010000      DLE    (Data Link Escape)
     017      021    011   00010001      DC1 (XON) (Device Control 1)
     018      022    012   00010010      DC2       (Device Control 2)
     019      023    013   00010011      DC3 (XOFF)(Device Control 3)
     020      024    014   00010100      DC4       (Device Control 4)
     021      025    015   00010101      NAK    (Negative Acknowledgement)
     022      026    016   00010110      SYN    (Synchronous Idle)
     023      027    017   00010111      ETB    (End of Trans. Block)
     024      030    018   00011000      CAN    (Cancel)
     025      031    019   00011001       EM    (End of Medium)
     026      032    01A   00011010      SUB    (Substitute)
     027      033    01B   00011011      ESC    (Escape)
     028      034    01C   00011100       FS    (File Separator)
     029      035    01D   00011101       GS    (Group Separator)
     030      036    01E   00011110       RS    (Request to Send)(Record Separator)
     031      037    01F   00011111       US    (Unit Separator)
     032      040    020   00100000       SP    (Space)
     033      041    021   00100001        !    (exclamation mark)
     034      042    022   00100010        "    (double quote)
     035      043    023   00100011        #    (number sign)
     036      044    024   00100100        $    (dollar sign)
     037      045    025   00100101        %    (percent)
     038      046    026   00100110        &    (ampersand)
     039      047    027   00100111        '    (single quote)
     040      050    028   00101000        (    (left/opening parenthesis)
     041      051    029   00101001        )    (right/closing parenthesis)
     042      052    02A   00101010        *    (asterisk)
     043      053    02B   00101011        +    (plus)
     044      054    02C   00101100        ,    (comma)
     045      055    02D   00101101        -    (minus or dash)
     046      056    02E   00101110        .    (dot)
     047      057    02F   00101111        /    (forward slash)
     048      060    030   00110000        0
     049      061    031   00110001        1
     050      062    032   00110010        2
     051      063    033   00110011        3
     052      064    034   00110100        4
     053      065    035   00110101        5
     054      066    036   00110110        6
     055      067    037   00110111        7
     056      070    038   00111000        8
     057      071    039   00111001        9
     058      072    03A   00111010        :    (colon)
     059      073    03B   00111011        ;    (semi-colon)
     060      074    03C   00111100        <    (less than)
     061      075    03D   00111101        =    (equal sign)
     062      076    03E   00111110        >    (greater than)
     063      077    03F   00111111        ?    (question mark)
     064      100    040   01000000        @    (AT symbol)
     065      101    041   01000001        A
     066      102    042   01000010        B
     067      103    043   01000011        C
     068      104    044   01000100        D
     069      105    045   01000101        E
     070      106    046   01000110        F
     071      107    047   01000111        G
     072      110    048   01001000        H
     073      111    049   01001001        I
     074      112    04A   01001010        J
     075      113    04B   01001011        K
     076      114    04C   01001100        L
     077      115    04D   01001101        M
     078      116    04E   01001110        N
     079      117    04F   01001111        O
     080      120    050   01010000        P
     081      121    051   01010001        Q
     082      122    052   01010010        R
     083      123    053   01010011        S
     084      124    054   01010100        T
     085      125    055   01010101        U
     086      126    056   01010110        V
     087      127    057   01010111        W
     088      130    058   01011000        X
     089      131    059   01011001        Y
     090      132    05A   01011010        Z
     091      133    05B   01011011        [    (left/opening bracket)
     092      134    05C   01011100        \    (back slash)
     093      135    05D   01011101        ]    (right/closing bracket)
     094      136    05E   01011110        ^    (caret/cirumflex)
     095      137    05F   01011111        _    (underscore)
     096      140    060   01100000        `
     097      141    061   01100001        a
     098      142    062   01100010        b
     099      143    063   01100011        c
     100      144    064   01100100        d
     101      145    065   01100101        e
     102      146    066   01100110        f
     103      147    067   01100111        g
     104      150    068   01101000        h
     105      151    069   01101001        i
     106      152    06A   01101010        j
     107      153    06B   01101011        k
     108      154    06C   01101100        l
     109      155    06D   01101101        m
     110      156    06E   01101110        n
     111      157    06F   01101111        o
     112      160    070   01110000        p
     113      161    071   01110001        q
     114      162    072   01110010        r
     115      163    073   01110011        s
     116      164    074   01110100        t
     117      165    075   01110101        u
     118      166    076   01110110        v
     119      167    077   01110111        w
     120      170    078   01111000        x
     121      171    079   01111001        y
     122      172    07A   01111010        z
     123      173    07B   01111011        {    (left/opening brace)
     124      174    07C   01111100        |    (vertical bar)
     125      175    07D   01111101        }    (right/closing brace)
     126      176    07E   01111110        ~    (tilde)
     127      177    07F   01111111      DEL    (delete)
 */
public class TestRdnParser extends TestCase {
	
		    //All valid ASCII imprimibles
			private static String[] utf8Values003 = new String[]{"0","1","2","3","4","5","6","7","8","9","!","$","%","&",
				"'","(",")","*","-",".","/",":","=","?","@","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O",
				"P","Q","R","S","T","U","V","W","X","Y","Z","[","]","^","_","`","a","b","c","d","e","f","g","h","i","j","k","l","m","n",
				"o","p","q","r","s","t","u","v","w","x","y","z","{","|","}","~","<",">"};
			
			 //All valid ASCII imprimibles without "<",">","="
			private static String[] utf8Values009 = new String[]{"0","1","2","3","4","5","6","7","8","9","!","$","%","&",
				"'","(",")","*","-",".","/",":","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O",
				"P","Q","R","S","T","U","V","W","X","Y","Z","[","]","^","_","`","a","b","c","d","e","f","g","h","i","j","k","l","m","n",
				"o","p","q","r","s","t","u","v","w","x","y","z","{","|","}","~","?","@"};
		
            //All Hex ASCII 
			private static String[] utf8Values004 = new String[]{"\\00","\\01","\\02","\\03","\\04","\\05","\\06","\\07","\\08","\\09","\\0a","\\0b",
				"\\0c","\\0d","\\0e","\\0f","\\10","\\11","\\12","\\13","\\14","\\15","\\16","\\17","\\18","\\19","\\1a","\\1b","\\1c","\\1d","\\1e",
				"\\1f","\\20","\\21","\\24","\\25","\\26","\\27","\\28","\\29","\\2a","\\2d","\\2e","\\2f","\\31","\\32",
				"\\33","\\34","\\35","\\36","\\37","\\38","\\39","\\3a","\\3f","\\41","\\42","\\43","\\44","\\45","\\46",
				"\\47","\\48","\\49","\\4a","\\4b","\\4c","\\4d","\\4e","\\4f","\\51","\\52","\\53","\\54","\\55","\\56","\\57","\\58","\\59","\\5a",
				"\\5b","\\5d","\\5e","\\5f","\\61","\\62","\\63","\\64","\\65","\\66","\\67","\\68","\\69","\\6a","\\6b","\\6c","\\6d","\\6e",
				"\\6f","\\71","\\72","\\73","\\74","\\75","\\76","\\77","\\78","\\79","\\7a","\\7b","\\7c","\\7d","\\7e","\\22","\\23","\\2b","\\2c",
				"\\3c","\\3d","\\3e","\\3b","\\5c"};
	
            //Not Valid types in ASCII
			private static String[] utf8Values006 = new String[]{"!","$","%","&","'","(",")","*","/",":","<","=",">","?","@","[","]","^","_","`","{","|","}","~"};
			
			//Valid types in ASCII
			private static String[] utf8Values007 = new String[]{"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","a","b","c","d","e","f","g","h","i","j"
				,"k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","-","."};
	
			//Special characters in ASCII
			private static String[] utf8Values005 = new String[]{"\"","#","+",",","\\",";"};
			
			//Special characters up to 7e
			private static String[] utf8Values008 = new String[]{"\\7f","\\80","\\81","\\82","\\83","\\84","\\85","\\86","\\87","\\88","\\89","\\8a",
				"\\8b","\\8c","\\8d","\\8e","\\8f","\\90","\\91","\\92","\\93","\\94","\\95","\\96","\\97","\\98","\\99","\\9a","\\9b","\\9c","\\9d",
				"\\9e","\\9f"};
			
			//Model Value
			private static String value="model";

			//Model Type
			private static String type="type=";
		
	        //Non PrintTable Ascii Data Test Ramdom Combinations
			private static String[] utf8Values001=new String[]{
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\01\\02\\03\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\02\\03\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\03\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\0b\\0c\\0d\\0e\\0f",
				"t=\\0c\\0d\\0e\\0f",
				"t=\\0d\\0e\\0f",
				"t=\\0e\\0f",
				"t=\\0f",
				"t=\\00",
				"t=\\00\\01",
				"t=\\00\\01\\02",
				"t=\\00\\01\\02\\03",
				"t=\\00\\01\\02\\03\\04",
				"t=\\00\\01\\02\\03\\04\\05",
				"t=\\00\\01\\02\\03\\04\\05\\06",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08\\09",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08\\09\\0a",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08\\09\\0a\\0b",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e",
				"t=\\00\\01\\02\\03\\04\\05\\06\\07\\08\\09\\0a\\0b\\0c\\0d\\0e\\0f",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18\\19\\1a\\1b\\1c\\1d\\1e\\1f\\20",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18\\19\\1a\\1b\\1c\\1d\\1e\\1f",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18\\19\\1a\\1b\\1c\\1d\\1e",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18\\19\\1a\\1b\\1c\\1d",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18\\19\\1a\\1b\\1c",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18\\19\\1a\\1b",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18\\19\\1a",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18\\19",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17\\18",
				"t=\\10\\11\\12\\13\\14\\15\\16\\17",
				"t=\\10\\11\\12\\13\\14\\15\\16",
				"t=\\10\\11\\12\\13\\14\\15",
				"t=\\10\\11\\12\\13\\14",
				"t=\\10\\11\\12\\13",
				"t=\\10\\11\\12",
				"t=\\10\\11",
				"t=\\10",
				"t=\\7f"};
				
			//PrinTable UTF8 Data Test Ramdom Combinations
			private static String[] utf8Values002=new String[]{
				"t=\\31\\32\\33\\34\\35\\36\\37\\38\\39\\3a\\3b\\3c\\3d\\3e\\3f",
				"t=\\41\\42\\43\\44\\45\\46\\47\\48\\49\\4a\\4b\\4c\\4d\\4e\\4f",
				"t=\\51\\52\\53\\54\\55\\56\\57\\58\\59\\5a\\5b\\5c\\5d\\5e\\5f",
				"t=\\61\\62\\63\\64\\65\\66\\67\\68\\69\\6a\\6b\\6c\\6d\\6e\\6f",
				"t=\\71\\72\\73\\74\\75\\76\\77\\78\\79\\7a\\7b\\7c\\7d\\7e",
				"t=\\21","t=\\22","t=\\23","t=\\24","t=\\25","t=\\26","t=\\27",
				"t=\\28","t=\\29","t=\\2a","t=\\2b","t=\\2c","t=\\2d","t=\\2e","t=\\2f"};
		
		
	public static void main(String[] args) {
	}

	public TestRdnParser(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * <p>Here we are testing if the constructor can create an instance of the rdn with different combinations of utf8 not printables.</p>
	 *
	 */
	public void testRdnString001() {

		try {
			for(int i=0;i<utf8Values001.length ;i++){
				Rdn x=new Rdn(utf8Values001[i]);
				assertNotNull(x);
			}
			
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
	}

	/**
	 * <p>Here we are testing if the constructor can create an instance of the rdn with different combinations of utf8 printables.</p>
	 *
	 */
	public void testRdnString002() {

		
		try {
			for(int i=0;i<utf8Values002.length ;i++){
				Rdn x=new Rdn(utf8Values002[i]);
				assertNotNull(x);
				
			}
			
		} catch (InvalidNameException e) {
			fail("Failed with:"+e);
		}
	}

	/**
	 * <p>Here we are testing if the constructor can create an instance of the rdn with different combinations of utf8 not printables and 
	 * printables. 1 to 1</p>
	 *
	 */
	public void testRdnString003() {
		
		for (int i = 0; i < utf8Values003.length; i++) {
			for (int j = 0; j < utf8Values004.length; j++) {
				try {
					Rdn x=new Rdn(this.type+this.utf8Values003[i]+this.utf8Values004[j]);
					assertNotNull(x);
					Object ret=x.getValue();
					String temp=utf8Values003[i].toString()+utf8Values004[j].toString();
					assertFalse(temp.equals(ret));
				} catch (InvalidNameException e) {
					fail("Failed with:"+e);
				}
				
			}
		}
	}

	/**
	 * <p>Here we are testing if the constructor can create an instance of the rdn with different combinations of utf8 not printables and 
	 * printables. 1 to 1</p>
	 *
	 */	
	public void testRdnString004() {
		
		for (int i = 0; i < utf8Values004.length; i++) {
			for (int j = 0; j < utf8Values003.length; j++) {
				try {
					Rdn y=new Rdn(this.type+this.utf8Values004[i]+this.utf8Values003[j]);
					assertNotNull(y);
					Object ret=y.getValue();
					String temp=utf8Values004[i].toString()+utf8Values003[j].toString();
					assertFalse(temp.equals(ret));
				} catch (InvalidNameException e) {
					fail("Failed with:"+e );
				}
				
			}
		}
	}

	/**
	 * <p>Here we are testing if the constructor can create an instance of the rdn with different combinations of utf8 not printables and 
	 * printables. 1 utf8 to 1 ascii to 1 utf8 and 1 ascii to 1 utf8 to 1 ascii.</p>
	 *
	 */	
	public void testRdnString005() {
		
		for (int i = 0; i < utf8Values004.length; i++) {
			for (int j = 0; j < utf8Values003.length; j++) {
				try {
					Rdn y=new Rdn(this.type+this.utf8Values004[i]+this.utf8Values003[j]+this.utf8Values004[i]);
					assertNotNull(y);
					Object ret=y.getValue();
					String temp=utf8Values004[i].toString()+utf8Values003[j].toString()+utf8Values004[i].toString();
					assertFalse(temp.equals(ret));
				} catch (InvalidNameException e) {
					fail("Failed with:"+e );
				}
				
			}
		}
		for (int i = 0; i < utf8Values003.length; i++) {
			for (int j = 0; j < utf8Values004.length; j++) {
				try {
					Rdn y=new Rdn(this.type+this.utf8Values003[i]+this.utf8Values004[j]+this.utf8Values003[i]);
					assertNotNull(y);
					Object ret=y.getValue();
					String temp=utf8Values003[i].toString()+utf8Values004[j].toString()+utf8Values003[i].toString();
					assertFalse(temp.equals(ret));
				} catch (InvalidNameException e) {
					fail("Failed with:"+e);
				}
				
			}
		}
		for (int i = 0; i < utf8Values003.length; i++) {
			for (int j = 0; j < utf8Values004.length; j++) {
				try {
					Rdn y=new Rdn(this.type+this.utf8Values003[i]+this.utf8Values003[i]+this.utf8Values004[j]);
					assertNotNull(y);
					Object ret=y.getValue();
					String temp=utf8Values003[i].toString()+utf8Values003[i].toString()+utf8Values004[j].toString();
					assertFalse(temp.equals(ret));
				} catch (InvalidNameException e) {
					fail("Failed with:"+e);
				}
				
			}
		}
	}

	/**
	 * <p>Here we are testing if the constructor throw an exception with only a type.</p>
	 *
	 */
	public void testRdnString006() {

		for(int i=0;i<utf8Values003.length ;i++){
			
			try {
			
				Rdn x=new Rdn(utf8Values003[i]);
				fail("This is wrong."+i);
			} catch (InvalidNameException e) {
			
			}
						
		}
		for(int i=0;i<utf8Values004.length ;i++){
			
			try {
			
				Rdn x=new Rdn(utf8Values004[i]);
				fail("This is wrong."+i);
			} catch (InvalidNameException e) {
			
			}
						
		}
		
	}

	/**
	 * <p>Here we are testing if the constructor throw an exception a type in utf8.</p>
	 *
	 */
	public void testRdnString007() {
		
		for(int i=0;i<utf8Values004.length ;i++){
			
			try {
			
				Rdn x=new Rdn(utf8Values004[i]+"=");
				fail("This is wrong."+i);
			} catch (InvalidNameException e) {
			
			}
						
		}
		
	}
	
	/**
	 * <p>Here we are testing if the constructor throw an exception when a type in ascii is send but not valid.</p>
	 *
	 */
	public void testRdnString008() {
		
		for(int i=0;i<utf8Values006.length ;i++){
			
			try {
			
				Rdn x=new Rdn(utf8Values006[i]+"=");
				fail("This is wrong."+i);
			} catch (InvalidNameException e) {
			
			}
						
		}
		
	}
	/**
	 * <p>Here we are testing if the constructor with a type in ascii valid is send.</p>
	 *
	 */
	public void testRdnString009() {
		
		for(int i=0;i<utf8Values007.length ;i++){
			
			try {
			
				Rdn x=new Rdn(utf8Values007[i]+"=");
				assertNotNull(x);
				
			} catch (InvalidNameException e) {
				fail("This is wrong."+e);
			}
						
		}
		
	}
	
	/**
	 * <p>Here we are testing if the constructor receives in the value an utf8 value.</p>
	 *
	 */
	public void testRdnString010(){
		for(int i=0;i<utf8Values004.length;i++){
			try {
				Rdn x=new Rdn(this.type+utf8Values004[i]);
				assertNotNull(x);
				Object ret=x.getValue();
				String temp=utf8Values004[i].toString();
				assertFalse(temp.equals(ret));
			} catch (InvalidNameException e) {
				fail("Failed with:"+e);
			}
		}
	}
	
	/**
	 * <p>Here we are testing if the constructor receives in the value utf8 values.</p>
	 *
	 */
	public void testRdnString011(){
		for(int i=0;i<utf8Values004.length;i++){
			try {
				Rdn x=new Rdn(type+utf8Values004[i]+utf8Values004[i]);
				assertNotNull(x);
				Object ret=x.getValue();
				String temp=utf8Values004[i].toString()+utf8Values004[i].toString();
				assertFalse(temp.equals(ret));
			} catch (InvalidNameException e) {
				fail("Failed with:"+e);
			}
		}
		for(int i=0;i<utf8Values004.length;i++){
			try {
				Rdn x=new Rdn(type+utf8Values004[i]+utf8Values004[i]+utf8Values004[i]);
				assertNotNull(x);
				Object ret=x.getValue();
				String temp=utf8Values004[i].toString()+utf8Values004[i].toString();
				assertFalse(temp.equals(ret));
			} catch (InvalidNameException e) {
				fail("Failed with:"+e);
			}
		}
	}
	
	/**
	 * <p>Here we are testing if the constructor receives in the value an ascii value.</p>
	 *
	 */
	public void testRdnString012(){
		for(int i=0;i<utf8Values003.length;i++){
			try {
				Rdn x=new Rdn(this.type+utf8Values003[i]);
				assertNotNull(x);
				Object ret=x.getValue();
				String temp=utf8Values003[i].toString();
				assertTrue(temp.equals(ret));
			} catch (InvalidNameException e) {
				fail("Failed with:"+e);
			}
		}
	}
	
	/**
	 * <p>Here we are testing if the constructor receives in the value ascii values.</p>
	 *
	 */
	public void testRdnString013(){
		for(int i=0;i<utf8Values003.length;i++){
			try {
				Rdn x=new Rdn(type+utf8Values003[i]+utf8Values003[i]);
				assertNotNull(x);
				Object ret=x.getValue();
				String temp=utf8Values003[i].toString()+utf8Values003[i].toString();
				assertTrue(temp.equals(ret));
			} catch (InvalidNameException e) {
				fail("Failed with:"+e);
			}
		}
		for(int i=0;i<utf8Values003.length;i++){
			try {
				Rdn x=new Rdn(type+utf8Values003[i]+utf8Values003[i]+utf8Values003[i]);
				assertNotNull(x);
				Object ret=x.getValue();
				String temp=utf8Values003[i].toString()+utf8Values003[i].toString()+utf8Values003[i].toString();
				assertTrue(temp.equals(ret));
			} catch (InvalidNameException e) {
				fail("Failed with:"+e);
			}
		}
	}
	
	
	/**
	 * <p>Here we are testing if the constructor receives an especial character in the type.</p>
	 *
	 */
	public void testRdnString014() {
		
		for(int i=0;i<utf8Values005.length ;i++){
			
			try {
			
				Rdn x=new Rdn(utf8Values005[i]+"=");
				fail("This is wrong.");
			} catch (InvalidNameException e) {
				
			}
						
		}
	}
	
	/**
	 * <p>Here we are testing if the constructor receives an especial character in the value.</p>
	 *
	 */
	public void testRdnString015() {
		
		for(int i=0;i<utf8Values005.length ;i++){
			
			try {
				
				Rdn x=new Rdn(type+utf8Values005[i]);
				if(utf8Values005[i]=="+"||utf8Values005[i]=="#"){}
				else{fail("This is wrong."+ utf8Values005[i]);}
			
			} catch (InvalidNameException e) {
				
			}
						
		}
	}
	
	/**
	 * <p>Here we are testing if the constructor can receive an utf8 up to 7e</p>
	 *
	 */
	public void testRdnString016(){
		try{
			for (int i = 0; i < utf8Values008.length; i++) {
				Rdn x=new Rdn(type+utf8Values008[i]);
				assertNotNull(x);
								
			}
						
		}catch (InvalidNameException e) {
			fail();
		}
	}
	
	/**
	 * <p>Here we are testing if the constructor can receive a multivalue.</p> 
	 *
	 */
	public void testRdnString017(){
		try{
			for (int i = 0; i < utf8Values003.length; i++) {
				Rdn x=new Rdn(type+utf8Values003[i]+"+"+type+utf8Values003[i]+"+"+type+utf8Values003[i]);
				assertNotNull(x);		
			}
			for (int i = 0; i < utf8Values003.length; i++) {
				for (int j = 0; j < utf8Values004.length; j++) {
					Rdn x=new Rdn(type+utf8Values003[i]+"+"+type+utf8Values004[j]+"+"+type+utf8Values003[i]);
					assertNotNull(x);
				}
						
			}
									
		}catch (InvalidNameException e) {
			fail();
		}
		try{
			
			for (int i = 0; i < utf8Values003.length; i++) {
				for (int j = 0; j < utf8Values004.length; j++) {
					for (int k = 0; k < utf8Values006.length; k++) {
						Rdn x=new Rdn(utf8Values005[0]+utf8Values006[k]+"="+utf8Values003[i]+"+"+type+utf8Values004[j]+"+"+type+utf8Values003[i]);
						fail();
					}
					
				}
						
			}
			for (int i = 0; i < utf8Values003.length; i++) {
				for (int j = 0; j < utf8Values004.length; j++) {
					for (int k = 0; k < utf8Values006.length; k++) {
						Rdn x=new Rdn(type+utf8Values003[i]+"+"+utf8Values005[0]+utf8Values006[k]+"="+utf8Values004[j]+"+"+type+utf8Values003[i]);
						fail();
					}
					
				}
						
			}
			for (int i = 0; i < utf8Values003.length; i++) {
				for (int j = 0; j < utf8Values004.length; j++) {
					for (int k = 0; k < utf8Values006.length; k++) {
						Rdn x=new Rdn(type+utf8Values003[i]+"+"+utf8Values004[j]+"="+utf8Values004[j]+"+"+type+utf8Values003[i]);
						fail();
					}
					
				}
						
			}
						
		}catch (InvalidNameException e) {
			
		}
		
	}
	
	
	/**
	 * <p>Here we are testing if the constructor can receive a multivalue between '"'.</p> 
	 *
	 */
	public void testRdnString018(){
		try{
			for (int i = 0; i < utf8Values009.length; i++) {
				Rdn x=new Rdn(type+"\""+utf8Values009[i]+"\""+"+"+type+utf8Values009[i]+"+"+type+utf8Values009[i]);
				assertNotNull(x);
				String temp=type+utf8Values009[i]+"+"+type+utf8Values009[i]+"+"+type+utf8Values009[i];
				assertEquals(temp,x.toString());
			}
			for (int j = 0; j < utf8Values004.length; j++) {
				Rdn x=new Rdn(type+"\""+utf8Values004[j]+"\"");
				assertNotNull(x);
				String temp=null;
				if(j<(utf8Values004.length-9)){
					
					temp=type+Rdn.unescapeValue(utf8Values004[j]);
				}else{
					temp=type+"\\"+Rdn.unescapeValue(utf8Values004[j]);
					
				}
				
				assertEquals(0,temp.compareToIgnoreCase(x.toString()));
									
			}
			
		}catch (InvalidNameException e) {
			fail();
		}
	}
	
	/**
	 * <p>Here we are testing if the constructor can receive a multivalue between '"'.</p> 
	 *
	 */
	public void testRdnString019(){
		try{
			
			for (int i = 0; i < utf8Values009.length; i++) {
				for (int j = 1; j < utf8Values004.length; j++) {//starts in one because that will be test in the next method.
								
					Rdn x=new Rdn(type+"\""+utf8Values009[i]+"\""+"+"+type+"\""+utf8Values004[j]+"\""+"+"+type+utf8Values009[i]);
			
					assertNotNull(x);
					String temp=null;
					if(j<(utf8Values004.length-9)){
					
						if(((String)Rdn.unescapeValue(utf8Values004[j])).compareToIgnoreCase(utf8Values009[i])>0){
							temp=type+utf8Values009[i]+"+"+type+utf8Values009[i]+"+"+type+Rdn.unescapeValue(utf8Values004[j]);
			
							if((j!=82&&j!=83&&j<109)&&((j>=55&j<=80|j>=84&j<=108)&&utf8Values009[i]=="[")|(i==49&utf8Values009[i]=="]")){
								temp=type+Rdn.unescapeValue(utf8Values004[j])+"+"+type+utf8Values009[i]+"+"+type+utf8Values009[i];
											
							}

							if((i==50|i==51|i==52)&((j>=55&j<83)|(j>=84&j<109))){
								temp=type+Rdn.unescapeValue(utf8Values004[j])+"+"+type+utf8Values009[i]+"+"+type+utf8Values009[i];
			
							}
						}else{
							temp=type+Rdn.unescapeValue(utf8Values004[j])+"+"+type+utf8Values009[i]+"+"+type+utf8Values009[i];
			
							if((i>=22|i<=59)&&(utf8Values004[j]=="\\5b"|utf8Values004[j]=="\\5d"|utf8Values004[j]=="\\5e"
								|utf8Values004[j]=="\\5f")&&/*(i!=49&(utf8Values009[i]!="]"|utf8Values009[i]!="["))&&*/(i<49|i>52)&&(i<79)){
								temp=type+utf8Values009[i]+"+"+type+utf8Values009[i]+"+"+type+Rdn.unescapeValue(utf8Values004[j]);
			
								
							}
						}
						if(((String)Rdn.unescapeValue(utf8Values004[j])).compareToIgnoreCase(utf8Values009[i])==0){
							temp=type+utf8Values009[i]+"+"+type+Rdn.unescapeValue(utf8Values004[j])+"+"+type+utf8Values009[i];
	
						}

											
					}else{
						
						
						if(((String)Rdn.unescapeValue(utf8Values004[j])).compareToIgnoreCase(utf8Values009[i])<0){
							temp=type+utf8Values009[i]+"+"+type+utf8Values009[i]+"+"+type+"\\"+Rdn.unescapeValue(utf8Values004[j]);
	
						}
						
						if(utf8Values004[j]=="\\22"|utf8Values004[j]=="\\23"|utf8Values004[j]=="\\2b"|utf8Values004[j]=="\\2c"
						|utf8Values004[j]=="\\3c"|utf8Values004[j]=="\\3d"|utf8Values004[j]=="\\3e"|utf8Values004[j]=="\\3b"
							|utf8Values004[j]=="\\5c"){
							temp=type+"\\"+Rdn.unescapeValue(utf8Values004[j])+"+"+type+utf8Values009[i]+"+"+type+utf8Values009[i];
	
							
														
							if(utf8Values009[i]=="!"|utf8Values009[i]=="$"|utf8Values009[i]=="$"|utf8Values009[i]=="%"|
								utf8Values009[i]=="%"|utf8Values009[i]=="&"|utf8Values009[i]=="'"|utf8Values009[i]=="("|utf8Values009[i]==")"
									|utf8Values009[i]=="*"|utf8Values009[i]=="["
									&&utf8Values004[j]=="\\2b"|utf8Values004[j]=="\\2c"&&i!=48){
								temp=type+utf8Values009[i]+"+"+type+utf8Values009[i]+"+"+type+"\\"+Rdn.unescapeValue(utf8Values004[j]);
	
							}
							
							if(utf8Values009[i]=="!"|(i>=22|i<=59&&utf8Values004[j]=="\\5c")&i!=49&i!=50&i!=51&i!=52&i!=79&i!=80&i!=81&i!=82){
									temp=type+utf8Values009[i]+"+"+type+utf8Values009[i]+"+"+type+"\\"+Rdn.unescapeValue(utf8Values004[j]);
	
								}
						}
						
						if(utf8Values004[j]=="\\3c"|utf8Values004[j]=="\\3d"|utf8Values004[j]=="\\3e"|utf8Values004[j]=="\\3b"
								|utf8Values004[j]=="\\5c"&&i<22){
								temp=type+utf8Values009[i]+"+"+type+utf8Values009[i]+"+"+type+"\\"+Rdn.unescapeValue(utf8Values004[j]);
	
						}

					}
					assertEquals(0,temp.compareTo(x.toString()));
				}
						
			}
	
		}catch (InvalidNameException e) {
			fail();
		}
	}
	
	/**
	 * <p>Here we are testing if the constructor receives combinations of bytes arrays.</p>
	 *
	 */
	public void testBytes(){
		try{
			for (int i = 0; i < 16; i++) {
				for (int j = 0; j < 16; j++) {
							
					String[] hex={"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
					Rdn x=new Rdn("t=#"+hex[i]+hex[j]);
					assertNotNull(x);
				}
			}
									
		}catch (InvalidNameException e) {
			fail();
		}catch (IllegalArgumentException e) {
			fail();
		}
		
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 16; k++) {
					try{
				
						String[] hex={"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
						Rdn x=new Rdn("t=#"+hex[i]+hex[j]+hex[k]);
						fail();
					}catch (InvalidNameException e) {
						
					}catch (IllegalArgumentException e) {
						
					}
				}
			}
		}
		try{
			for (int i = 0; i < 16; i++) {
				for (int j = 0; j < 16; j++) {
					for (int k = 0; k < 16; k++) {
						for (int f = 0; f < 16; f++) {
							
						
							
							String[] hex={"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
							Rdn x=new Rdn("t=#"+hex[i]+hex[j]+hex[k]+hex[f]);
							assertNotNull(x);
						}
					}
				}
			}
									
		}catch (InvalidNameException e) {
			fail();
		}catch (IllegalArgumentException e) {
			fail();
		}						
		
		
	}
	
	/**
	 * <p>Test method to test if the null character in utf8 where is put when the types are not equals between them.</p>
	 *
	 */
	public void testRDNNULL() throws Exception {
		try {
			String y="ca=nine+type="+Rdn.escapeValue("\\00");
			Rdn x=new Rdn("ca=nine+type=\"liom,\"+type=\"\\00\"");
			byte[] temp=new byte[]{99,97,61,110,105,110,101,43,116,121,112,101,61,0,43,116,121,112,101,61,108,105,111,109,92,44};
			for(int i=0;i<26;i++){
				assertEquals(temp[i],x.toString().getBytes("UTF-8")[i]);
			}
			
			x=new Rdn("ca=nine+pe=\"liom,\"+type=\"\\00\"");
			assertEquals("ca=nine+pe=liom\\,+type="+Rdn.unescapeValue("\\00"),x.toString());
		} catch (InvalidNameException e) {
			fail();
			
		}
		
	}
	
		                  
}
