package com.payhub.gateway.infrastructure.routing;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GatewayRouteConfig {

    @Bean
    RouterFunction<ServerResponse> payhubRoutes(
            @Value("${payhub.gateway.routes.ops-bff}") String opsBff,
            @Value("${payhub.gateway.routes.merchant-service}") String merchantService,
            @Value("${payhub.gateway.routes.payment-orchestrator}") String paymentOrchestrator
    ) {
        return route("ops-bff")
                .route(path("/ops/**"), http())
                .before(uri(opsBff))
                .build()
                .and(route("merchant-service")
                        .route(path("/api/v1/merchants/**"), http())
                        .before(uri(merchantService))
                        .build())
                .and(route("payment-orchestrator")
                        .route(path("/api/v1/payments/**"), http())
                        .before(uri(paymentOrchestrator))
                        .build());
    }
}
