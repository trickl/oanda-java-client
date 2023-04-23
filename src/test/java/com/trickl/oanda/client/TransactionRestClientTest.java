package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.transaction.MarketOrderTransaction;
import com.trickl.model.oanda.transaction.OrderFillTransaction;
import com.trickl.model.oanda.transaction.Transaction;
import com.trickl.model.oanda.transaction.TransactionFilter;
import com.trickl.oanda.config.OandaConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@ActiveProfiles({"unittest"})
@SpringBootTest(classes = OandaConfiguration.class)
public class TransactionRestClientTest extends BaseRestClientTest {

  private TransactionRestClient transactionRestClient;

  @BeforeEach
  public void setup() {
    startServer();
    transactionRestClient =
        new TransactionRestClient(webClient, "ACCOUNT", new TransactionIdClient());
  }

  @AfterEach
  public void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testFindByTransactionId() throws IOException {
    prepareResponse("Transaction_FindByTransactionId.json");

    Mono<Transaction> flux = transactionRestClient.findByTransactionId("6410");

    StepVerifier.create(flux)
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(OrderFillTransaction.class);
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/transactions/6410");
    expectRequestCount(1);
  }

  @Test
  public void testFindByIdBetweenAndFilterIn() throws IOException {
    prepareResponse("Transaction_FindByIdBetweenAndFilterIn.json");

    Flux<Transaction> flux =
        transactionRestClient.findByIdBetweenAndFilterIn(
            "6409",
            "6412",
            Arrays.asList(TransactionFilter.MARKET_ORDER, TransactionFilter.LIMIT_ORDER));

    StepVerifier.create(flux)
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(MarketOrderTransaction.class);
              assertThat(transaction.getId()).isEqualTo("6409");
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(OrderFillTransaction.class);
              assertThat(transaction.getId()).isEqualTo("6410");
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(MarketOrderTransaction.class);
              assertThat(transaction.getId()).isEqualTo("6411");
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(OrderFillTransaction.class);
              assertThat(transaction.getId()).isEqualTo("6412");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/transactions/idrange" 
        + "?from=6409&to=6412&type=MARKET_ORDER,LIMIT_ORDER");
    expectRequestCount(1);
  }

  @Test
  public void testFindByCreatedBetweenAndFilterIn() throws IOException {
    prepareResponse("Transaction_FindByCreatedBetweenAndFilterIn.json");
    prepareResponse("Transaction_FindByIdBetweenAndFilterIn.json");

    Flux<Transaction> flux =
        transactionRestClient.findByCreatedBetweenAndFilterIn(
            Instant.parse("2017-01-10T14:40:00Z"),
            Instant.parse("2017-01-13T16:40:00Z"),
            Arrays.asList(TransactionFilter.MARKET_ORDER, TransactionFilter.LIMIT_ORDER),
            0,
            25);

    StepVerifier.create(flux)
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(MarketOrderTransaction.class);
              assertThat(transaction.getId()).isEqualTo("6409");
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(OrderFillTransaction.class);
              assertThat(transaction.getId()).isEqualTo("6410");
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(MarketOrderTransaction.class);
              assertThat(transaction.getId()).isEqualTo("6411");
            })
        .assertNext(
            transaction -> {
              assertThat(transaction).isNotNull();
              assertThat(transaction).isInstanceOf(OrderFillTransaction.class);
              assertThat(transaction.getId()).isEqualTo("6412");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/transactions"
        + "?from=2017-01-10T14:40:00.000000000Z&to=2017-01-13T16:40:00.000000000Z"
        + "&type=MARKET_ORDER,LIMIT_ORDER&pageSize=25");
    expectPath("/v3/accounts/ACCOUNT/transactions/idrange?from=6409&to=6412");
    expectRequestCount(2);
  }
}
