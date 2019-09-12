package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.instrument.Candlestick;
import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.oanda.config.OandaConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@ActiveProfiles({"unittest"})
@SpringBootTest(classes = OandaConfiguration.class)
public class CandleRestClientTest extends BaseRestClientTest {

  private CandleRestClient candleRestClient;

  @BeforeEach
  private void setup() {
    startServer();
    candleRestClient = new CandleRestClient(webClient);
  }

  @AfterEach
  private void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testFindBetween() throws IOException {
    prepareResponse("Candle_FindBetween.json");

    Flux<Candlestick> flux =
        candleRestClient.findBetween(
            new CurrencyPair(Currency.getInstance("EUR"), Currency.getInstance("USD")),
            Instant.parse("2007-12-03T10:15:30.00Z"),
            Instant.parse("2007-12-13T12:15:30.00Z"));

    StepVerifier.create(flux)
        .assertNext(
            candle -> {
              assertThat(candle).isNotNull();
              assertThat(candle.getTime())
                  .isEqualTo(Instant.parse("2016-10-17T15:16:40.000000000Z"));
            })
        .assertNext(
            candle -> {
              assertThat(candle).isNotNull();
              assertThat(candle.getTime())
                  .isEqualTo(Instant.parse("2016-10-17T15:16:45.000000000Z"));
            })
        .expectNextCount(4)
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath(
        "/v3/instruments/EUR_USD/candles"
            + "?from=2007-12-03T10:15:30.000000000Z" 
            + "&to=2007-12-13T12:15:30.000000000Z&granularity=M1");
    expectRequestCount(1);
  }
}
