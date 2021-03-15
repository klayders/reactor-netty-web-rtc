package org.netty.websocket.client;


import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;

/**
 * emulate ws connection with ReactorNettyWebSocketClient
 */

public class ReactiveJavaClientWebSocket {
  public static void main(String[] args) {

    var headers = HttpClient.create()
      // set authorization header;
      // this maybe jwt or user identification or else
      .headers(ss -> ss.add("userId", "123"));

    var client = new ReactorNettyWebSocketClient(headers);

    client.execute(
      URI.create("ws://localhost:8080/socket"),
      session -> session.send(createMessage(session))
        .thenMany(receiveMessagesFromServer(session))
        .then()
    )
      .block(Duration.ofSeconds(10L));
  }

  private static Mono<WebSocketMessage> createMessage(WebSocketSession session) {
    return Mono.just(session.textMessage("event-spring-reactive-client-websocket"));
  }

  private static Flux<String> receiveMessagesFromServer(WebSocketSession session) {
    return session.receive()
      .map(webSocketMessage -> {
        String payloadAsText = webSocketMessage.getPayloadAsText();
        System.out.println("msg=" + payloadAsText);
        return payloadAsText;
      });
  }

}
