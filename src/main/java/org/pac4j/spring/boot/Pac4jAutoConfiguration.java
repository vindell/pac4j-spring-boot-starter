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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.authorizer.CheckHttpMethodAuthorizer;
import org.pac4j.core.authorization.generator.AuthorizationGenerator;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants.HTTP_METHOD;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.core.http.callback.CallbackUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.http.authorization.authorizer.IpRegexpAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;


@Configuration
@ConditionalOnClass({ Clients.class })
@ConditionalOnProperty(prefix = Pac4jProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ Pac4jProperties.class })
@SuppressWarnings("rawtypes")
public class Pac4jAutoConfiguration {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Bean
	public Clients clients (
			Pac4jProperties pac4jProperties,
			List<Client> clientList,
			List<AuthorizationGenerator> authorizationGenerators,
			AjaxRequestResolver ajaxRequestResolver,
			CallbackUrlResolver callbackUrlResolver,
			UrlResolver urlResolver) {
		
		
		Clients clients = new Clients();
		
		clients.setAjaxRequestResolver(ajaxRequestResolver);
		clients.setAuthorizationGenerators(authorizationGenerators);
		clients.setCallbackUrlResolver(callbackUrlResolver);
		clients.setClients(clientList);
		if(StringUtils.hasText(pac4jProperties.getClients())) {
			clients.setDefaultSecurityClients(pac4jProperties.getClients());
		} else {
			final List<String> defaultClients = clientList.stream().map(client -> client.getName()).collect(Collectors.toList());
			clients.setDefaultSecurityClients(StringUtils.collectionToCommaDelimitedString(defaultClients));
		}
		clients.setUrlResolver(urlResolver);
		
		for (Client client : clients.getClients()) {
			logger.debug("Client Inited : {}", client.toString());
		}
		logger.debug("Default Security Clients : {}", clients.getDefaultSecurityClients());
		
		
		return clients;
	}
	
	@Bean
	public Config config(Pac4jProperties pac4jProperties, Clients clients,
						 ObjectProvider<Authorizer> authorizerProvider,
						 ObjectProvider<SecurityLogic> securityLogicProvider,
						 ObjectProvider<HttpActionAdapter> httpActionAdapterProvider) {

		final Config config = new Config(clients);
		
		if(StringUtils.hasText(pac4jProperties.getAllowedIpRegexpPattern())) {	
			config.addAuthorizer("isIPAuthenticated", new IpRegexpAuthorizer(pac4jProperties.getAllowedIpRegexpPattern()));
		}
		if(ArrayUtils.isNotEmpty(pac4jProperties.getAllowedHttpMethods())) {	
			String[] allowedHttpMethods = pac4jProperties.getAllowedHttpMethods();
			List<HTTP_METHOD> methods = new ArrayList<HTTP_METHOD>();
			for (String method : allowedHttpMethods) {
				methods.add(HTTP_METHOD.valueOf(method));
			}
			config.addAuthorizer("isMethodAuthenticated", new CheckHttpMethodAuthorizer(methods));
		}
		
		/*excludePath
		excludeRegex
		excludeBranch
		
		[] methods
		private String headerName;
	    private String expectedValue;*/
	    
	    
		//config.addMatcher("path", new AntPathMatcher().excludePath("").excludeBranch("").excludeRegex(""));
		//config.addMatcher("header", new HeaderMatcher());
		//config.addMatcher("method", new HttpMethodMatcher());
		
		config.setClients(clients);
		config.setHttpActionAdapter(httpActionAdapterProvider.getIfAvailable());
		//config.setSessionStore(sessionStore);
		
		return config;
	}
}

