package com.kj.dom.configuration

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ResourcesConfig {

    @Bean
    fun produceLog(injectionPoint: InjectionPoint): Logger =
        LoggerFactory.getLogger(injectionPoint.member.declaringClass.name)


}