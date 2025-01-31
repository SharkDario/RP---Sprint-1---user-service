package com.mindhub.user_service;

import com.mindhub.user_service.dtos.NewEntityUser;
import com.mindhub.user_service.services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(UserService userService) {
		return args -> {
			NewEntityUser user = new NewEntityUser("Miguel7", "12345678", "dario@gmail.com");
			userService.registerAdmin(user);
		};
	}
}
