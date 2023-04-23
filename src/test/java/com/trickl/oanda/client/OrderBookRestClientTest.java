package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.instrument.OrderBook;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@ActiveProfiles({"unittest"})
@SpringBootTest(classes = OandaConfiguration.class)
public class OrderBookRestClientTest extends BaseRestClientTest {

  private OrderBookRestClient orderBookRestClient;

  @BeforeEach
  public void setup() {
    startServer();
    orderBookRestClient = new OrderBookRestClient(webClient);
  }

  @AfterEach
  public void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testFindLastBefore() throws IOException {
    prepareResponse("OrderBook_FindLastBefore.json");

    Mono<OrderBook> flux =
        orderBookRestClient.findLastBefore(
            new CurrencyPair(Currency.getInstance("EUR"), Currency.getInstance("GBP")),
            Instant.parse("2007-12-13T12:15:30.00Z"));

    StepVerifier.create(flux)
        .assertNext(
            orderBook -> {
              assertThat(orderBook).isNotNull();
              assertThat(orderBook.getTime()).isEqualTo(Instant.parse("2017-01-10T14:40:00Z"));
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/instruments/EUR_GBP/orderBook?time=2007-12-13T12:00:00Z");
    expectRequestCount(1);
  }
}
