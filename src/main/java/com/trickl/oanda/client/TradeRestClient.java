package com.trickl.oanda.client;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.trade.GetTradesResponse;
import com.trickl.model.oanda.trade.Trade;
import com.trickl.model.oanda.trade.TradeStateFilter;
import com.trickl.oanda.validation.ServerResponseValidator;
import com.trickl.text.oanda.CurrencyPairFormat;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class TradeRestClient {

  private final WebClient webClient;

  private final String accountId;

  private final TransactionIdClient transactionIdClient;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  /**
   * Get a list of Trades that satisfy a id-based Trade query.
   *
   * @param tradeIds List of Trade IDs to retrieve
   * @param tradeStateFilter The state to filter the requested Trades by [default=PENDING]
   * @param instrument The instrument to filter the requested trades by
   * @param limit The maximum number of Trades to return [default=50, maximum=500]
   * @param endingTradeId The ending Trade ID (inclusive) to fetch
   * @return The requested list of trades
   */
  public Flux<Trade> find(
      List<String> tradeIds,
      TradeStateFilter tradeStateFilter,
      CurrencyPair instrument,
      Integer limit,
      String endingTradeId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    if (tradeIds != null) {
      params.add("ids", tradeIds.stream().collect(Collectors.joining(",")));
    }
    if (tradeStateFilter != null) {
      params.add("state", tradeStateFilter.toString());
    }
    if (instrument != null) {
      params.add(
          "instrument", CurrencyPairFormat.format(instrument, CurrencyPairFormat.OANDA_FORMAT));
    }
    if (limit != null) {
      params.add("count", limit.toString());
    }
    if (endingTradeId != null) {
      params.add("beforeID", endingTradeId);
    }

    String tradesEndpoint =
        new MessageFormat("/v3/accounts/{0}/trades").format(new Object[] {accountId});

    return webClient
        .get()
        .uri(builder -> builder.path(tradesEndpoint).queryParams(params).build())
        .retrieve()
        .bodyToMono(GetTradesResponse.class)
        .doOnNext(
          response -> {
            validator.validate(response);
            transactionIdClient.publish(response.getLastTransactionId());
          })
        .flatMapIterable(GetTradesResponse::getTrades);
  }
}
