package fr.ippon.fluxflixservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@SpringBootApplication
public class FluxFlixServiceApplication {

	@Bean
	RouterFunction<ServerResponse> routerFunction(BikeHandler bikeHandler) {
		return route(GET("/bikes"), bikeHandler::all)
				.andRoute(GET("/bikes/{id}"), bikeHandler::byId)
				.andRoute(GET("/bikes/{id}/events"), bikeHandler::events);
	}

	public static void main(String[] args) {
		SpringApplication.run(FluxFlixServiceApplication.class, args);
	}

}


@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
class Bike {
	@Id
	private String id;
	private String name;
}


interface BikeRepository extends ReactiveMongoRepository<Bike, String> {
	Flux<Bike> findByName(String name);
}


@Service
class BikeService {
	private final BikeRepository bikeRepository;

	BikeService(BikeRepository bikeRepository) {
		this.bikeRepository = bikeRepository;
	}

	public  Flux<Bike> getAllBikes() {
		return this.bikeRepository.findAll();
	}

	public Mono<Bike> getBikeById(String id) {
		return this.bikeRepository.findById(id);
	}

	public Flux<BikeEvent> getEvents(String bikeId) {
		return Flux.<BikeEvent>generate(sink -> sink.next(new BikeEvent(bikeId, new Date())))
				.delayElements(Duration.ofSeconds(1));
	}

}


@Data
@NoArgsConstructor
@AllArgsConstructor
class BikeEvent {
	private String bikeId;
	private Date dateReleased;
}


@Component
class BikeHandler {
	private final BikeService bikeService;

	BikeHandler(BikeService bikeService) {
		this.bikeService = bikeService;
	}

	public Mono<ServerResponse> all(ServerRequest request) {
		return ServerResponse.ok()
				.body(bikeService.getAllBikes(), Bike.class);
	}

	public Mono<ServerResponse> byId(ServerRequest request) {
		return ServerResponse.ok()
				.body(bikeService.getBikeById(request.pathVariable("id")), Bike.class);
	}

	public Mono<ServerResponse> events(ServerRequest request) {
		return ServerResponse.ok()
				.contentType(MediaType.TEXT_EVENT_STREAM)
				.body(bikeService.getEvents(request.pathVariable("id")), BikeEvent.class);
	}
}


//@RestController
//@RequestMapping("/bikes")
//class BikeController {
//	private final BikeService bikeService;
//
//	BikeController(BikeService bikeService) {
//		this.bikeService = bikeService;
//	}
//
//	@GetMapping
//	public Flux<Bike> all() {
//		return this.bikeService.getAllBikes();
//	}
//
//	@GetMapping("/{id}")
//	public Mono<Bike> byId(@PathVariable String id) {
//		return this.bikeService.getBikeById(id);
//	}
//
//	@GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//	public Flux<BikeEvent> events(@PathVariable String id) {
//		return this.bikeService.getEvents(id);
//	}
//}


@Component
class SampleDataInitializer implements ApplicationRunner {

	private final BikeRepository bikeRepository;

	SampleDataInitializer(BikeRepository bikeRepository) {
		this.bikeRepository = bikeRepository;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Flux<Bike> bikeFlux = Flux.just("Canyon", "Trek SL5", "Giant", "Trek SL6")
				.map(bikeName -> new Bike(null, bikeName))
				.flatMap(bikeRepository::save);
		bikeRepository.deleteAll()
				.thenMany(bikeFlux)
				.thenMany(bikeRepository.findAll())
				.subscribe(System.out::println);
	}
}