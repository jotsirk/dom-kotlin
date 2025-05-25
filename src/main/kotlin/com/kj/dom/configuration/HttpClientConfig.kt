package com.kj.dom.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class HttpClientConfig(
  @Value("\${dragons-of-mugloar.base.path}")
  private val baseUrl: String,
) {
  @Bean("domRestClient")
  fun domRestClient(): RestClient =
    RestClient
      .builder()
      .baseUrl(baseUrl)
      .build()
}
