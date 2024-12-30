package kwh.cofshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class CofshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(CofshopApplication.class, args);
	}

}
