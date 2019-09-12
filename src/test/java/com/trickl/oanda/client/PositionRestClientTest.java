package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.position.Position;
import com.trickl.oanda.config.OandaConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.Currency;
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
public class PositionRestClientTest extends BaseRestClientTest {

  private PositionRestClient positionRestClient;

  @BeforeEach
  private void setup() {
    startServer();
    positionRestClient = new PositionRestClient(webClient, "ACCOUNT", new TransactionIdClient());
  }

  @AfterEach
  private void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testFindByInstrumentId() throws IOException {
    prepareResponse("Position_FindByInstrumentId.json");

    Mono<Position> flux =
        positionRestClient.findByInstrumentId(
            new CurrencyPair(Currency.getInstance("EUR"), Currency.getInstance("USD")));

    StepVerifier.create(flux)
        .assertNext(
            position -> {
              assertThat(position).isNotNull();
              assertThat(position.getInstrument()).isEqualTo("EUR_USD");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/positions/EUR_USD");
    expectRequestCount(1);
  }

  @Test
  public void testFindAll() throws IOException {
    prepareResponse("Position_FindAll.json");

    Flux<Position> flux = positionRestClient.findAll();

    StepVerifier.create(flux)
        .assertNext(
            position -> {
              assertThat(position).isNotNull();
              assertThat(position.getInstrument()).isEqualTo("CHF_JPY");
            })
        .assertNext(
            position -> {
              assertThat(position).isNotNull();
              assertThat(position.getInstrument()).isEqualTo("AUD_JPY");
            })
        .expectNextCount(11)
        .expectComplete()
        .verify(Duration.ofSeconds(3));

    expectPath("/v3/accounts/ACCOUNT/positions");
    expectRequestCount(1);
  }
}
