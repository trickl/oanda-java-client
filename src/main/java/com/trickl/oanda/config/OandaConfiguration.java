package com.trickl.oanda.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trickl.oanda.client.AccountRestClient;
import com.trickl.oanda.client.CandleRestClient;
import com.trickl.oanda.client.OrderBookRestClient;
import com.trickl.oanda.client.OrderRestClient;
import com.trickl.oanda.client.PositionRestClient;
import com.trickl.oanda.client.PriceStreamClient;
import com.trickl.oanda.client.TradeRestClient;
import com.trickl.oanda.client.TransactionIdClient;
import com.trickl.oanda.client.TransactionRestClient;
import com.trickl.oanda.client.TransactionStreamClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OandaConfiguration {

  @Value("${oanda.exchangeId:OANDA}")
  @Getter
  private String exchangeId;

  @Value("${oanda.apiToken:}")
  @Getter
  private String apiToken;

  @Value("${oanda.accountId:}")
  @Getter
  private String accountId;

  @Value("${oanda.isProduction:false}")
  @Getter
  private boolean isProduction;

  @Value("${oanda.orderBookRecorderBean:influxDbOrderBookRepository}")
  @Getter
  private String orderBookRecorderBean;

  @Value("${oanda.candleRecorderBean:influxDbCandleRepository}")
  @Getter
  private String candleRecorderBean;

  private static final String DEV_STREAM_URL = "https://stream-fxpractice.oanda.com";

  private static final String PROD_STREAM_URL = "https://stream-fxtrade.oanda.com";

  private static final String DEV_REST_URL = "https://api-fxpractice.oanda.com";

  private static final String PROD_REST_URL = "https://api-fxtrade.oanda.com";

  /**
   * Get the Oanda stream url.
   *
   * @return Oanda stream url
   */
  public String getStreamUrl() {
    return isProduction ? PROD_STREAM_URL : DEV_STREAM_URL;
  }

  /**
   * Get the Oanda rest url.
   *
   * @return Oanda rest url
   */
  public String getRestUrl() {
    return isProduction ? PROD_REST_URL : DEV_REST_URL;
  }

  @Bean
  ObjectMapper oandaObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  @Bean
  WebClient oandaRestClient() {
    return WebClient.builder()
        .baseUrl(getRestUrl())
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
        .defaultHeader("Accept-Datetime-Format", "RFC3339")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean
  WebClient oandaStreamClient() {
    return WebClient.builder()
        .baseUrl(getStreamUrl())
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
        .defaultHeader("Accept-Datetime-Format", "RFC3339")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs(
                    configurer ->
                        configurer
                            .defaultCodecs()
                            .jackson2JsonDecoder(
                                new Jackson2JsonDecoder(
                                    oandaObjectMapper(), MediaType.APPLICATION_OCTET_STREAM)))
                .build())
        .build();
  }

  @Bean
  AccountRestClient oandaAccountRestClient() {
    return new AccountRestClient(oandaRestClient(), accountId, oandaTransactionIdClient());
  }

  @Bean
  CandleRestClient oandaCandleRestClient() {
    return new CandleRestClient(oandaRestClient());
  }

  @Bean
  OrderBookRestClient oandaOrderBookRestClient() {
    return new OrderBookRestClient(oandaRestClient());
  }

  @Bean
  OrderRestClient oandaOrderRestClient() {
    return new OrderRestClient(oandaRestClient(), accountId, oandaTransactionIdClient());
  }

  @Bean
  PositionRestClient oandaPositionRestClient() {
    return new PositionRestClient(oandaRestClient(), accountId, oandaTransactionIdClient());
  }

  @Bean
  TradeRestClient oandaTradeRestClient() {
    return new TradeRestClient(oandaRestClient(), accountId, oandaTransactionIdClient());
  }

  @Bean
  TransactionRestClient oandaTransactionRestClient() {
    return new TransactionRestClient(oandaRestClient(), accountId, oandaTransactionIdClient());
  }

  @Bean
  TransactionIdClient oandaTransactionIdClient() {
    return new TransactionIdClient();
  }

  @Bean
  PriceStreamClient oandaPriceStreamClient() {
    return new PriceStreamClient(oandaStreamClient(), accountId, true);
  }

  @Bean
  TransactionStreamClient oandaTransactionStreamClient() {
    return new TransactionStreamClient(oandaStreamClient(), accountId, true);
  }
}
