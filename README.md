# reactor-netty-web-rtc

In this case -> server need for set connection by websocket for users. When user start initialize **RTCPeerConnection**,
from client1 send messages from RTC to server. Then server send messages to client2, client3... When client2 receive
message from client1, than open channels between two clients;

## Server

0) main start
   class [ReactiveWebSocketApplication.java](/src/main/java/org/netty/websocket/ReactiveWebSocketApplication.java)

## Client

### Browser

0) start server
1) open in two windows of browser [index.html](/src/main/resources/static/index.html) as file
2) when you open page of [index.html](/src/main/resources/static/index.html) click "Create offer"
3) send message
4) open browser console and check message from another window of browser

### Java

0) start server
1) start [ReactiveJavaClientWebSocket.java](/src/main/java/org/netty/websocket/client/ReactiveJavaClientWebSocket.java)

