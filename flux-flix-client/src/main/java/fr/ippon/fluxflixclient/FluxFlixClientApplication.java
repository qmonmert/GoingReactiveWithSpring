package fr.ippon.fluxflixclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

@SpringBootApplication
public class FluxFlixClientApplication {

	@Bean
	WebClient client() {
		return WebClient.builder()
				//.filter(ExchangeFilterFunctions.basicAuthentication("john", "pw"))
				.baseUrl("http://localhost:8080/bikes")
				.build();
	}

	@Bean
	CommandLineRunner demo(WebClient client) {
		return args -> {
			client.get()
					.retrieve()
					.bodyToFlux(Bike.class)
					.filter(bike -> bike.getName().equalsIgnoreCase("Canyon"))
					.flatMap(bike -> client.get()
										.uri("/{id}/events", bike.getId())
										.retrieve()
										.bodyToFlux(BikeEvent.class))
					.subscribe(System.out::println);
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(FluxFlixClientApplication.class, args);
	}
}


@Data
@AllArgsConstructor
class Bike {
	private String id;
	private String name;
}


@Data
@AllArgsConstructor
class BikeEvent {
	private String bikeId;
	private Date dateReleased;
}