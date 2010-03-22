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


options {

STATIC = false;

}


PARSER_BEGIN(FilterParser)
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

package org.apache.harmony.jndi.provider.ldap.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.io.StringReader;
import org.apache.harmony.jndi.provider.ldap.Filter;
import org.apache.harmony.jndi.internal.parser.AttributeTypeAndValuePair;
import org.apache.harmony.jndi.provider.ldap.Filter.MatchingRuleAssertion;
import org.apache.harmony.jndi.provider.ldap.Filter.SubstringFilter;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;


/**
 * Ldap filter parser which parse the string representation of Ldap filters to
 * instance of org.apache.harmony.jndi.provider.ldap.filter.Filter according
 * RFC2254. And It also support parse variables of the form {i}.
 * 
 * @see org.apache.harmony.jndi.provider.ldap.filter.Filter
 * @see javax.naming.directory.DirContext#search(javax.naming.Name, String,
 *      Object[], javax.naming.directory.SearchControls)
 */
public class FilterParser {

  public static void main(String args[]) throws ParseException, FileNotFoundException {
    FilterParser parser = new FilterParser(new FileInputStream("parser.filter.test"));
   //  FilterParser parser = new FilterParser(System.in);
    //System.out.println(parser.value());
      parser.test();
  }

    private Object[] args;

    private boolean isEndEOF = false;

    public FilterParser(String filter) {
        this(new StringReader(filter));
        isEndEOF = true;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    private String getArgAttrValue(String index) {
        int i = Integer.valueOf(index).intValue();
        if (args == null || args.length <= i || args[i] == null) {
            throw new IllegalArgumentException();
        }

        return (String) args[i];
    }

    private String getArgValue(String index) {
        int i = Integer.valueOf(index).intValue();
        if (args == null || args.length <= i || args[i] == null) {
            throw new IllegalArgumentException();
        }
        if (args[i] instanceof String) {
            return (String) args[i];
        }

        //FIXME:
        throw new RuntimeException("Convert value to corresponding java type.");
    }

    private String convertToUtf8Char(String s) {
        byte[] bs = new byte[] { (byte)Integer.parseInt(s, 16) };
        return Utils.getString(bs);
    }

    private Filter parseSubstring(String des, List list) {
        Filter filter = null;
        if (list.size() == 1) {
            if (list.get(0).equals("*")) {
                filter = new Filter(Filter.PRESENT_FILTER);
                filter.setValue(des);
            } else {
                filter = new Filter(Filter.EQUALITY_MATCH_FILTER);
                filter
                        .setValue(new AttributeTypeAndValuePair(des, list
                                .get(0)));
            }
        } else {
            String initial = null;
            String any = "";
            String end = null;
            if (list.get(0).equals("*")) {
                any = "*";
            } else {
                initial = (String) list.get(0);
            }
            
            for (int i = 1; i < list.size(); ++i) {
                String value = (String) list.get(i);
                if (i == list.size() - 1 && !value.equals("*")) {
                    end = value;
                } else {
                    any += value;
                }
            }
            filter = new Filter(Filter.SUBSTRINGS_FILTER);
            SubstringFilter substring = new SubstringFilter(des);
            if (initial != null) {
                substring.addInitial(initial);
            }
            
            substring.addAny(any);
            
            if (end != null) {
                substring.addFinal(end);
            }
            filter.setValue(substring);
        }
        
        return filter;
    }
}

PARSER_END(FilterParser)


TOKEN :
{
<COLON_DN : ":dn" >
|
<HEX_CHAR : ["a"-"f", "A"-"F"] >
|
<NOHEX_CHAR : ["g"-"z", "G" - "Z"] >
|
<LPARENT : "(" >
|
<RPARENT : ")" >
|
<LBRACE : "{" >
|
<RBRACE : "}" >
|
<AND : "&" >
|
<OR : "|" >
|
<NOT : "!" >
|
<ZERO : "0">
|
<COLON : ":">
|
<EQUAL : "=">
|
<LESS : "<">
|
<GREATER : ">">
|
<APPROX : "~">
|
<ASTERISK : "*">
|
<DIGIT : ["1"-"9"]>
|
<HYPHEN : "-">
|
<PERIOD : ".">
|
<BACKSLASH : "\\">
|
<SEMI: ";">
|
<CHAR : ~["*", "\\", "(", ")", "\n", "\u0085"] >
}

String option():
        {
            StringBuilder value = new StringBuilder();
            Token t;
        }

        {
            t = <SEMI> {value.append(t.image);}
            (t = <HEX_CHAR> {value.append(t.image);}
            |t = <NOHEX_CHAR> {value.append(t.image);}
            |t = <ZERO> {value.append(t.image);}
            |t = <DIGIT> {value.append(t.image);}
            |t = <HYPHEN> {value.append(t.image);})+

            {return value.toString();}
        }

String number():
        {
            StringBuilder value = new StringBuilder();
            Token t;
        }
        {
            (t = <ZERO> {value.append(t.image);}
            | (t = <DIGIT> {value.append(t.image);}
               (t = <ZERO> {value.append(t.image);}
               |t = <DIGIT> {value.append(t.image);})*))

            {return value.toString();}
        }


String oid():
        {
            StringBuilder value = new StringBuilder();
            String number = null;
            Token t = null;
        }
        {
            number = number() {value.append(number);}
                (t = <PERIOD> {value.append(t.image);}
                 number = number() {value.append(number);})+

            {return value.toString();}
        }



String matchingrule():
        {
            String value = null;
        }
        {
           (value =  oid() | value = attrType())
           {return value;}
        }

String argument():
        {String num = null;}
        {
            (<LBRACE> num = number() <RBRACE>)

            {return getArgAttrValue(num);}
        }

String attrType():
        {
            StringBuilder value = new StringBuilder();
            String arg;
            Token t;
        }

        {
            (t = <HEX_CHAR> {value.append(t.image);}| t = <NOHEX_CHAR> {value.append(t.image);}| arg = argument() {value.append(arg);})
            ((t = <HEX_CHAR> | t = <NOHEX_CHAR> | t = <ZERO> | t = <DIGIT> | t = <HYPHEN>) {value.append(t.image);} | arg = argument() {value.append(arg);})*
            {return value.toString();}
        }

String attrDescr():
        {String type;}
        {
            (type = attrType() (option())*)
            {return type;}

        }

void test():
        {}
        {
            {
                /*
                 * initial args for test, so there should not be args index larger than
                 * 49 in test case
                 */
                args = new Object[20];
                for (int i = 0; i < args.length; i++) {
                    args[i] = "{" + i + "}";
                }
            }

            parse() ("\n" | "\u0085") test() | LOOKAHEAD(2) ("\n" | "\u0085") | ("\n" | "\u0085") <EOF> | <EOF> 
        }
// FIXME: get string representation of AttributeValue, then use Rdn.unescapeValue(String) to get value
String value():
        {
            StringBuilder value = new StringBuilder();
            String hexValue = null;
            Token t = null;;
            String arg = null;
        }
        {
            // string form of AttributeValue
            ((t = <NOHEX_CHAR> {value.append(t.image);}
            | t = <HEX_CHAR> {value.append(t.image);}
            | t = <ZERO> {value.append(t.image);}
            | t = <DIGIT> {value.append(t.image);}
            | t = <HYPHEN> {value.append(t.image);}
            | t = <COLON> {value.append(t.image);}
            | hexValue = backslashValue() {value.append(hexValue);}
            | arg = argument() {value.append(arg);}
            | t = <CHAR> {value.append(t.image);}))+ 
            
            // TODO: add binary form of AttributeValue

            {return value.toString();}
        }

String backslashValue():
        {String value;}
        {
            // TODO: ",", "+", """, "\", "<", ">", ";", space at beginning, "#" at beginning,
            // space at end prefixed by a backslash
            <BACKSLASH> value = hexDigit() {return convertToUtf8Char(value);}
        }

String hexDigit():
        {
            String value;
            Token t;
        }
        {
            (t = <HEX_CHAR> | t = <DIGIT> | t = <ZERO> ) {value = t.image;}
            (t = <HEX_CHAR> | t = <DIGIT> | t = <ZERO> ) {value += t.image;}

            {return value;}
        }


Filter parse():
        {
            Filter filter = null;
            Filter temp = null;
        }
        {
            <LPARENT>
            (
                <AND>
                {filter = new Filter(Filter.AND_FILTER);}

                temp = parse()
                {filter.addChild(temp);}

                temp = parse()
                {filter.addChild(temp);}

                (temp = parse()
                    {filter.addChild(temp);} 
                )*
                |
                <OR>
                {filter = new Filter(Filter.OR_FILTER);}

                temp = parse()
                {filter.addChild(temp);}

                temp = parse()
                {filter.addChild(temp);}

                (temp = parse()
                    {filter.addChild(temp);} 
                )*
                |
                <NOT>
                {filter = new Filter(Filter.NOT_FILTER);}
                temp = parse()
                {filter.setValue(temp);}
                |
                filter = item()
            )
            <RPARENT>
            {
                return filter;
            }
        }

void eof():
        {}
        {
            <EOF>
        }

Filter item():
        {
            Filter filter = null;
            String value = null;
            String des = null;
            String temp = null;
            List list = new ArrayList();
            SubstringFilter substring = null;
            MatchingRuleAssertion rule = null;
        }

        {
            (des = attrDescr() (
                LOOKAHEAD(2)
                <APPROX> <EQUAL> value = value() 
                {
                    filter = new Filter(Filter.APPROX_MATCH_FILTER); filter.setValue(new AttributeTypeAndValuePair(des, value));
                } 
                | <GREATER> <EQUAL> value = value() 
                {
                    filter = new Filter(Filter.GREATER_OR_EQUAL_FILTER);filter.setValue(new AttributeTypeAndValuePair(des, value));
                } 
                | <LESS> <EQUAL> value = value() 
                {
                    filter = new Filter(Filter.LESS_OR_EQUAL_FILTER);filter.setValue(new AttributeTypeAndValuePair(des, value));
                } 
                 
                | LOOKAHEAD(3) <EQUAL> (asterisk_start(list) | value_start(list)) {filter = parseSubstring(des, list);} 

                | rule = extensible_1(des) {filter = new Filter(Filter.EXTENSIBLE_MATCH_FILTER); filter.setValue(rule);} )
            | rule = extensible_2() {filter = new Filter(Filter.EXTENSIBLE_MATCH_FILTER); filter.setValue(rule);} )
            
            {return filter;}
        }

void asterisk_start(List list):
            {}
            {
                <ASTERISK> {list.add("*");} [value_start(list)]
            }

void value_start(List list):
            {
                String value;
            }
            {
                value = value() {list.add(value);} [asterisk_start(list)]
            }

String final_part():
        {
            String value;
        }
        {
            value = value()

            {return value;}
        }
String tail_part():
        {
            String value;
            String temp;
        }
        {
            value = any_part() [temp = final_part() {value = value + temp;}]
            {return value;}
        }

String any_part():
        {
            StringBuilder value = new StringBuilder();
            Token t;
            String temp;
        }
        {
            t = <ASTERISK> {value.append(t.image);}
            (LOOKAHEAD(2) temp = value() {value.append(temp);}
             t = <ASTERISK> {value.append(t.image);})+

            {return value.toString();}
        }

MatchingRuleAssertion extensible_1(String type):
        {
            MatchingRuleAssertion rule = new MatchingRuleAssertion();
            rule.setType(type);
            String value;
        }
        {
            [LOOKAHEAD(2) <COLON_DN> {rule.setDnAttributes(true);} ] 
            [LOOKAHEAD(2) <COLON> value = oid() {rule.setMatchingRule(value);} ] <COLON>  <EQUAL> 
            value = value() {rule.setMatchValue(value);}
            {return rule;}
        }

MatchingRuleAssertion extensible_2():
        {
            MatchingRuleAssertion rule = new MatchingRuleAssertion();
            String value;
        }
        {
            [LOOKAHEAD(2) <COLON_DN>] <COLON> value = matchingrule() {rule.setMatchingRule(value);} <COLON>  <EQUAL> 
            value = value() {rule.setMatchValue(value);}
            {return rule;}
        }

void colon_oid():
        {}
        {
        <COLON> oid()
        }
