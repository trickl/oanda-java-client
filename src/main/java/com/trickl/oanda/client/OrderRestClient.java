package com.trickl.oanda.client;

import com.trickl.model.oanda.instrument.CurrencyPair;
import com.trickl.model.oanda.order.CreateOrderResponse;
import com.trickl.model.oanda.order.GetOrdersResponse;
import com.trickl.model.oanda.order.Order;
import com.trickl.model.oanda.order.OrderStateFilter;
import com.trickl.oanda.text.CurrencyPairFormat;
import com.trickl.oanda.validation.ServerResponseValidator;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class OrderRestClient {

  private final WebClient webClient;

  private final String accountId;

  private final TransactionIdClient transactionIdClient;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  /**
   * Get a list of Orders that satisfy a id-based Order query.
   *
   * @param orderIds List of Order IDs to retrieve
   * @param orderStateFilter The state to filter the requested Orders by [default=PENDING]
   * @param instrument The instrument to filter the requested orders by
   * @param limit The maximum number of Orders to return [default=50, maximum=500]
   * @param endingOrderId The ending Order ID (inclusive) to fetch
   * @return The requested list of orders
   */
  public Flux<Order> find(
      List<String> orderIds,
      OrderStateFilter orderStateFilter,
      CurrencyPair instrument,
      Integer limit,
      String endingOrderId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    if (orderIds != null) {
      params.add("ids", orderIds.stream().collect(Collectors.joining(",")));
    }
    if (orderStateFilter != null) {
      params.add("state", orderStateFilter.toString());
    }
    if (instrument != null) {
      params.add(
          "instrument", CurrencyPairFormat.format(instrument, CurrencyPairFormat.OANDA_FORMAT));
    }
    if (limit != null) {
      params.add("count", limit.toString());
    }
    if (endingOrderId != null) {
      params.add("beforeID", endingOrderId);
    }

    String ordersEndpoint =
        new MessageFormat("/v3/accounts/{0}/orders").format(new Object[] {accountId});

    return webClient
        .get()
        .uri(builder -> builder.path(ordersEndpoint).queryParams(params).build())
        .retrieve()
        .bodyToMono(GetOrdersResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            })
        .flatMapIterable(GetOrdersResponse::getOrders);
  }

  /**
   * Create an order for an account.
   *
   * @param order The order
   * @return The server response
   */
  public Mono<CreateOrderResponse> create(Order order) {

    String ordersEndpoint =
        new MessageFormat("/v3/accounts/{0}/orders").format(new Object[] {accountId});

    HashMap<String, Object> orderWrapper = new HashMap<>();
    orderWrapper.put("order", order);

    return webClient
        .post()
        .uri(ordersEndpoint)
        .body(BodyInserters.fromObject(orderWrapper))
        .retrieve()
        .bodyToMono(CreateOrderResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            });
  }
}
