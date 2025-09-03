package kr.hhplus.be.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ServerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void assertJTest() {
		// JUnit + AssertJ가 제대로 동작하는지 테스트
		String actual = "Hello Spring Boot";
		assertThat(actual)
			.isNotNull()
			.isEqualTo("Hello Spring Boot")
			.startsWith("Hello")
			.endsWith("Boot");
	}

}
