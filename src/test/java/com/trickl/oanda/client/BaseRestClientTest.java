package com.trickl.oanda.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class BaseRestClientTest {

  protected MockWebServer server;

  protected WebClient webClient;

  protected Validator validator;

  protected void startServer() {
    server = new MockWebServer();
    webClient =
        WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector())
            .baseUrl(server.url("/").toString())
            .build();
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  protected void prepareResponse(String fileName) throws IOException {
    Path responsePath = classAsResourcePathConvention(this.getClass(), fileName);
    String responseContent = new String(Files.readAllBytes(responsePath));
    prepareResponse(
        response ->
            response.setHeader("Content-Type", "application/json").setBody(responseContent));
  }

  protected void prepareResponse(Consumer<MockResponse> consumer) {
    MockResponse response = new MockResponse();
    consumer.accept(response);
    this.server.enqueue(response);
  }

  protected void expectPath(String path) {
    expectRequest(path, HttpMethod.GET);
  }

  protected void expectRequest(String path, HttpMethod method) {
    expectRequest(
        request -> {
          assertThat(request.getPath()).isEqualTo(path);
          assertThat(request.getMethod()).isEqualTo(method.toString());
        });
  }

  protected void expectRequest(Consumer<RecordedRequest> consumer) {
    try {
      consumer.accept(this.server.takeRequest());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    }
  }

  protected void expectRequestCount(int count) {
    assertThat(this.server.getRequestCount()).isEqualTo(count);
  }

  private <T> Path classAsResourcePathConvention(Class<T> clazz, String filename) {
    String resourcePath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
    String projectDir = resourcePath.substring(0, resourcePath.indexOf("target"));
    return Paths.get(
        projectDir,
        "src/test/resources/",
        clazz.getPackage().getName().replaceAll("\\.", "/"),
        filename);
  }
}
