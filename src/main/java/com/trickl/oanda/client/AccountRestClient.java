package com.trickl.oanda.client;

import com.trickl.model.oanda.account.Account;
import com.trickl.model.oanda.account.AccountChanges;
import com.trickl.model.oanda.account.AccountProperties;
import com.trickl.model.oanda.account.GetAccountChangesResponse;
import com.trickl.model.oanda.account.GetAccountResponse;
import com.trickl.model.oanda.account.GetAccountsResponse;
import com.trickl.oanda.validation.ServerResponseValidator;
import java.text.MessageFormat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AccountRestClient {

  private final WebClient webClient;

  private final String accountId;

  private final TransactionIdClient transactionIdClient;

  private final ServerResponseValidator validator = new ServerResponseValidator();

  /**
   * Get the account data.
   *
   * @return A single account
   */
  public Mono<Account> get() {
    return findById(accountId);
  }

  /**
   * Get the account data for any account.
   *
   * @param accountId The account identifier
   * @return The requested list of accounts
   */
  public Mono<Account> findById(@NonNull String accountId) {

    String accountsEndpoint =
        new MessageFormat("/v3/accounts/{0}").format(new Object[] {accountId});

    return webClient
        .get()
        .uri(accountsEndpoint)
        .retrieve()
        .bodyToMono(GetAccountResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            })
        .map(GetAccountResponse::getAccount);
  }

  /**
   * Endpoint used to poll an Account for its current state and changes since a specified
   * TransactionID.
   *
   * @param sinceTransactionId ID of the Transaction to get Account changes since
   * @return The account changes
   */
  public Mono<AccountChanges> findChangesSince(String sinceTransactionId) {

    String accountsEndpoint =
        new MessageFormat("/v3/accounts/{0}/changes").format(new Object[] {accountId});

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("sinceTransactionID", sinceTransactionId);

    return webClient
        .get()
        .uri(builder -> builder.path(accountsEndpoint).queryParams(params).build())
        .retrieve()
        .bodyToMono(GetAccountChangesResponse.class)
        .doOnNext(
            response -> {
              validator.validate(response);
              transactionIdClient.publish(response.getLastTransactionId());
            })
        .map(GetAccountChangesResponse::getChanges);
  }

  /**
   * Get a list of Accounts.
   *
   * @return The requested list of accounts
   */
  public Flux<AccountProperties> findAll() {

    return webClient
        .get()
        .uri("/v3/accounts")
        .retrieve()
        .bodyToMono(GetAccountsResponse.class)
        .doOnNext(validator::validate)
        .flatMapIterable(GetAccountsResponse::getAccounts);
  }
}
