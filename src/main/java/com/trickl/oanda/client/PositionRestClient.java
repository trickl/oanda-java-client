package com.trickl.oanda.client;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.position.GetPositionResponse;
import com.trickl.model.oanda.position.GetPositionsResponse;
import com.trickl.model.oanda.position.Position;
import com.trickl.oanda.text.CurrencyPairFormat;
import com.trickl.oanda.validation.ServerResponseValidator;
import java.text.MessageFormat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class PositionRestClient {

  private final WebClient webClient;

  private final String accountId;

  private final TransactionIdClient transactionIdClient;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  /**
   * Get the position for an account and instrument.
   *
   * @param instrument instrument identifier
   * @return The requested list of positions
   */
  public Mono<Position> findByInstrumentId(@NonNull CurrencyPair instrument) {

    String positionsEndpoint =
        new MessageFormat("/v3/accounts/{0}/positions/{1}")
            .format(
                new Object[] {
                  accountId, CurrencyPairFormat.format(instrument, CurrencyPairFormat.OANDA_FORMAT)
                });

    return webClient
        .get()
        .uri(positionsEndpoint)
        .retrieve()
        .bodyToMono(GetPositionResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            })
        .map(GetPositionResponse::getPosition);
  }

  /**
   * Get a list of Positions for an account.
   *
   * @return The requested list of positions
   */
  public Flux<Position> findAll() {

    String positionsEndpoint =
        new MessageFormat("/v3/accounts/{0}/positions").format(new Object[] {accountId});

    return webClient
        .get()
        .uri(positionsEndpoint)
        .retrieve()
        .bodyToMono(GetPositionsResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            })
        .flatMapIterable(GetPositionsResponse::getPositions);
  }
}
