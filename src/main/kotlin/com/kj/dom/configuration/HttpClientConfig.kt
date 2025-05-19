package com.kj.dom.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class HttpClientConfig {

    @Bean("domRestClient")
    fun domRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("https://dragonsofmugloar.com/api/v2")
            .build()
}