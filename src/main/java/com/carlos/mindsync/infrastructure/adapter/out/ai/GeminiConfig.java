package com.carlos.mindsync.infrastructure.adapter.out.ai;

import com.carlos.mindsync.application.port.out.AiAdvisorPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
class GeminiConfig {

	@Bean
	AiAdvisorPort aiAdvisorPort(GeminiProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(properties.readTimeout());
		// One reusable client: connections are pooled instead of rebuilt on every call.
		RestClient client = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
		return new GeminiAdapter(client, properties);
	}
}
