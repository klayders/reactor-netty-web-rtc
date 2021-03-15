package org.netty.websocket.config;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.netty.websocket.model.Event;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

@Slf4j
@Component
public class ReactiveWebSocketHandler implements WebSocketHandler {

  private static final ObjectMapper json = new ObjectMapper();

  private static final Map<String, WebSocketSession> authSession = new ConcurrentHashMap<>();
  private static final Map<String, WebSocketSession> notAuthSession = new ConcurrentHashMap<>();
//  private static final List<WebSocketSession> notAuthSession = new CopyOnWriteArrayList<>();

  private final Flux<String> eventFlux = Flux.generate(sink -> {
    Event event = new Event(randomUUID().toString(), now().toString());
    try {
      sink.next(json.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      sink.error(e);
    }
  });

  private final Flux<String> intervalFlux = Flux.interval(Duration.ofMillis(1000L))
    .zipWith(eventFlux, (time, event) -> event);

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    handleAuth(session);

//    return session
//      .send(
//        session.receive()
//        .map(WebSocketMessage::getPayloadAsText)
//        .map(session::textMessage)
//      );
    return session.receive()
//      .map(WebSocketMessage::getPayloadAsText)
      .flatMap(message -> handleMessage(message, session))
      .doOnError(e -> handleError(e, session))
      .doOnComplete(() -> handleComplete(session))
//      .zipWith(
//        session.send(intervalFlux.map(payload -> session.textMessage(payload)))
//      )
      .then();
  }

  public void sendMessage() {

  }

//    return webSocketSession.send(
//      intervalFlux
//        .map(webSocketSession::textMessage)
//    )
//      .and(
//        webSocketSession.receive()
//          .map(WebSocketMessage::getPayloadAsText)
//          .log()
//      );
//
//    return session.receive()
//      .map(WebSocketMessage::getPayloadAsText)
//      .map(this::toEvent)
//      .doOnNext(subscriber::onNext)
//      .doOnError(subscriber::onError)
//      .doOnComplete(subscriber::onComplete)
//      .zipWith(session.send(outputEvents.map(session::textMessage)))
//      .then();

  public void handleAuth(WebSocketSession session) {
    var headers = session.getHandshakeInfo().getHeaders();
    var userId = headers.getFirst("userId");
    if (StringUtils.hasLength(userId)) {
      authSession.put(userId, session);
    }

    notAuthSession.put(session.getId(), session);
    log.info("handleAuth: join user={}, sessionId={}, authMap={}, notAuthMap={}",
      userId, session.getId(), authSession.size(), notAuthSession.size()
    );
  }

  private Flux<Void> handleMessage(WebSocketMessage message, WebSocketSession session) {
    log.info("handleMessage: msg={}", message);

//    List<WebSocketSession> collect = notAuthSession.values()
//      .stream()
//      .filter(ws -> !ws.getId().equals(session.getId()))
//      .collect(Collectors.toList());

//    if (CollectionUtils.isEmpty(collect)) {
//      log.info("flux empty");
//      return Flux.empty();
//    }
//    log.info("flux has adress");

    return Flux.fromIterable(notAuthSession.values())
      .filter(webSocketSession -> !session.getId().equals(webSocketSession.getId()))
      .flatMap(webSocketSession -> {
        String payloadAsText = message.getPayloadAsText();
        log.info("message is={}", payloadAsText);
        return webSocketSession.send(Mono.just(webSocketSession.textMessage(payloadAsText)));
      });

//    return message;
  }

  private void handleError(Throwable e, WebSocketSession session) {
    log.error("handleError", e);
    removeSession(session);
  }

  private void handleComplete(WebSocketSession session) {
    removeSession(session);
  }

  private void removeSession(WebSocketSession session) {
    var userId = session.getHandshakeInfo().getHeaders().getFirst("userId");
    if (StringUtils.hasLength(userId)) {
      authSession.remove(userId);
    }

    notAuthSession.remove(session.getId());
    log.info("removeSession: userId={}, sessionId={}, authMap={}, notAuthMap={}", userId, session.getId(), authSession.size(), notAuthSession.size());
  }

}
