# SpringOne Reactive Web Demo

<img src="https://github.com/Pivotal-Field-Engineering/reactive-web-demo/blob/master/docs/header-image.png">

This is a fork of Brian Clozel's excellent Spring Webflux Workshop, found here: https://github.com/bclozel/webflux-workshop
A code-level walkthrough of this project can be found here: https://bclozel.github.io/webflux-workshop/

This version is intended for deployment at demo stations at SpringOne, and to showcase Reactive Web with non-blocking I/O. It is installed in the following locations:
<br><br>Org: **S1Pdemo5**
<br>Space: **reactive-web-demo**
<hr>

## Demo components

The demo consists of two Spring Boot applications, and the source code should be loaded into IntelliJ on the SpringOne demo station. The apps are running on PWS, but you can also build and run locally if you like.

The **stock-quotes** service generates a Flux stream of stock quote data, and exposes it on the `/quotes` endpoint. You can see the stream in the app logs as it is being generated. The key source code begins on line 40 of **QuoteGenerator.java**

```java
    public Flux<Quote> fetchQuoteStream(Duration period) {
        // We use here Flux.generate to create quotes,
        // iterating on each stock starting at index 0
        return Flux.generate(() -> 0,
                (BiFunction<Integer, SynchronousSink<Quote>, Integer>) (index, sink) -> {
                    Quote updatedQuote = updateQuote(this.prices.get(index));
                    sink.next(updatedQuote);
                    return ++index % this.prices.size();
                })
                // We want to emit them with a specific period;
                // to do so, we zip that Flux with a Flux.interval
                .zipWith(Flux.interval(period)).map(Tuple2::getT1)
                // Because values are generated in batches,
                // we need to set their timestamp after their creation
                .map(quote -> {
                    quote.setInstant(Instant.now());
                    return quote;
                })
                .log("io.spring.workshop.stockquotes");
    }
```

The **trader-service** web app consumes data from the stock-quotes service and allows the Javascript to consume it as a Flux stream at the `/feed` endpoint. The key code is ``QuotesController's`` ``quotesStream()`` method.

```java
    // program arg: --quotes.endpoint=https://localhost:8081
    // ENV var: QUOTES_ENDPOINT https://localhost:8081
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
```

## Visualizing the stream

Point your browser at http://trading-service.cfapps.io/quotes and you can watch the data plotted live with non-blocking IO. If you build and run locally, go to http://localhost:8080/quotes instead.

## Pushing to PWS

Each Spring Boot project (stock-quotes, trading-service) contains a [manifest and vars file](https://docs.cloudfoundry.org/devguide/deploy-apps/manifest.html#variable-substitution) used to push to PWS.

1. Sign up for [PWS](https://run.pivotal.io/)
2. [Install](https://docs.run.pivotal.io/cf-cli/install-go-cli.html) cf-cli
3. [Login](https://docs.run.pivotal.io/cf-cli/getting-started.html#login) to cf

### Push Stock Quotes

cd into stock-quotes then run this cf-cli command

```
# default app.route=stock-quotes.cfapps.io
> cf push --vars-file ./vars.yml --var app.route=#change-me.cfapps.io
```

### Push Trading Service

cd into trading-service then run this cf-cli command

```
# default app.route=trading-service.cfapps.io
# env-val-1 sets value for QUOTES_ENDPOINT, default is https://stock-quotes.cfapps.io
> cf push --vars-file ./vars.yml \
  --var app.route=#change-me.cfapps.io \
  --var env-val-1=#route-to-stock-quotes
```
