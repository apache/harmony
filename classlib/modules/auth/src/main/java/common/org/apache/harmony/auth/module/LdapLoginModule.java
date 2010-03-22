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

package org.apache.harmony.auth.module;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.harmony.auth.LdapPrincipal;
import org.apache.harmony.auth.UserPrincipal;

public class LdapLoginModule extends SharedStateManager implements LoginModule {

    private static enum LdapAuthMode{
        search_first("search-first mode"),
        authentication_first("authentication-first mode"),
        authentication_only("authentication-only mode");
        
        private String strValue;
        LdapAuthMode(String str){
            strValue = str;
        }
        
        public String toString(){
            return strValue;
        }
    };
    
    //harmony lacks ldap provider
    private final String LDAP_FACTORY = "";
    
    private LoginModuleUtils.LoginModuleStatus status = new LoginModuleUtils.LoginModuleStatus();
    
    private Subject subject;

    private CallbackHandler callbackHandler;

    private Map<String, ?> options;
    
    private String ldapUserProvider;
    
    private String userID;

    private char[] userPassword;
    
    private LdapPrincipal ldapPrincipal;

    private UserPrincipal userPrincipal;
    
    private UserPrincipal extraUserPrincipal;

    private boolean useSSL = false;
    
    private LdapAuthMode ldapAuthMode;
    
    private String userFilter;
    
    private String authIdentity;
    
    private String authzIdentity;
    
    private String ldapPrincipalEntryName; 
    
    public boolean abort() throws LoginException {
        LoginModuleUtils.ACTION action = status.checkAbout();
        if (action.equals(LoginModuleUtils.ACTION.no_action)) {
            if (status.isLoggined()) {
                return true;
            } else {
                return false;
            }
        }
        clear();
        debugUtil.recordDebugInfo("[LdapLoginModule] aborted authentication\n");
        if(status.isCommitted()){
        	debugUtil.recordDebugInfo("[LdapLoginModule]: logged out Subject\n");
        }
        debugUtil.printAndClearDebugInfo();
        status.logouted();
        return true;
    }

    public boolean commit() throws LoginException {
        LoginModuleUtils.ACTION action = status.checkCommit();
        switch (action) {
        case no_action:
            return true;
        case logout:
            clear();
            throw new LoginException("Fail to login");
        default:
            if (subject.isReadOnly()) {
                clear();
                throw new LoginException("Subject is readonly.");
            }
            subject.getPrincipals().add(ldapPrincipal);
            debugUtil.recordDebugInfo("[LdapLoginModule] added LadpPrincipal \""+ ldapPrincipalEntryName + "\" to Subject\n");
            subject.getPrincipals().add(userPrincipal);
            debugUtil.recordDebugInfo("[LdapLoginModule] added UserPrincipal \""+ userID + "\" to Subject\n");
            if(extraUserPrincipal != null){
                subject.getPrincipals().add(extraUserPrincipal);
                debugUtil.recordDebugInfo("[LdapLoginModule] added UserPrincipal \""+ authzIdentity + "\" to Subject\n");
            }
            debugUtil.printAndClearDebugInfo();
            status.committed();
            clearPass();
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = (Map<String, Object>) sharedState;
        if (null == options) {
            throw new NullPointerException();
        }
        this.options = options;
        debugUtil = new DebugUtil(options);
        prepareSharedState(sharedState,options);
        processForOptions();
        status.initialized();
    }

    public boolean login() throws LoginException {
        LoginModuleUtils.ACTION action = status.checkLogin();
        if (action.equals(LoginModuleUtils.ACTION.no_action)) {
            return true;
        }
        getLdapParameters();
        loginWithSharedState();
/*        if(useFirstPass || tryFirstPass){
            getUserIdentityFromSharedStatus();
        }
        else{
            getUserIdentityFromCallbackHandler();
        }
        boolean passAuth = false;
        passAuth = mainAuthenticationProcess();
        if (!passAuth) {
            if(tryFirstPass){
                recordDebugInfo("[LdapLoginModule] tryFirstPass failed with:" + new FailedLoginException("Login incorrect").toString() + "\n");
                getUserIdentityFromCallbackHandler();
                passAuth = mainAuthenticationProcess();
                if(!passAuth){
                    recordDebugInfo("[LdapLoginModule] regular authentication failed\n");
                    printAndClearDebugInfo();
                    throw new FailedLoginException("Cannot bind to LDAP server");
                }
                else{
                    recordDebugInfo("[LdapLoginModule] regular authentication succeeded\n");
                }
            }
            else {
                if (useFirstPass) {
                    recordDebugInfo("[LdapLoginModule] useFirstPass failed with:"
                            + new FailedLoginException("Cannot bind to LDAP server")
                                    .toString() + "\n");                
                } else {
                    recordDebugInfo("[LdapLoginModule] regular authentication failed\n");
                }
                printAndClearDebugInfo();
                throw new FailedLoginException("Cannot bind to LDAP server");
            }
        }        
        else{
            if(tryFirstPass){
                recordDebugInfo("[LdapLoginModule] tryFirstPass ");
            }
            else if(useFirstPass){
                recordDebugInfo("[LdapLoginModule] useFirstPass ");
            }
            else{
                recordDebugInfo("[LdapLoginModule] authentication ");
            }
            recordDebugInfo("succeeded\n");
        }
        storePass();*/
        try {
            getPrinclpalsFromLdap();
        } catch (InvalidNameException e) {
            throw new LoginException("Error to get principal from ldap");
        }
        debugUtil.printAndClearDebugInfo();
        status.logined();
        return true;
    }

    public boolean logout() throws LoginException {
        LoginModuleUtils.ACTION action = status.checkLogout();
        if (action.equals(LoginModuleUtils.ACTION.no_action)) {
            return true;
        }
        clear();
        debugUtil.recordDebugInfo("[LdapLoginModule] logged out Subject\n");
        debugUtil.printAndClearDebugInfo();
        status.logouted();
        return true;
    }
    
    private void clear() throws LoginException {
        LoginModuleUtils.clearPassword(userPassword);
        userPassword = null;
        if (ldapPrincipal != null) {
            subject.getPrincipals().remove(ldapPrincipal);
            ldapPrincipal = null;
        }

        if (userPrincipal != null) {
            subject.getPrincipals().remove(userPrincipal);
            userPrincipal = null;
        }
        
        if (extraUserPrincipal != null) {
            subject.getPrincipals().remove(extraUserPrincipal);
            extraUserPrincipal = null;
        }      
        status.logouted();
    }
    
    protected boolean mainAuthenticationProcess() throws LoginException{
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_FACTORY);
        env.put(Context.PROVIDER_URL, ldapUserProvider);
        if(useSSL){
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        DirContext ctx;
        debugUtil.recordDebugInfo("[LdapLoginModule] attempting to authenticate user: "+ userID +"\n");
        try{
            if(ldapAuthMode == LdapAuthMode.search_first){
                boolean logined = false;
                env.put(Context.SECURITY_AUTHENTICATION, "none");
                ctx = new InitialDirContext(env);
                SearchControls constraints = new SearchControls();
                constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
                debugUtil.recordDebugInfo("[LdapLoginModule] searching for entry belonging to user: "+ userID +"\n");
                NamingEnumeration enumer = ctx.search("", userFilter, constraints);
                env.put(Context.SECURITY_AUTHENTICATION, "simple"); 
                while(enumer.hasMore()){
                    NameClassPair item = (NameClassPair) enumer.next();
                    try{
                        env.put(Context.SECURITY_PRINCIPAL, "simple");
                        env.put(Context.SECURITY_PRINCIPAL, item.getNameInNamespace());
                        env.put(Context.SECURITY_CREDENTIALS, new String(userPassword));
                        ctx = new InitialDirContext(env);
                        ldapPrincipalEntryName = item.getNameInNamespace();
                        debugUtil.recordDebugInfo("[LdapLoginModule] found entry: " + ldapPrincipalEntryName + "\n");
                        logined = true;
                        break;
                    }
                    catch(AuthenticationException e){
                        
                    }
                }
                if(!logined){
                    return false;
                }
            }
            else{
                env.put(Context.SECURITY_AUTHENTICATION, "simple"); 
                env.put(Context.SECURITY_PRINCIPAL, authIdentity);
                env.put(Context.SECURITY_CREDENTIALS, new String(userPassword));
                try{
                    ctx = new InitialDirContext(env);
                }
                catch(AuthenticationException e){
                    return false;
                }
                if(ldapAuthMode == LdapAuthMode.authentication_only){
                    ldapPrincipalEntryName = authIdentity;
                }
                else{
                    SearchControls constraints = new SearchControls();
                    constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
                    debugUtil.recordDebugInfo("[LdapLoginModule] searching for entry belonging to user: "+ userID +"\n");
                    NamingEnumeration enumer = ctx.search("", userFilter, constraints);
                    if(enumer.hasMore()){
                        NameClassPair item = (NameClassPair) enumer.next();
                        ldapPrincipalEntryName = item.getNameInNamespace();
                        debugUtil.recordDebugInfo("[LdapLoginModule] found entry: " + ldapPrincipalEntryName + "\n");
                    }
                    else{
                        return false;
                    }
                }
            }
        }
        catch(NamingException e){
            return false;
        }
        return true;
    }
    
    private void processForOptions() {
        Object optionValue = null;

        optionValue = options.get("useSSL");
        if (optionValue != null && optionValue.equals("true")) {
            useSSL = true;
        }
        
        userFilter = (String)options.get("userFilter");
        authIdentity = (String)options.get("authIdentity");
        authzIdentity = (String)options.get("authzIdentity");
        if(authIdentity==null){
            ldapAuthMode = LdapAuthMode.search_first;
        }
        else if(userFilter==null){
            ldapAuthMode = LdapAuthMode.authentication_only;
        }
        else{
            ldapAuthMode = LdapAuthMode.authentication_first;
        }
        debugUtil.recordDebugInfo("[LdapLoginModule] "+ ldapAuthMode + "\n");
        if(useSSL){
        	debugUtil.recordDebugInfo("[LdapLoginModule] SSL enabled\n");
        }
        else{
        	debugUtil.recordDebugInfo("[LdapLoginModule] SSL disabled\n");
        }
        debugUtil.printAndClearDebugInfo();
    }
    
    private void getLdapParameters()throws LoginException{
        ldapUserProvider = (String)options.get("userProvider");
        if (ldapUserProvider == null) {
            throw new LoginException("Unable to locate the LDAP directory service");
        }
        debugUtil.recordDebugInfo("[LdapLoginModule] user provider: " + ldapUserProvider + "\n");
    }
    
    protected void getUserIdentityFromCallbackHandler() throws LoginException {
        
        if (callbackHandler == null) {
            throw new LoginException("no CallbackHandler available");
        }
        ArrayList<Callback> callbacks = new ArrayList<Callback>();
        NameCallback jndiNameCallback = new NameCallback("User ID");
        callbacks.add(jndiNameCallback);
        PasswordCallback jndiPasswordCallback = new PasswordCallback(
                "User Password", false);
        callbacks.add(jndiPasswordCallback);
        try {
            callbackHandler.handle(callbacks.toArray(new Callback[callbacks
                    .size()]));
        } catch (Exception e) {
            throw new LoginException(e.toString());
        }
        userID = jndiNameCallback.getName();
        userPassword = jndiPasswordCallback.getPassword();
    }
    
    private void getPrinclpalsFromLdap() throws InvalidNameException{
        ldapPrincipal = new LdapPrincipal(ldapPrincipalEntryName); 
        userPrincipal = new UserPrincipal(userID);
        if(authzIdentity != null){
            extraUserPrincipal = new UserPrincipal(authzIdentity);
        }
    }

	@Override
	protected String getModuleName() {
		return "LdapLoginModule";
	}

	@Override
	protected String getUserName() {
		return userID;
	}

	@Override
	protected char[] getUserPassword() {
		return userPassword;
	}

	@Override
	protected void setUserName(String userName) {
		this.userID = userName;
	}

	@Override
	protected void setUserPassword(char[] userPassword) {
		this.userPassword = userPassword;
	}
}
