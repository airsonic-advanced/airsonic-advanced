<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<script type="text/javascript" src="<c:url value='/script/sockjs-client-1.4.0.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/stomp-4.0.8.min.js'/>"></script>
<script type="text/javascript">
    var StompClient = {
        stompClient: null,
        onConnect: null,
        onDisconnect: null,
        onError: null,
        connect: function(subscriptions) {
            if (this.stompClient != null) {
                this.disconnect();
            }
            var socket = new SockJS('/airsonic');
            this.stompClient = Stomp.over(socket);  
            this.stompClient.reconnect_delay = 30000;
            this.stompClient.connect({}, function(frame) {
                console.log('Connected', frame);
                if (StompClient.onConnect) {
                    StompClient.onConnect(frame);
                }
                if (subscriptions) {
                    for (var sub in subscriptions) {
                        StompClient.stompClient.subscribe(sub, subscriptions[sub]);
                    }
                }
            }, function(frame) {
                console.log('Error', frame);
                if (StompClient.onError) {
                    StompClient.onError(frame);
                }
            }, function(frame) {
                console.log('Disconnected', frame);
                if (StompClient.onDisconnect) {
                    StompClient.onDisconnect(frame);
                }
            });
        },
        disconnect: function() {
            if (this.stompClient != null) {
                this.stompClient.disconnect(function() {
                    console.log('Disconnected from Airsonic websocket');
                    if (StompClient.onDisconnect) {
                        StompClient.onDisconnect();
                    }
                });
            }
            this.stompClient = null;
        }
    }
</script>
