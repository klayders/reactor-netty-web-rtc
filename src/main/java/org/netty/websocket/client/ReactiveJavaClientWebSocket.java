package org.netty.websocket.client;


import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;

public class ReactiveJavaClientWebSocket {
  public static void main(String[] args) {

    HttpClient headers = HttpClient.create()
      .headers(ss -> ss.add("userId", "123"));

    var client = new ReactorNettyWebSocketClient(headers);

    client.execute(
      URI.create("ws://localhost:8080/socket"),
      session -> {
        return session.send(
          Mono.just(
            session.textMessage("event-spring-reactive-client-websocket")
          )
        )
          .thenMany(
            session.receive()
              .map(webSocketMessage -> {
                String payloadAsText = webSocketMessage.getPayloadAsText();
                System.out.println("msg=" + payloadAsText);
                return payloadAsText;
              })
              .log()
          )
          .then();
      }
    )
      .block(Duration.ofSeconds(10L));
  }

}
