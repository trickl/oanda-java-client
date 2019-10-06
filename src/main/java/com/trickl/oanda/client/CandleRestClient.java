package com.trickl.oanda.client;

import com.trickl.model.oanda.instrument.Candlestick;
import com.trickl.model.oanda.instrument.CandlestickGranularity;
import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.instrument.GetCandlesResponse;
import com.trickl.oanda.validation.ServerResponseValidator;
import com.trickl.text.oanda.CurrencyPairFormat;
import com.trickl.text.oanda.Rfc3339;

import java.text.MessageFormat;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class CandleRestClient {

  private final WebClient webClient;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  /**
   * Find candles for an instrument.
   *
   * @param instrument Instrument Id
   * @param start Start date
   * @param end End date
   * @return A list of candlesticks
   */
  public Flux<Candlestick> findBetween(CurrencyPair instrument, Instant start, Instant end) {

    String candlesEndpoint =
        new MessageFormat("/v3/instruments/{0}/candles")
            .format(new Object[] {CurrencyPairFormat.format(instrument, "{0}_{1}")});

    return webClient
        .get()
        .uri(
            builder ->
                builder
                    .path(candlesEndpoint)
                    .queryParam("from", Rfc3339.YMDHMSN_FORMATTER.format(start))
                    .queryParam("to", Rfc3339.YMDHMSN_FORMATTER.format(end))
                    .queryParam("granularity", CandlestickGranularity.M1)
                    .build())
        .retrieve()
        .bodyToMono(GetCandlesResponse.class)
        .doOnNext(validator::validate)
        .flatMapIterable(GetCandlesResponse::getCandles);
  }
}
