package com.example.demo.config;

import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Configuration
public class ActApiConfig {

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();

	}

	@Bean
	@Qualifier(value = "connectionContextNPE")
	DefaultConnectionContext connectionContextNPE(@Value("${cf.npe.apiHost}") String apiHost) {
		return DefaultConnectionContext.builder().apiHost(apiHost).build();
	}

	@Bean
	@Qualifier(value = "tokenProviderNPE")
	PasswordGrantTokenProvider tokenProviderNPE(@Value("${cf.username}") String username,
			@Value("${cf.password}") String password) {
		return PasswordGrantTokenProvider.builder().password(password).username(username).build();
	}

	@Bean
	@Qualifier(value = "cloudFoundryClientNPE")
	ReactorCloudFoundryClient cloudFoundryClientNPE(ConnectionContext connectionContextNPE,
			TokenProvider tokenProviderNPE) {
		return ReactorCloudFoundryClient.builder().connectionContext(connectionContextNPE)
				.tokenProvider(tokenProviderNPE).build();
	}

	@Bean
	@Qualifier(value = "connectionContextProd")
	DefaultConnectionContext connectionContextProd(@Value("${cf.prod.apiHost}") String apiHost) {
		return DefaultConnectionContext.builder().apiHost(apiHost).build();
	}

	@Bean
	@Qualifier(value = "tokenProviderProd")
	PasswordGrantTokenProvider tokenProviderProd(@Value("${cf.username}") String username,
			@Value("${cf.password}") String password) {
		return PasswordGrantTokenProvider.builder().password(password).username(username).build();
	}

	@Bean
	@Qualifier(value = "cloudFoundryClientProd")
	ReactorCloudFoundryClient cloudFoundryClientProd(ConnectionContext connectionContextProd,
			TokenProvider tokenProviderProd) {
		return ReactorCloudFoundryClient.builder().connectionContext(connectionContextProd)
				.tokenProvider(tokenProviderProd).build();
	}

	@Bean
	public CacheManager cacheManager(Caffeine caffeine) {
		CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
		caffeineCacheManager.getCache("getCfData");
		caffeineCacheManager.setCaffeine(caffeine);
		return caffeineCacheManager;
	}

	@Bean
	public Caffeine caffeineConfig() {
		return Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS);
//                expireAfterWrite(24, TimeUnit.HOURS);
	}

	@Bean
	public HttpClient getClient() {
		HttpClient client = HttpClientBuilder.create().build();
		try {
		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(null, (chain, authType) -> true);
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
				NoopHostnameVerifier.INSTANCE);
		client = HttpClients.custom().setSSLSocketFactory(sslsf).build();
		}
		catch (Exception e) {
			log.error("Exception ingetClient: "+e.getMessage());
		}
		return client;
	}
}
