package com.trickl.oanda.client;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.pricing.common.PriceStreamMessage;
import com.trickl.oanda.validation.ServerResponseValidator;
import com.trickl.text.oanda.CurrencyPairFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class PriceStreamClient {

  private final WebClient webClient;

  private final String accountId;

  private final boolean validate;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  /**
   * Get a live stream of prices for an instrument.
   *
   * @param instruments Instruments
   * @return A stream of candlesticks
   */
  public Flux<PriceStreamMessage> get(List<CurrencyPair> instruments) {

    String streamEndpoint =
        new MessageFormat("/v3/accounts/{0}/pricing/stream").format(new Object[] {accountId});

    return webClient
        .get()
        .uri(
            builder ->
                builder
                    .path(streamEndpoint)
                    .queryParam("snapshot", true)
                    .queryParam(
                        "instruments",
                        instruments.stream()
                            .map(
                                instrument ->
                                    CurrencyPairFormat.format(
                                        instrument, CurrencyPairFormat.OANDA_FORMAT))
                            .collect(Collectors.toList())
                            .toArray())
                    .build())
        .retrieve()
        .bodyToFlux(PriceStreamMessage.class)
        .doOnNext(
            price -> {
              if (validate) {
                validator.validate(price);
              }
            });
  }
}
