package com.homie.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
		info = @Info(title = "App Quản Lý Tài Chính của Homie", version = "1.0"),
		security = @SecurityRequirement(name = "bearerAuth") // Yêu cầu mọi API phải có chìa khóa này
)
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT"
)
@SpringBootApplication
public class Application {
	private static final Logger log = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(Application.class, args);

		String port = context.getEnvironment().getProperty("server.port");
		if (port == null) port = "8080"; // Mặc định nếu chưa đổi port

		// Dòng này sẽ tạo ra link xanh mượt ở Console nè homie
		log.info("\n----------------------------------------------------------\n" +
				"\tApplication is running! Access URLs:\n" +
				"\tLocal: \t\thttp://localhost:" + port + "/\n" +
				"\tSwagger UI: \thttp://localhost:" + port + "/swagger-ui/index.html\n" +
				"----------------------------------------------------------");
	}
}