package io.spring.workshop.tradingservice;

import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@Controller
public class QuotesController {

    @Value("${quotes.endpoint}")
    private String quotesEndpoint;

	@GetMapping(path = "/quotes/feed", produces = TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public Flux<Quote> quotesStream() {
		return WebClient.create(quotesEndpoint())
				.get()
				.uri("/quotes")
				.accept(APPLICATION_STREAM_JSON)
				.retrieve()
				.bodyToFlux(Quote.class)
				.share()
				.log("io.spring.workshop.tradingservice");
	}

	@GetMapping("/quotes")
	public String quotes() {
		return "quotes";
	}
	
	private String quotesEndpoint() {
	    return quotesEndpoint;
    }
}