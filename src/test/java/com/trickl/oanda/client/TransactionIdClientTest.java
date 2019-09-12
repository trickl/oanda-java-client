package com.trickl.oanda.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Subscription;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

@RunWith(MockitoJUnitRunner.class)
public class TransactionIdClientTest {

  @Mock
  private Scheduler scheduler;  

  @Mock
  private Disposable task;

  @Captor
  ArgumentCaptor<Runnable> taskCapture;

  private Subscription subscription;


  /**
   * Setup the tests.
   */
  @Before
  public void init() {
    subscription = null;
  }

  @Test
  public void testSubscribesCorrectly() {
    TransactionIdClient client =
        new TransactionIdClient();
    Flux<String> output = client.get();

    StepVerifier.create(output)
      .consumeSubscriptionWith(sub -> subscription = sub)
      .then(this::unsubscribe)
      .expectComplete()
      .verify();
  }


  @Test
  public void testGeneratesFirstCorrectly() {
    TransactionIdClient client =
        new TransactionIdClient();
    Flux<String> output = client.get();

    StepVerifier.create(output)
      .consumeSubscriptionWith(sub -> subscription = sub)
      .then(() -> client.publish("0"))
      .expectNext("0")
      .then(this::unsubscribe)
      .expectComplete()
      .verify();
  }

  @Test
  public void testGeneratesMultipleCorrectly() {
    TransactionIdClient client =
        new TransactionIdClient();
    Flux<String> output = client.get();

    StepVerifier.create(output)
      .consumeSubscriptionWith(sub -> subscription = sub)
      .then(() -> client.publish("0"))
      .expectNext("0")
      .then(() -> client.publish("1"))
      .expectNext("1")
      .then(() -> client.publish("2"))
      .expectNext("2")
      .then(() -> client.publish("3"))
      .expectNext("3")
      .then(this::unsubscribe)
      .expectComplete()
      .verify();
  }

  void unsubscribe() {
    if (subscription != null) {
      subscription.cancel();
    }
    subscription = null;
  }
}
