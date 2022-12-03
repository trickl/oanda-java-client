package com.trickl.oanda.client;

import com.trickl.model.oanda.transaction.TransactionStreamMessage;
import com.trickl.oanda.validation.ServerResponseValidator;
import java.text.MessageFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class TransactionStreamClient {

  private final WebClient webClient;

  private final String accountId;

  private final boolean validate;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  /**
   * Get a live stream of transactions for an instrument.
   *
   * @return A stream of transactions
   */
  public Flux<TransactionStreamMessage> get() {

    String streamEndpoint =
        new MessageFormat("/v3/accounts/{0}/transactions/stream").format(new Object[] {accountId});

    return webClient
        .get()
        .uri(streamEndpoint)
        .retrieve()
        .bodyToFlux(TransactionStreamMessage.class)
        .doOnNext(
            price -> {
              if (validate) {
                validator.validate(price);
              }
            });
  }
}
