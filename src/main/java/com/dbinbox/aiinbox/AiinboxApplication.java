package com.dbinbox.aiinbox;

import com.dbinbox.aiinbox.model.Product;
import com.dbinbox.aiinbox.repository.ProductRepo;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@Transactional
public class AiinboxApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiinboxApplication.class, args);
	}
	@Bean
	CommandLineRunner initProducts(ProductRepo repo) {
		return args -> {
			repo.save(new Product("BAKE-01", "Matcha Whisk", 15, 10, ""));
			repo.save(new Product("BOOK-05", "The Cozy Baker", 25, 0, ""));
		};
	}
}
