package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.account.Account;
import com.trickl.model.oanda.account.AccountChanges;
import com.trickl.model.oanda.account.AccountProperties;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@ActiveProfiles({"unittest"})
@SpringBootTest(classes = OandaConfiguration.class)
public class AccountRestClientTest extends BaseRestClientTest {

  private AccountRestClient accountRestClient;

  @BeforeEach
  public void setup() {
    startServer();
    accountRestClient = new AccountRestClient(webClient, "ACCOUNT", new TransactionIdClient());
  }

  @AfterEach
  public void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testGet() throws IOException {
    prepareResponse("Account_Get.json");

    Mono<Account> flux = accountRestClient.get();

    StepVerifier.create(flux)
        .assertNext(
            account -> {
              assertThat(account).isNotNull();
              assertThat(account.getId()).isEqualTo("<ACCOUNT>");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT");
  }

  @Test
  public void testFindById() throws IOException {
    prepareResponse("Account_FindById.json");

    Mono<Account> flux = accountRestClient.findById("ACCOUNT_2");

    StepVerifier.create(flux)
        .assertNext(
            account -> {
              assertThat(account).isNotNull();
              assertThat(account.getId()).isEqualTo("<ACCOUNT_2>");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT_2");
    expectRequestCount(1);
  }

  @Test
  public void testFindChangesSince() throws IOException {
    prepareResponse("Account_FindChangesSince.json");

    Mono<AccountChanges> flux = accountRestClient.findChangesSince("123");

    StepVerifier.create(flux)
        .assertNext(
            changes -> {
              assertThat(changes).isNotNull();
              assertThat(changes.getOrdersTriggered()).isEmpty();
              assertThat(changes.getOrdersFilled()).isNotEmpty();
              assertThat(changes.getPositions()).isNotEmpty();
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/changes?sinceTransactionID=123");
    expectRequestCount(1);
  }

  @Test
  public void testFindAll() throws IOException {
    prepareResponse("Account_FindAll.json");

    Flux<AccountProperties> flux = accountRestClient.findAll();

    StepVerifier.create(flux)
        .assertNext(
            account -> {
              assertThat(account).isNotNull();
              assertThat(account.getId()).isEqualTo("<ACCOUNT_A>");
            })
        .assertNext(
            account -> {
              assertThat(account).isNotNull();
              assertThat(account.getId()).isEqualTo("<ACCOUNT_B>");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts");
    expectRequestCount(1);
  }
}
