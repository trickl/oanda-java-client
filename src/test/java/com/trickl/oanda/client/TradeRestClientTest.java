package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.trade.Trade;
import com.trickl.model.oanda.trade.TradeStateFilter;
import com.trickl.oanda.config.OandaConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
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
public class TradeRestClientTest extends BaseRestClientTest {

  private TradeRestClient tradeRestClient;

  @BeforeEach
  public void setup() {
    startServer();
    tradeRestClient = new TradeRestClient(webClient, "ACCOUNT", new TransactionIdClient());
  }

  @AfterEach
  public void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testFind() throws IOException {
    prepareResponse("Trade_Find.json");

    Flux<Trade> flux =
        tradeRestClient.find(
            Arrays.asList("123, 124"),
            TradeStateFilter.ALL,
            new CurrencyPair(Currency.getInstance("EUR"), Currency.getInstance("GBP")),
            5,
            "131");

    StepVerifier.create(flux)
        .assertNext(
            trade -> {
              assertThat(trade).isNotNull();
              assertThat(trade.getId()).isEqualTo("6397");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/trades?ids=123,%20124" 
        + "&state=ALL&instrument=EUR_GBP&count=5&beforeID=131");
    expectRequestCount(1);
  }
}
