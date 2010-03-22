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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.SearchResult;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.internal.parser.AttributeTypeAndValuePair;
import org.apache.harmony.jndi.provider.ldap.Filter.SubstringFilter;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1ChoiceWrap.ChosenValue;
import org.apache.harmony.jndi.provider.ldap.parser.FilterParser;
import org.apache.harmony.jndi.provider.ldap.parser.ParseException;

public class LdapSchemaFilter {
    private Filter commonFilter;

    public LdapSchemaFilter(String filterValue, Object[] filterArgs)
            throws InvalidSearchFilterException {
        try {
            FilterParser commonFilterParser = new FilterParser(filterValue);
            commonFilterParser.setArgs(filterArgs);
            commonFilter = commonFilterParser.parse();
        } catch (ParseException e) {
            // ldap.29=Invalid search filter
            throw new InvalidSearchFilterException(Messages
                    .getString("ldap.29")); //$NON-NLS-1$
        }
    }

    public HashSet<SearchResult> filter(
            HashSet<SearchResult> results) throws NamingException {
        return doFilter(results, commonFilter);
    }

    private HashSet<SearchResult> doFilter(
            HashSet<SearchResult> currentResults, Filter filter)
            throws NamingException {
        List<Filter> filters;

        HashSet<SearchResult> filteredResults = null;
        HashSet<SearchResult> tempResults;
        Iterator<SearchResult> iterator;
        AttributeTypeAndValuePair pair;
        String attributeType;
        Object attributeValue;
        SearchResult searchResult;
        Attribute attr;
        NamingEnumeration<?> valuesEnum;
        Object value;
        boolean hasMatch;

        switch (filter.getType()) {
        case Filter.AND_FILTER:
            filteredResults = currentResults;
            filters = filter.getChildren();
            for (int i = 0; i < filters.size(); i++) {
                filteredResults = doFilter(filteredResults, filters.get(i));
            }
            break;

        case Filter.OR_FILTER:
            filters = filter.getChildren();
            filteredResults = new HashSet<SearchResult>();
            for (int i = 0; i < filters.size(); i++) {
                tempResults = doFilter(currentResults, filters.get(i));
                filteredResults.addAll(tempResults);
            }
            break;

        case Filter.NOT_FILTER:
            filteredResults = new HashSet<SearchResult>();
            Filter tempFilter = (Filter) filter.getValue();
            tempResults = doFilter(currentResults, tempFilter);

            iterator = currentResults.iterator();
            while (iterator.hasNext()) {
                searchResult = iterator.next();
                if (!tempResults.contains(searchResult)) {
                    filteredResults.add(searchResult);
                }
            }
            break;

        case Filter.EQUALITY_MATCH_FILTER:
        case Filter.APPROX_MATCH_FILTER:
            filteredResults = new HashSet<SearchResult>();
            pair = (AttributeTypeAndValuePair) filter.getValue();
            attributeType = pair.getType();
            attributeValue = pair.getValue();

            iterator = currentResults.iterator();
            while (iterator.hasNext()) {
                searchResult = iterator.next();
                attr = searchResult.getAttributes().get(attributeType);
                if (attr != null) {
                    hasMatch = false;
                    valuesEnum = attr.getAll();

                    while (valuesEnum.hasMore()) {
                        value = valuesEnum.next();
                        if (attributeValue.toString().equalsIgnoreCase(
                                value.toString())) {
                            hasMatch = true;
                        }
                    }
                    if (hasMatch) {
                        filteredResults.add(searchResult);
                    }
                }
            }
            break;

        case Filter.SUBSTRINGS_FILTER:
            filteredResults = new HashSet<SearchResult>();
            Filter.SubstringFilter substringFilter = (SubstringFilter) filter
                    .getValue();
            attributeType = substringFilter.getType();
            List<ChosenValue> list = substringFilter.getSubstrings();
            String attributePatternValue = ""; //$NON-NLS-1$
            for (int i = 0; i < list.size(); i++) {
                attributePatternValue += list.get(i).getValue().toString();
            }

            attributePatternValue = attributePatternValue.replaceAll("\\*", //$NON-NLS-1$
                    ".*"); //$NON-NLS-1$
            Pattern pattern = Pattern.compile(attributePatternValue,
                    Pattern.CASE_INSENSITIVE);

            iterator = currentResults.iterator();
            while (iterator.hasNext()) {
                searchResult = iterator.next();
                attr = searchResult.getAttributes().get(attributeType);
                if (attr != null) {
                    hasMatch = false;
                    valuesEnum = attr.getAll();
                    while (valuesEnum.hasMore()) {
                        value = valuesEnum.next();
                        if (pattern.matcher(value.toString()).matches()) {
                            hasMatch = true;
                        }
                    }
                    if (hasMatch) {
                        filteredResults.add(searchResult);
                    }
                }
            }

            break;

        case Filter.GREATER_OR_EQUAL_FILTER:
            filteredResults = new HashSet<SearchResult>();
            pair = (AttributeTypeAndValuePair) filter.getValue();
            attributeType = pair.getType();
            attributeValue = pair.getValue();
            iterator = currentResults.iterator();
            while (iterator.hasNext()) {
                searchResult = iterator.next();
                attr = searchResult.getAttributes().get(attributeType);
                if (attr != null) {
                    hasMatch = false;
                    valuesEnum = attr.getAll();
                    while (valuesEnum.hasMore()) {
                        value = valuesEnum.next();
                        if ((value.toString().compareTo(attributeValue
                                .toString())) >= 0) {
                            hasMatch = true;
                        }
                    }
                    if (hasMatch) {
                        filteredResults.add(searchResult);
                    }
                }
            }
            break;

        case Filter.LESS_OR_EQUAL_FILTER:
            filteredResults = new HashSet<SearchResult>();
            pair = (AttributeTypeAndValuePair) filter.getValue();
            attributeType = pair.getType();
            attributeValue = pair.getValue();
            iterator = currentResults.iterator();
            while (iterator.hasNext()) {
                searchResult = iterator.next();
                attr = searchResult.getAttributes().get(attributeType);
                if (attr != null) {
                    hasMatch = false;
                    valuesEnum = attr.getAll();
                    while (valuesEnum.hasMore()) {
                        value = valuesEnum.next();

                        if ((value.toString().compareTo(attributeValue
                                .toString())) <= 0) {
                            hasMatch = true;
                        }
                    }
                    if (hasMatch) {
                        filteredResults.add(searchResult);
                    }
                }
            }
            break;

        case Filter.PRESENT_FILTER:
            filteredResults = new HashSet<SearchResult>();
            attributeType = filter.getValue().toString();
            iterator = currentResults.iterator();
            while (iterator.hasNext()) {
                searchResult = iterator.next();
                attr = searchResult.getAttributes().get(attributeType);
                if (attr != null) {
                    filteredResults.add(searchResult);
                }
            }
            break;

        case Filter.EXTENSIBLE_MATCH_FILTER:
            // TODO
            break;

        default:
            // Never reach here.
        }

        return filteredResults;
    }
}
