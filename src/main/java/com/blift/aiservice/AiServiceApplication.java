package com.blift.aiservice;

import com.blift.common.config.FeignClientInterceptor;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@Import(FeignClientInterceptor.class)
public class AiServiceApplication {

    /**
     * Explicit connect/read/write timeouts. Without them the OpenAI call relied solely
     * on the caller's .block() timeout, so a stalled connection could pin a request
     * thread for as long as the OS kept the socket open.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(45))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(45, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(45, TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
