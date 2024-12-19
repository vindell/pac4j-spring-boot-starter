package org.pac4j.spring.boot;

import org.apereo.cas.client.util.CommonUtils;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.client.CasProxyReceptor;
import org.pac4j.cas.client.direct.DirectCasClient;
import org.pac4j.cas.client.direct.DirectCasProxyClient;
import org.pac4j.cas.client.rest.CasRestBasicAuthClient;
import org.pac4j.cas.client.rest.CasRestFormClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.http.callback.CallbackUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.spring.boot.utils.Pac4jUrlUtils;
import org.pac4j.spring.boot.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@AutoConfigureBefore(Pac4jAutoConfiguration.class)
@ConditionalOnClass({CasConfiguration.class})
@ConditionalOnProperty(prefix = Pac4jCasProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ Pac4jCasProperties.class, Pac4jProperties.class, ServerProperties.class })
public class Pac4jCasConfiguration {
	
	@Autowired
	private Pac4jProperties pac4jProperties;
	@Autowired
	private Pac4jCasProperties pac4jCasProperties;

	@Bean
	public CasProxyReceptor proxyReceptor() {
		CasProxyReceptor proxyReceptor = new CasProxyReceptor();
		proxyReceptor.setCallbackUrl(pac4jProperties.getCallbackUrl());
		return proxyReceptor;
	}
	
	@Bean
	public CasConfiguration casConfiguration(UrlResolver urlResolver,
			@Autowired(required = false) CasProxyReceptor proxyReceptor) {

		CasConfiguration configuration = new CasConfiguration(pac4jCasProperties.getLoginUrl(), pac4jCasProperties.getProtocol() );
		
		if(pac4jCasProperties.isAcceptAnyProxy() && StringUtils.hasText(pac4jCasProperties.getAllowedProxyChains())) {	
			configuration.setAcceptAnyProxy(pac4jCasProperties.isAcceptAnyProxy());
			configuration.setAllowedProxyChains(CommonUtils.createProxyList(pac4jCasProperties.getAllowedProxyChains()));
		}
		
		/**
		 * 批量设置参数
		 */
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		
		map.from(pac4jCasProperties.getEncoding()).to(configuration::setEncoding);
		map.from(pac4jCasProperties.getCustomParams()).to(configuration::setCustomParams);
		map.from(pac4jCasProperties.isGateway()).to(configuration::setGateway);
		map.from(pac4jCasProperties.getLoginUrl()).to(configuration::setLoginUrl);
		map.from(pac4jCasProperties.getMethod()).to(configuration::setMethod);
		map.from(pac4jCasProperties.getPostLogoutUrlParameter()).to(configuration::setPostLogoutUrlParameter);
		map.from(pac4jCasProperties.getPrefixUrl()).to(configuration::setPrefixUrl);
		map.from(pac4jCasProperties.getProtocol()).to(configuration::setProtocol);
		map.from(proxyReceptor).to(configuration::setProxyReceptor);
		map.from(pac4jCasProperties.isRenew()).to(configuration::setRenew);
		map.from(pac4jCasProperties.getRestUrl()).to(configuration::setRestUrl);
		map.from(pac4jCasProperties.getTolerance()).to(configuration::setTimeTolerance);
		map.from(urlResolver).to(configuration::setUrlResolver);
		
		return configuration;
	}

	/**
	 * 
	 * TODO
	 * @author 		： <a href="https://github.com/hiwepy">wandl</a>
	 * @param configuration
	 * @return
	 */
	@Bean
	@ConditionalOnProperty(prefix = Pac4jCasProperties.PREFIX, value = Pac4jClientNames.CAS_CLIENT, havingValue = "true")
	public CasClient casClient(CasConfiguration configuration, CallbackUrlResolver callbackUrlResolver) {
		
		CasClient casClient = new CasClient(configuration);
		
		String clientName = StringUtils.hasText(pac4jCasProperties.getCasClientName()) ? pac4jCasProperties.getCasClientName() : Pac4jClientNames.CAS_CLIENT;
		String serviceUrl = Pac4jUrlUtils.constructRedirectUrl(pac4jCasProperties.getServiceUrl(), pac4jProperties.getClientParameterName(), clientName);
		String callbackUrl =  CommonUtils.constructRedirectUrl(pac4jCasProperties.getLoginUrl(), pac4jCasProperties.getServiceParameterName(),
				serviceUrl, pac4jCasProperties.isRenew(), pac4jCasProperties.isGateway());
		
		casClient.setCallbackUrl( callbackUrl);
		casClient.setCallbackUrlResolver(callbackUrlResolver);
		casClient.setName(clientName);
		
		return casClient;
	}
	
	@Bean
	@ConditionalOnProperty(prefix = Pac4jCasProperties.PREFIX, value = Pac4jClientNames.DIRECT_CAS_CLIENT, havingValue = "true")
	public DirectCasClient directCasClient(CasConfiguration configuration, CallbackUrlResolver callbackUrlResolver) {
		
		DirectCasClient casClient = new DirectCasClient();
		
		casClient.setConfiguration(configuration);
		casClient.setCallbackUrlResolver(callbackUrlResolver);
		casClient.setName(StringUtils.hasText(pac4jCasProperties.getDirectCasClientName()) ? pac4jCasProperties.getDirectCasClientName() : Pac4jClientNames.DIRECT_CAS_CLIENT);
		
		return casClient;
	}
	
	@Bean 
	@ConditionalOnProperty(prefix = Pac4jCasProperties.PREFIX, value = Pac4jClientNames.DIRECT_CAS_PROXY_CLIENT, havingValue = "true")
	public DirectCasProxyClient directCasProxyClient(CasConfiguration configuration, CallbackUrlResolver callbackUrlResolver) {
		
		DirectCasProxyClient casClient = new DirectCasProxyClient();
		
		casClient.setConfiguration(configuration);
		casClient.setCallbackUrlResolver(callbackUrlResolver);
		casClient.setName(StringUtils.hasText(pac4jCasProperties.getDirectCasProxyClientName()) ? pac4jCasProperties.getDirectCasProxyClientName() : Pac4jClientNames.DIRECT_CAS_PROXY_CLIENT);
		casClient.setServiceUrl(pac4jCasProperties.getPrefixUrl());
		
		return casClient;
	}
	
	@Bean 
	@ConditionalOnProperty(prefix = Pac4jCasProperties.PREFIX, value = Pac4jClientNames.CAS_REST_BASIC_AUTH_CLIENT, havingValue = "true")
	public CasRestBasicAuthClient casRestBasicAuthClient(CasConfiguration configuration) {
		
		CasRestBasicAuthClient casClient = new CasRestBasicAuthClient();
		
		casClient.setConfiguration(configuration);
		casClient.setName(StringUtils.hasText(pac4jCasProperties.getCasRestBasicAuthClientName()) ? pac4jCasProperties.getCasRestBasicAuthClientName() : Pac4jClientNames.CAS_REST_BASIC_AUTH_CLIENT);
		if(StringUtils.hasText(pac4jCasProperties.getHeaderName())) {	
			casClient.setHeaderName(pac4jCasProperties.getHeaderName());
		}
		if(StringUtils.hasText(pac4jCasProperties.getPrefixHeader())) {	
			casClient.setPrefixHeader(pac4jCasProperties.getPrefixHeader());
		}
		
		return casClient;
		
	}
	
	@Bean
	@ConditionalOnProperty(prefix = Pac4jCasProperties.PREFIX, value = Pac4jClientNames.CAS_REST_FORM_CLIENT, havingValue = "true")
	public CasRestFormClient casRestFormClient(CasConfiguration configuration) {
		
		/*
		 *  通过rest接口可以获取tgt，获取service ticket，甚至可以获取CasProfile
		 */
		CasRestFormClient casClient = new CasRestFormClient();
		
		casClient.setConfiguration(configuration);
		casClient.setName(StringUtils.hasText(pac4jCasProperties.getCasRestFormClientName()) ? pac4jCasProperties.getCasRestFormClientName() : Pac4jClientNames.CAS_REST_FORM_CLIENT);
		if(StringUtils.hasText(pac4jCasProperties.getUsernameParameterName())) {	
			casClient.setUsernameParameter(pac4jCasProperties.getUsernameParameterName());
		}
		if(StringUtils.hasText(pac4jCasProperties.getPasswordParameterName())) {	
			casClient.setPasswordParameter(pac4jCasProperties.getPasswordParameterName());
		}

		return casClient;
	}
	
}
