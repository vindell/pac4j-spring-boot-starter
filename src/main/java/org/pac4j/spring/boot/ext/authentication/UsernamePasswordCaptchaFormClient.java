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
package org.pac4j.spring.boot.ext.authentication;

import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.spring.boot.ext.Pac4jExtConstants;
import org.pac4j.spring.boot.ext.credentials.UsernamePasswordCaptchaCredentials;
import org.pac4j.spring.boot.ext.credentials.extractor.UsernamePasswordCaptchaCredentialsExtractor;

/**
 * TODO
 * @author 		： <a href="https://github.com/vindell">vindell</a>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UsernamePasswordCaptchaFormClient extends IndirectClient<UsernamePasswordCaptchaCredentials, CommonProfile> {

    private String loginUrl;

    public final static String ERROR_PARAMETER = "error";

    public final static String MISSING_FIELD_ERROR = "missing_field";

    private String usernameParameter = Pac4jConstants.USERNAME;

    private String passwordParameter = Pac4jConstants.PASSWORD;

    private String captchaParameter = Pac4jExtConstants.CAPTCHA;
    
    private boolean postOnly = true;
    
    public UsernamePasswordCaptchaFormClient() {
    }

    public UsernamePasswordCaptchaFormClient(final String loginUrl, final Authenticator usernamePasswordAuthenticator) {
        this.loginUrl = loginUrl;
        defaultAuthenticator(usernamePasswordAuthenticator);
    }

	public UsernamePasswordCaptchaFormClient(final String loginUrl, final String usernameParameter,
    		final String passwordParameter, final String captchaParameter, boolean postOnly,
                      final Authenticator usernamePasswordAuthenticator) {
        this.loginUrl = loginUrl;
        this.usernameParameter = usernameParameter;
        this.passwordParameter = passwordParameter;
        this.captchaParameter = captchaParameter;
        this.postOnly = postOnly;
        defaultAuthenticator(usernamePasswordAuthenticator);
    }

    public UsernamePasswordCaptchaFormClient(final String loginUrl, final Authenticator usernamePasswordAuthenticator,
                      final ProfileCreator profileCreator) {
        this.loginUrl = loginUrl;
        defaultAuthenticator(usernamePasswordAuthenticator);
        defaultProfileCreator(profileCreator);
    }

    @Override
    protected void clientInit() {
        CommonHelper.assertNotBlank("loginUrl", this.loginUrl);
        CommonHelper.assertNotBlank("usernameParameter", this.usernameParameter);
        CommonHelper.assertNotBlank("passwordParameter", this.passwordParameter);
        CommonHelper.assertNotBlank("captchaParameter", this.captchaParameter);
        
        defaultRedirectActionBuilder(ctx -> {
            final String finalLoginUrl = getUrlResolver().compute(this.loginUrl, ctx);
            return RedirectAction.redirect(finalLoginUrl);
        });
        defaultCredentialsExtractor(new UsernamePasswordCaptchaCredentialsExtractor(usernameParameter, passwordParameter, captchaParameter, postOnly));
    }

    @Override
    protected UsernamePasswordCaptchaCredentials retrieveCredentials(final WebContext context) {
        CommonHelper.assertNotNull("credentialsExtractor", getCredentialsExtractor());
        CommonHelper.assertNotNull("authenticator", getAuthenticator());

        final String username = context.getRequestParameter(this.usernameParameter);
        UsernamePasswordCaptchaCredentials credentials;
        try {
            // retrieve credentials
            credentials = getCredentialsExtractor().extract(context);
            logger.debug("usernamePasswordCredentials: {}", credentials);
            if (credentials == null) {
                throw handleInvalidCredentials(context, username, "Username and password cannot be blank -> return to the form with error",
                    MISSING_FIELD_ERROR);
            }
            // validate credentials
            getAuthenticator().validate(credentials, context);
        } catch (final CredentialsException e) {
            throw handleInvalidCredentials(context, username, "Credentials validation fails -> return to the form with error",
                computeErrorMessage(e));
        }

        return credentials;
    }

    protected HttpAction handleInvalidCredentials(final WebContext context, final String username, String message, String errorMessage) {
        // it's an AJAX request -> unauthorized (instead of a redirection)
        if (getAjaxRequestResolver().isAjax(context)) {
            logger.info("AJAX request detected -> returning 401");
            return HttpAction.status(HttpConstants.UNAUTHORIZED, context);
        } else {
            String redirectionUrl = CommonHelper.addParameter(this.loginUrl, this.usernameParameter, username);
            redirectionUrl = CommonHelper.addParameter(redirectionUrl, ERROR_PARAMETER, errorMessage);
            logger.debug("redirectionUrl: {}", redirectionUrl);
            return HttpAction.redirect(context, redirectionUrl);
        }
    }

    /**
     * Return the error message depending on the thrown exception. Can be overriden for other message computation.
     *
     * @param e the technical exception
     * @return the error message
     */
    protected String computeErrorMessage(final Exception e) {
        return e.getClass().getSimpleName();
    }

    public String getLoginUrl() {
        return this.loginUrl;
    }

    public void setLoginUrl(final String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getUsernameParameter() {
        return this.usernameParameter;
    }

    public void setUsernameParameter(final String usernameParameter) {
        this.usernameParameter = usernameParameter;
    }

    public String getPasswordParameter() {
        return this.passwordParameter;
    }

    public void setPasswordParameter(final String passwordParameter) {
        this.passwordParameter = passwordParameter;
    }
    
    public String getCaptchaParameter() {
		return captchaParameter;
	}

	public void setCaptchaParameter(String captchaParameter) {
		this.captchaParameter = captchaParameter;
	}

	public boolean isPostOnly() {
		return postOnly;
	}

	public void setPostOnly(boolean postOnly) {
		this.postOnly = postOnly;
	}

	@Override
    public String toString() {
        return CommonHelper.toNiceString(this.getClass(), "callbackUrl", this.callbackUrl, "name", getName(), "loginUrl",
                this.loginUrl, "usernameParameter", this.usernameParameter, "passwordParameter", this.passwordParameter,
                "redirectActionBuilder", getRedirectActionBuilder(), "extractor", getCredentialsExtractor(),
                "authenticator", getAuthenticator(), "profileCreator", getProfileCreator());
    }
}
