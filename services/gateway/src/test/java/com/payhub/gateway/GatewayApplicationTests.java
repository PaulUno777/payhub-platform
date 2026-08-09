package com.payhub.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestJwtDecoderConfig.class)
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
