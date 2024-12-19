/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
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
package org.pac4j.spring.boot;

import java.util.List;

import org.pac4j.core.ext.authentication.AuthenticatingFailureCounter;
import org.pac4j.core.ext.authentication.UsernamePasswordCaptchaAuthenticator;
import org.pac4j.core.ext.authentication.UsernamePasswordCaptchaFormClient;
import org.pac4j.core.ext.authentication.captcha.CaptchaResolver;
import org.pac4j.core.ext.credentials.extractor.UsernamePasswordCaptchaCredentialsExtractor;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.core.http.callback.CallbackUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.http.client.direct.CookieClient;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.client.direct.ParameterClient;
import org.pac4j.jwt.config.encryption.EncryptionConfiguration;
import org.pac4j.jwt.config.encryption.SecretEncryptionConfiguration;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.config.signature.SignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;

@Configuration
@AutoConfigureBefore(Pac4jAutoConfiguration.class)
@ConditionalOnClass({ CookieClient.class, ParameterClient.class, HeaderClient.class, JwtAuthenticator.class })
@ConditionalOnProperty(prefix = Pac4jJwtProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ Pac4jJwtProperties.class, Pac4jProperties.class, ServerProperties.class })
public class Pac4jJwtConfiguration {

	@Autowired
	private Pac4jJwtProperties jwtProperties;

	@Bean
	@ConditionalOnMissingBean
	public EncryptionConfiguration encryptionConfiguration() {
        return new SecretEncryptionConfiguration(
                jwtProperties.getEncryptSecret(), JWEAlgorithm.parse(jwtProperties.getJweAlgorithm().value()),
                EncryptionMethod.parse(jwtProperties.getEncryption().value()));
	}
	
	@Bean
	@ConditionalOnMissingBean
	public SignatureConfiguration signatureConfiguration() {
		return new SecretSignatureConfiguration(jwtProperties.getSignSecret(),
				JWSAlgorithm.parse(jwtProperties.getJwsAlgorithm().value()));
	}

	@Bean
	@ConditionalOnMissingBean
	public JwtAuthenticator jwtAuthenticator(List<SignatureConfiguration> signatureConfigurations,
			List<EncryptionConfiguration> encryptionConfigurations) {

		JwtAuthenticator authenticator = new JwtAuthenticator();
		//authenticator.setEncryptionConfiguration(encryptionConfiguration);
		authenticator.setEncryptionConfigurations(encryptionConfigurations);
		//authenticator.setExpirationTime(expirationTime);
		//authenticator.setProfileDefinition(profileDefinition);
		//authenticator.setRealmName(realmName);
		//authenticator.setSignatureConfiguration(signatureConfiguration);
		authenticator.setSignatureConfigurations(signatureConfigurations);
		
		return authenticator;
	}
	
	@Bean("jwtUpcAuthenticator")
	public UsernamePasswordCaptchaAuthenticator jwtUpcAuthenticator(ObjectProvider<CaptchaResolver> captchaResolver,
																	ObjectProvider<AuthenticatingFailureCounter> failureCounter) {
		
		UsernamePasswordCaptchaAuthenticator authenticator = new UsernamePasswordCaptchaAuthenticator();
		
		authenticator.setCaptchaRequired(jwtProperties.isCaptchaRequired());
		authenticator.setCaptchaResolver(captchaResolver.getIfAvailable());
		authenticator.setFailureCounter(failureCounter.getIfAvailable());
		authenticator.setRetryTimesKeyAttribute(jwtProperties.getRetryTimesKeyAttribute());
		authenticator.setRetryTimesWhenAccessDenied(jwtProperties.getRetryTimesWhenAccessDenied());
		
		return authenticator;
	}
	
	@Bean("jwtUpcCredentialsExtractor")
	public UsernamePasswordCaptchaCredentialsExtractor jwtUpcCredentialsExtractor() {
        return new UsernamePasswordCaptchaCredentialsExtractor(
                jwtProperties.getUsernameParameterName(),
                jwtProperties.getPasswordParameterName(),
                jwtProperties.getCaptchaParamName(),
                jwtProperties.isPostOnly());
		
	}
	
	@Bean("jwtAuthcClient")
 	public UsernamePasswordCaptchaFormClient jwtAuthcClient(AjaxRequestResolver ajaxRequestResolver, CallbackUrlResolver callbackUrlResolver,
 			@Qualifier("jwtUpcAuthenticator") UsernamePasswordCaptchaAuthenticator authenticator,
 			@Qualifier("jwtUpcCredentialsExtractor") UsernamePasswordCaptchaCredentialsExtractor credentialsExtractor,
 			UrlResolver urlResolver) {

		UsernamePasswordCaptchaFormClient client = new UsernamePasswordCaptchaFormClient();
		
		client.setAjaxRequestResolver(ajaxRequestResolver);
		client.setAuthenticator(authenticator);
		//client.setAuthorizationGenerator(authorizationGenerator);
		//client.setAuthorizationGenerators(authorizationGenerators);
		client.setCallbackUrl(jwtProperties.getCallbackUrl());
		client.setCallbackUrlResolver(callbackUrlResolver);
		client.setCredentialsExtractor(credentialsExtractor);
		if(jwtProperties.getCustomProperties() != null ) {
			client.setCustomProperties(jwtProperties.getCustomProperties());
		}
		client.setLoginUrl(jwtProperties.getLoginUrl());
		client.setName(jwtProperties.getDefaultClientName());
		client.setPasswordParameter(jwtProperties.getPasswordParameterName());
		client.setUrlResolver(urlResolver);
		client.setUsernameParameter(jwtProperties.getUsernameParameterName());
		
 		return client;
 	}
	
	@Bean("jwtCookieAuthzClient")
	public CookieClient jwtCookieAuthzClient(JwtAuthenticator jwtAuthenticator) {

		CookieClient client = new CookieClient(jwtProperties.getAuthorizationCookieName(), 
				jwtAuthenticator);
		
		//client.setAuthenticator(jwtAuthenticator);
		//client.setAuthorizationGenerator(authorizationGenerator);
		//client.setAuthorizationGenerators(authorizationGenerators);
		//client.setCredentialsExtractor(credentialsExtractor);
		if(jwtProperties.getCustomProperties() != null ) {
			client.setCustomProperties(jwtProperties.getCustomProperties());
		}
		client.setName(jwtProperties.getCookieClientName());
		client.setCookieName(jwtProperties.getAuthorizationCookieName());
		//client.setProfileCreator(profileCreator);
		
		return client;
	}

	@Bean("jwtHeaderAuthzClient")
	public HeaderClient jwtHeaderAuthzClient(JwtAuthenticator jwtAuthenticator) {

		HeaderClient client = new HeaderClient(jwtProperties.getAuthorizationHeaderName(), jwtAuthenticator);
		
		//client.setAuthenticator(jwtAuthenticator);
		//client.setAuthorizationGenerator(authorizationGenerator);
		//client.setAuthorizationGenerators(authorizationGenerators);
		//client.setCredentialsExtractor(credentialsExtractor);
		if(jwtProperties.getCustomProperties() != null ) {
			client.setCustomProperties(jwtProperties.getCustomProperties());
		}
		client.setName(jwtProperties.getHeaderClientName());
		client.setHeaderName(jwtProperties.getAuthorizationHeaderName());
		client.setPrefixHeader(jwtProperties.getAuthorizationHeaderPrefix());
		//client.setProfileCreator(profileCreator);

		return client;
	}

	@Bean
	public ParameterClient jwtParamAuthzClient(JwtAuthenticator jwtAuthenticator) {
		
		// REST authent with JWT for a token passed in the url as the token parameter

		ParameterClient client = new ParameterClient(jwtProperties.getAuthorizationParamName(), jwtAuthenticator);
		//client.setAuthenticator(jwtAuthenticator);
		//client.setAuthorizationGenerator(authorizationGenerator);
		//client.setAuthorizationGenerators(authorizationGenerators);
		//client.setCredentialsExtractor(credentialsExtractor);
		if(jwtProperties.getCustomProperties() != null ) {
			client.setCustomProperties(jwtProperties.getCustomProperties());
		}
		client.setName(jwtProperties.getParamClientName());
		client.setParameterName(jwtProperties.getAuthorizationParamName());
		//client.setProfileCreator(profileCreator);
		client.setSupportGetRequest(jwtProperties.isSupportGetRequest());
		client.setSupportPostRequest(jwtProperties.isSupportPostRequest());
		
		return client;
	}

}
