package com.trickl.oanda.client;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.instrument.GetOrderBookResponse;
import com.trickl.model.oanda.instrument.OrderBook;
import com.trickl.oanda.text.CurrencyPairFormat;
import com.trickl.oanda.text.Rfc3339;
import com.trickl.oanda.validation.ServerResponseValidator;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class OrderBookRestClient {

  private final WebClient webClient;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  private static final Duration QUOTE_ALIGNMENT = Duration.ofMinutes(20);

  /**
   * Get the last order book for an instrument before a certain time.
   *
   * @param instrument the instrument to query
   * @param endExcl the last time (now if null)
   * @return An order book
   */
  public Mono<OrderBook> findLastBefore(CurrencyPair instrument, Instant endExcl) {

    String orderBookEndpoint =
        new MessageFormat("/v3/instruments/{0}/orderBook")
            .format(new Object[] {CurrencyPairFormat.format(instrument, "{0}_{1}")});

    Optional<Instant> endAlignedExcl = Optional.empty();
    if (endExcl != null) {
      long endAlignedExclMs = endExcl.toEpochMilli();
      endAlignedExclMs =
          (endAlignedExclMs / QUOTE_ALIGNMENT.toMillis()) * QUOTE_ALIGNMENT.toMillis();
      endAlignedExcl = Optional.of(Instant.ofEpochMilli(endAlignedExclMs));
    }

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    if (endAlignedExcl.isPresent()) {
      params.add("time", Rfc3339.YMDHMS_FORMATTER.format(endAlignedExcl.get()));
    }

    return webClient
        .get()
        .uri(builder -> builder.path(orderBookEndpoint).queryParams(params).build())
        .header(HttpHeaders.ACCEPT_ENCODING, "application/gzip")
        .retrieve()
        .bodyToMono(GetOrderBookResponse.class)
        .doOnNext(validator::validate)
        .map(GetOrderBookResponse::getOrderBook);
  }
}
