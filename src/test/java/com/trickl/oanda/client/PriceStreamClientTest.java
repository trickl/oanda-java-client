package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.pricing.common.Price;
import com.trickl.model.oanda.pricing.common.PriceStreamMessage;
import com.trickl.model.oanda.pricing.common.PricingHeartbeat;
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
public class PriceStreamClientTest extends BaseRestClientTest {

  private PriceStreamClient priceStreamClient;

  @BeforeEach
  private void setup() {
    startServer();
    priceStreamClient = new PriceStreamClient(webClient, "ACCOUNT", true);
  }

  @AfterEach
  private void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testGet() throws IOException {
    prepareResponse("PriceStream_Get.stream");

    Flux<PriceStreamMessage> flux =
        priceStreamClient.get(
            Arrays.asList(
                new CurrencyPair(Currency.getInstance("EUR"), Currency.getInstance("GBP"))));

    StepVerifier.create(flux)
        .assertNext(
            message -> {
              assertThat(message).isNotNull();
              assertThat(message).isInstanceOf(Price.class);
            })
        .assertNext(
            message -> {
              assertThat(message).isNotNull();
              assertThat(message).isInstanceOf(Price.class);
            })
        .assertNext(
            message -> {
              assertThat(message).isNotNull();
              assertThat(message).isInstanceOf(Price.class);
            })
        .assertNext(
            message -> {
              assertThat(message).isNotNull();
              assertThat(message).isInstanceOf(Price.class);
            })
        .assertNext(
            message -> {
              assertThat(message).isNotNull();
              assertThat(message).isInstanceOf(PricingHeartbeat.class);
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/pricing/stream?snapshot=true&instruments=EUR_GBP");
    expectRequestCount(1);
  }
}
