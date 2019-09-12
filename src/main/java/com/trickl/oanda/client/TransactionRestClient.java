package com.trickl.oanda.client;

import com.trickl.model.oanda.transaction.GetTransactionResponse;
import com.trickl.model.oanda.transaction.GetTransactionsByCreatedBetweenResponse;
import com.trickl.model.oanda.transaction.GetTransactionsByIdsBetweenResponse;
import com.trickl.model.oanda.transaction.Transaction;
import com.trickl.model.oanda.transaction.TransactionFilter;
import com.trickl.oanda.text.Rfc3339;
import com.trickl.oanda.validation.ServerResponseValidator;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class TransactionRestClient {

  private final WebClient webClient;

  private final String accountId;

  private final TransactionIdClient transactionIdClient;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  private static final int DEFAULT_PAGE_SIZE = 1000;

  /**
   * Get the details of a single account transaction.
   *
   * @param transactionId The Transaction ID
   * @return A transaction, if it exists
   */
  public Mono<Transaction> findByTransactionId(String transactionId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

    String transactionEndpoint =
        new MessageFormat("/v3/accounts/{0}/transactions/{1}")
            .format(new Object[] {accountId, transactionId});

    return webClient
        .get()
        .uri(builder -> builder.path(transactionEndpoint).queryParams(params).build())
        .retrieve()
        .bodyToMono(GetTransactionResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            })
        .map(GetTransactionResponse::getTransaction);
  }

  /**
   * Get a list of Transactions that satisfy a time-based Transaction query.
   *
   * @param start Start date
   * @param end End date
   * @param transactionFilters A filter for restricting the types of Transactions to retrieve
   * @param pageNumber the starting page number
   * @param pageSize the number of items per page
   * @return The request time range of transactions
   */
  public Flux<Transaction> findByCreatedBetweenAndFilterIn(
      Instant start,
      Instant end,
      List<TransactionFilter> transactionFilters,
      int pageNumber,
      int pageSize) {

    GetTransactionsByCreatedBetweenResponse response =
        findByCreatedBetweenAndFilterIn(start, end, transactionFilters, pageSize).block();

    if (pageNumber >= response.getPages().size()) {
      return Flux.empty();
    }

    String pageEndpoint = response.getPages().get(pageNumber);
    return findUsingEndpoint(pageEndpoint);
  }

  private Mono<GetTransactionsByCreatedBetweenResponse> findByCreatedBetweenAndFilterIn(
      Instant start, Instant end, List<TransactionFilter> filters, int pageSize) {

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    if (start != null) {
      params.add("from", Rfc3339.YMDHMSN_FORMATTER.format(start));
    }
    if (end != null) {
      params.add("to", Rfc3339.YMDHMSN_FORMATTER.format(end));
    }
    if (filters != null) {
      String typeList = filters.stream().map(Object::toString).collect(Collectors.joining(","));
      params.add("type", typeList);
    }
    params.add("pageSize", Optional.ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE).toString());

    String transactionsEndpoint =
        new MessageFormat("/v3/accounts/{0}/transactions").format(new Object[] {accountId});

    return webClient
        .get()
        .uri(builder -> builder.path(transactionsEndpoint).queryParams(params).build())
        .retrieve()
        .bodyToMono(GetTransactionsByCreatedBetweenResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            });
  }

  /**
   * Get a list of Transactions that satisfy a id-based Transaction query.
   *
   * @param startingTransactionId The starting Transaction ID (inclusive) to fetch
   * @param endingTransactionId The ending Transaction ID (inclusive) to fetch
   * @param filters A filter for restricting the types of Transactions to retrieve
   * @return The request time range of transactions
   */
  public Flux<Transaction> findByIdBetweenAndFilterIn(
      String startingTransactionId, String endingTransactionId, List<TransactionFilter> filters) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    if (startingTransactionId != null) {
      params.add("from", startingTransactionId);
    }
    if (endingTransactionId != null) {
      params.add("to", endingTransactionId);
    }
    if (filters != null) {
      String typeList = filters.stream().map(Object::toString).collect(Collectors.joining(","));
      params.add("type", typeList);
    }

    String transactionsEndpoint =
        new MessageFormat("/v3/accounts/{0}/transactions/idrange").format(new Object[] {accountId});

    return webClient
        .get()
        .uri(builder -> builder.path(transactionsEndpoint).queryParams(params).build())
        .retrieve()
        .bodyToMono(GetTransactionsByIdsBetweenResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            })
        .flatMapIterable(GetTransactionsByIdsBetweenResponse::getTransactions);
  }

  private Flux<Transaction> findUsingEndpoint(String endpoint) {
    MultiValueMap<String, String> parameters =
        UriComponentsBuilder.fromUriString(endpoint).build().getQueryParams();
    String startingTransactionId = parameters.getFirst("from");
    String endingTransactionId = parameters.getFirst("to");
    String typeList = parameters.getFirst("type");
    List<TransactionFilter> filters = typeList == null ? null :
        Arrays.asList(typeList.split(",")).stream()
            .map(token -> Enum.valueOf(TransactionFilter.class, token))
            .collect(Collectors.toList());

    return findByIdBetweenAndFilterIn(startingTransactionId, endingTransactionId, filters);
  }
}
