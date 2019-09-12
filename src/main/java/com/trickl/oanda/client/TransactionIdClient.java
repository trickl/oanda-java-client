package com.trickl.oanda.client;

import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

@Getter
public class TransactionIdClient implements Supplier<Flux<String>> {

  private final DirectProcessor<String> processor;

  private final ConnectableFlux<String> connector;

  public TransactionIdClient() {
    processor = DirectProcessor.create();
    connector = processor.publish();
  }

  @Override
  public Flux<String> get() {
    return connector;
  }

  void publish(@NonNull String transactionId) {
    processor.onNext(transactionId);
  }
}
