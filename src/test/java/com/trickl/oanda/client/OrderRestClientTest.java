package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.order.CreateOrderResponse;
import com.trickl.model.oanda.order.MarketOrder;
import com.trickl.model.oanda.order.Order;
import com.trickl.model.oanda.order.OrderStateFilter;
import com.trickl.model.oanda.order.TimeInForce;
import com.trickl.oanda.config.OandaConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.Currency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@ActiveProfiles({"unittest"})
@SpringBootTest(classes = OandaConfiguration.class)
public class OrderRestClientTest extends BaseRestClientTest {

  private OrderRestClient orderRestClient;

  @BeforeEach
  public void setup() {
    startServer();
    orderRestClient = new OrderRestClient(webClient, "ACCOUNT", new TransactionIdClient());
  }

  @AfterEach
  public void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testFind() throws IOException {
    prepareResponse("Order_Find.json");

    Flux<Order> flux =
        orderRestClient.find(
            null,
            OrderStateFilter.ALL,
            new CurrencyPair(Currency.getInstance("EUR"), Currency.getInstance("GBP")),
            null,
            null);

    StepVerifier.create(flux)
        .assertNext(
            order -> {
              assertThat(order).isNotNull();
              assertThat(order.getId()).isEqualTo("6375");
            })
        .assertNext(
            order -> {
              assertThat(order).isNotNull();
              assertThat(order.getId()).isEqualTo("6376");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/orders?state=ALL&instrument=EUR_GBP");
    expectRequestCount(1);
  }

  @Test
  public void testCreate() throws IOException {
    prepareResponse("Order_Create.json");

    Mono<CreateOrderResponse> flux =
        orderRestClient.create(
            MarketOrder.builder().timeInForce(TimeInForce.FOK).instrument("EUR_GBP").build());

    StepVerifier.create(flux)
        .assertNext(
            response -> {
              assertThat(response).isNotNull();
              assertThat(response.getOrderCreateTransaction()).isNotNull();
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectRequest("/v3/accounts/ACCOUNT/orders", HttpMethod.POST);
    expectRequestCount(1);
  }
}
