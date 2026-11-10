package com.administracionback.admonv1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"jwt.secret=" + DrawIntegrationTest.TEST_JWT_SECRET,
		DrawIntegrationTest.TEST_DATA
})
class Admonv1ApplicationTests {

	@Test
	void contextLoads() {
	}

}
