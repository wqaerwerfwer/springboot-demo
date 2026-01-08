package com.wfql.springbootdemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * swagger 配置文件
 */
@Configuration
public class SwaggerConfiguration {

    @Value("${server.port}")
    private String port;

    private static InetAddress localHost = null;

    static {
        try {
            localHost =  InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 管理系统")
                        .version("1.0.0")
                        .description("接口文档")
                        .termsOfService("http://"+localHost+":"+port));
    }

    @Bean
    public GroupedOpenApi collection() {
        String[] paths = {"/**",};
        String[] packagedToMatch = {"com.wfql.springbootdemo.controller"};
        return GroupedOpenApi.builder()
                .group("设备采集系统")
                .pathsToMatch(paths)
                .packagesToScan(packagedToMatch).build();
    }

}