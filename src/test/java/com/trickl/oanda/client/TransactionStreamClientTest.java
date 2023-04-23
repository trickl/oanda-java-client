package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.transaction.MarketOrderTransaction;
import com.trickl.model.oanda.transaction.OrderFillTransaction;
import com.trickl.model.oanda.transaction.TransactionHeartbeat;
import com.trickl.model.oanda.transaction.TransactionStreamMessage;
import com.trickl.oanda.config.OandaConfiguration;
import java.io.IOException;
import java.time.Duration;
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
public class TransactionStreamClientTest extends BaseRestClientTest {

  private TransactionStreamClient transactionStreamClient;

  @BeforeEach
  public void setup() {
    startServer();
    transactionStreamClient = new TransactionStreamClient(webClient, "ACCOUNT", true);
  }

  @AfterEach
  public void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testGet() throws IOException {
    prepareResponse("TransactionStream_Get.stream");

    Flux<TransactionStreamMessage> flux = transactionStreamClient.get();

    StepVerifier.create(flux)
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(TransactionHeartbeat.class);
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(MarketOrderTransaction.class);
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(OrderFillTransaction.class);
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(TransactionHeartbeat.class);
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/transactions/stream");
    expectRequestCount(1);
  }
}
