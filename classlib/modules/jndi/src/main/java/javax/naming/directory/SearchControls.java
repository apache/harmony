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

package javax.naming.directory;

import java.io.Serializable;

/**
 * This class represents the scope of a search, and the list of attributes that
 * the search encompasses.
 * <p>
 * The various scopes are defined by class constants representing Object,
 * Single-depth, and Full-depth searches of the directory.
 * </p>
 * <p>
 * This class is not thread-safe.
 * </p>
 */
public class SearchControls implements Serializable {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object
     */
    private static final long serialVersionUID = 0xdd935921dd0f3e33L;

    /**
     * Bounds the search to the object scope only.
     * <p>
     * The search takes place over the given object. The resulting enumeration
     * will therefore only contain either zero or one (the given) object
     * depending upon whether the object matches the search criteria.
     * </p>
     * <p>
     * If the object does match, its name in the enumeration will be empty since
     * names are specified relative to the root of the search.
     * </p>
     */
    public static final int OBJECT_SCOPE = 0;

    /**
     * Bounds the search to a single level of the naming context rooted at the
     * given object.
     * <p>
     * The search will take place over the object, or if the object is a context
     * then the object and all objects that are one level removed from the given
     * context.
     * </p>
     * <p>
     * Matches are named by a relative name to the given root, so will have
     * atomic (single level valid) names.
     * </p>
     */
    public static final int ONELEVEL_SCOPE = 1;

    /**
     * Bounds the search to the subtree rooted at the given object or naming
     * context.
     * <p>
     * The search will take place over the object, or if the object is a context
     * then the object and all objects that are reachable from he given context.
     * </p>
     * <p>
     * The names that are returned in the enumeration are defined to be either
     * relative names to the given root, or full URIs of the matching objects.
     * </p>
     * <p>
     * The search is defined to no cross naming system boundaries.
     * </p>
     */
    public static final int SUBTREE_SCOPE = 2;

    /**
     * The scope of the search.
     * <p>
     * Constrained to be one of ONELEVEL_SCOPE, OBJECT_SCOPE, or SUBTREE_SCOPE.
     * </p>
     * 
     * @serial
     */
    private int searchScope;

    /**
     * search time limitation.
     * <p>
     * Maximum number of milliseconds to wait for the search to complete.
     * </p>
     * 
     * @serial
     */
    private int timeLimit;

    /**
     * Flag showing whether searches should dereference JNDI links.
     * 
     * @serial
     */
    private boolean derefLink;

    /**
     * Flag showing whether an Object is returned in the search results.
     * 
     * @serial
     */
    private boolean returnObj;

    /**
     * Maximum number of search results that will be returned.
     * 
     * @serial
     */
    private long countLimit;

    /**
     * Lists attributes to match.
     * <p>
     * Contains a single entry for each attribute that is to be matches -- or it
     * is null if all attributes are to be matched.
     * </p>
     * 
     * @serial
     */
    private String attributesToReturn[];

    /**
     * Default constructor.
     * <p>
     * Equivalent to:
     * <code>SearchControls (ONELEVEL_SCOPE, 0, 0, null, false, false)</code>.
     * </p>
     */
    public SearchControls() {
        this(ONELEVEL_SCOPE, 0, 0, null, false, false);
    }

    /**
     * Constructs a search control instance with all parameters.
     * 
     * @param searchScope
     *            the search scope, chosen from OBJECT_SCOPE, ONELEVEL_SCOPE or
     *            SUBTREE_SCOPE.
     * @param countLimit
     *            the maximum number of search results. If is zero, then the
     *            number of search results returned is unlimited.
     * @param timeLimit
     *            the maximum number of search time in milliseconds, for the
     *            search. If is zero, then there is no time limit for the
     *            search.
     * @param attributesToReturn
     *            an array of identifiers of attributes to return for each
     *            result. If is null, then all attributes are returned for each
     *            result.
     * @param returnObj
     *            an flag. If true then search results contain an object,
     *            otherwise they contain only a name and class pair.
     * @param derefLink
     *            an flag. If true then <code>LinkRef</code> references are
     *            followed in the search, otherwise they are not.
     * 
     */
    public SearchControls(int searchScope, long countLimit, int timeLimit,
            String attributesToReturn[], boolean returnObj, boolean derefLink) {

        this.searchScope = searchScope;
        this.countLimit = countLimit;
        this.timeLimit = timeLimit;
        this.attributesToReturn = attributesToReturn;
        this.derefLink = derefLink;
        this.returnObj = returnObj;
    }

    /**
     * Gets the maximum number of search results.
     * 
     * @return the maximum number of search results to return.
     */
    public long getCountLimit() {
        return countLimit;
    }

    /**
     * Gets the flag indicates whether search will follow LinkRef references.
     * 
     * @return flag indicates whether searches will follow <code>LinkRef</code>
     *         references. If true then <code>LinkRef</code> references are
     *         followed in the search, otherwise they are not.
     */
    public boolean getDerefLinkFlag() {
        return derefLink;
    }

    /**
     * Gets identifiers of attributes to return for each result.
     * 
     * @return an array of identifiers of attributes to return for each result.
     *         If is null, then all attributes are returned for each result.
     */
    public String[] getReturningAttributes() {
        return attributesToReturn;
    }

    /**
     * Gets the flag whether search results will include the object (true) or
     * not (false).
     * 
     * @return if true then search results contain an object, otherwise they
     *         contain only a name and class pair.
     */
    public boolean getReturningObjFlag() {
        return returnObj;
    }

    /**
     * Gets the search scope.
     * 
     * @return the search scope, chosen from OBJECT_SCOPE, ONELEVEL_SCOPE or
     *         SUBTREE_SCOPE.
     */
    public int getSearchScope() {
        return searchScope;
    }

    /**
     * Gets the the maximum number of search time.
     * 
     * @return the maximum number of search time in milliseconds, for the
     *         search. If is zero, then there is no time limit for the search.
     */
    public int getTimeLimit() {
        return timeLimit;
    }

    /**
     * Sets the maximum number of search results.
     * 
     * @param l
     *            the maximum number of search results. If is zero, then the
     *            number of search results returned is unlimited.
     */
    public void setCountLimit(long l) {
        countLimit = l;
    }

    /**
     * Sets the flag indicates whether search will follow <code>LinkRef</code>
     * references.
     * 
     * @param flag
     *            flag indicates whether searches will follow
     *            <code>LinkRef</code> references. If true then
     *            <code>LinkRef</code> references are followed in the search,
     *            otherwise they are not.
     */
    public void setDerefLinkFlag(boolean flag) {
        derefLink = flag;
    }

    /**
     * Sets identifiers of attributes to return for each result.
     * 
     * @param as
     *            an array of identifiers of attributes to return for each
     *            result. If is null, then all attributes are returned for each
     *            result.
     */
    public void setReturningAttributes(String as[]) {
        attributesToReturn = as;
    }

    /**
     * Sets the flag whether search results will include the object (true) or
     * not (false).
     * 
     * @param flag
     *            if true then search results contain an object, otherwise they
     *            contain only a name and class pair.
     */
    public void setReturningObjFlag(boolean flag) {
        returnObj = flag;
    }

    /**
     * Sets the search scope.
     * 
     * @param i
     *            the search scope, chosen from OBJECT_SCOPE, ONELEVEL_SCOPE or
     *            SUBTREE_SCOPE.
     */
    public void setSearchScope(int i) {
        searchScope = i;
    }

    /**
     * Sets the the maximum number of search time.
     * 
     * @param i
     *            the maximum number of search time in milliseconds, for the
     *            search. If is zero, then there is no time limit for the
     *            search.
     */
    public void setTimeLimit(int i) {
        timeLimit = i;
    }

}
