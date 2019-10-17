/*
 * Copyright (c) 2018, vindell (https://github.com/vindell).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.pac4j.spring.boot.ext.credentials.extractor;

import java.util.Optional;

import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.extractor.CredentialsExtractor;
import org.pac4j.spring.boot.ext.Pac4jExtConstants;
import org.pac4j.spring.boot.ext.authentication.AuthenticationRequest;
import org.pac4j.spring.boot.ext.credentials.UsernamePasswordCaptchaCredentials;
import org.pac4j.spring.boot.ext.exception.AuthMethodNotSupportedException;
import org.pac4j.spring.boot.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONObject;

/**
 * TODO
 * @author 		： <a href="https://github.com/vindell">vindell</a>
 */
public class UsernamePasswordCaptchaCredentialsExtractor implements CredentialsExtractor<UsernamePasswordCaptchaCredentials> {
	
	// =====================================================================================
	private static Logger logger = LoggerFactory.getLogger(UsernamePasswordCaptchaCredentialsExtractor.class);

	private String usernameParameter = Pac4jConstants.USERNAME;
	private String passwordParameter = Pac4jConstants.PASSWORD;
	private String captchaParameter = Pac4jExtConstants.CAPTCHA;
	private boolean postOnly = true;
	
    public UsernamePasswordCaptchaCredentialsExtractor(String usernameParameter, String passwordParameter,
			String captchaParameter, boolean postOnly) {
		this.usernameParameter = usernameParameter;
		this.passwordParameter = passwordParameter;
		this.captchaParameter = captchaParameter;
		this.postOnly = postOnly;
	}

	@Override
    public Optional<UsernamePasswordCaptchaCredentials> extract(WebContext context) {
    	
    	if (isPostOnly() && ! WebUtils.isPostRequest(context)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Authentication method not supported. Request method: " + context.getRequestMethod());
			}
			throw new AuthMethodNotSupportedException("Authentication method not supported: " + context.getRequestMethod());
		}

		// Post && JSON
		if(WebUtils.isPostRequest(context) && WebUtils.isContentTypeJson(context)) {
			
			AuthenticationRequest loginRequest = JSONObject.parseObject(context.getRequestContent(), AuthenticationRequest.class);
			
			final String username = loginRequest.getUsername();
	        final String password = loginRequest.getPassword();
	        if (username == null || password == null) {
	            return null;
	        }
	        String captcha = loginRequest.getCaptcha();
	        return Optional.ofNullable(new UsernamePasswordCaptchaCredentials(username, password, captcha));

		} else {
			
			final Optional<String> username = context.getRequestParameter(this.usernameParameter);
	        final Optional<String> password = context.getRequestParameter(this.passwordParameter);
	        if (!username.isPresent() || !password.isPresent()) {
	            return null;
	        }
	        
	        Optional<String> captcha = context.getRequestParameter(this.captchaParameter);
	        
	        return Optional.ofNullable(new UsernamePasswordCaptchaCredentials(username.get(), password.get(), captcha.get()));
	 		
		}
        
    }

	/**
	 * Sets the parameter name which will be used to obtain the username from the login
	 * request.
	 *
	 * @param usernameParameter the parameter name. Defaults to "username".
	 */
	public void setUsernameParameter(String usernameParameter) {
		Assert.hasText(usernameParameter, "Username parameter must not be empty or null");
		this.usernameParameter = usernameParameter;
	}

	/**
	 * Sets the parameter name which will be used to obtain the password from the login
	 * request..
	 *
	 * @param passwordParameter the parameter name. Defaults to "password".
	 */
	public void setPasswordParameter(String passwordParameter) {
		Assert.hasText(passwordParameter, "Password parameter must not be empty or null");
		this.passwordParameter = passwordParameter;
	}

	/**
	 * Defines whether only HTTP POST requests will be allowed by this filter. If set to
	 * true, and an authentication request is received which is not a POST request, an
	 * exception will be raised immediately and authentication will not be attempted. The
	 * <tt>unsuccessfulAuthentication()</tt> method will be called as if handling a failed
	 * authentication.
	 * <p>
	 * Defaults to <tt>true</tt> but may be overridden by subclasses.
	 */
	public void setPostOnly(boolean postOnly) {
		this.postOnly = postOnly;
	}

	public final String getUsernameParameter() {
		return usernameParameter;
	}

	public final String getPasswordParameter() {
		return passwordParameter;
	}
	
	public boolean isPostOnly() {
		return postOnly;
	}

	public String getCaptchaParameter() {
		return captchaParameter;
	}

	public void setCaptchaParameter(String captchaParameter) {
		this.captchaParameter = captchaParameter;
	}
    
}