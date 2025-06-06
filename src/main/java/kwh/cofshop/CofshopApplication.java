package kwh.cofshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CofshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(CofshopApplication.class, args);
	}

}
