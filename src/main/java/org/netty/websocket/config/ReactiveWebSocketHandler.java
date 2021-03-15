package org.netty.websocket.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ReactiveWebSocketHandler implements WebSocketHandler {

  private static final Map<String, WebSocketSession> authSession = new ConcurrentHashMap<>();
  private static final Map<String, WebSocketSession> notAuthSession = new ConcurrentHashMap<>();


  @Override
  public Mono<Void> handle(WebSocketSession session) {
    handleAuth(session);

    return session.receive()
      .flatMap(message -> handleMessage(message, session))
      .doOnError(e -> handleError(e, session))
      .doOnComplete(() -> handleComplete(session))
      .then();
  }


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

    return Flux.fromIterable(notAuthSession.values())
      // filter current user
      .filter(webSocketSession -> !session.getId().equals(webSocketSession.getId()))
      // send messages to another users
      .flatMap(webSocketSession -> {
        String payloadAsText = message.getPayloadAsText();
        log.info("message is={}", payloadAsText);
        return webSocketSession.send(Mono.just(webSocketSession.textMessage(payloadAsText)));
      });
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
    log.info("removeSession: userId={}, sessionId={}, authMap={}, notAuthMap={}",
      userId, session.getId(), authSession.size(), notAuthSession.size()
    );
  }

}
