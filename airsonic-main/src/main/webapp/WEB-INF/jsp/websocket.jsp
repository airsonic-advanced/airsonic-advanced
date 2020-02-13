<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<script type="text/javascript" src="<c:url value='/script/sockjs-client-1.4.0.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/stomp-4.0.8.min.js'/>"></script>
<script type="text/javascript">
    var StompClient = {
        stompClient: null,
        onConnect: null,
        onDisconnect: null,
        onError: null,
        reconnectionRequired: function() {
            return this.stompClient == null || !this.stompClient.connected;
        },
        send: function(dest, msg) {
            if (this.reconnectionRequired()) {
                this.connect(function(stompclient) {
                    stompclient.send(location, msg);
                });
            } else {
                this.stompClient.send(dest, {}, msg);
            }
        }, connect: function(runAfterConnect) {
            if (this.stompClient != null) {
                this.disconnect();
            }
            var socket = new SockJS('/airsonic');
            this.stompClient = Stomp.over(socket);  
            this.stompClient.reconnect_delay = 30000;
            var stompclient = this;
            this.stompClient.connect({}, function(frame) {
                console.log('Connected', frame);
                if (stompclient.onConnect) {
                    stompclient.onConnect(frame);
                }
                if (runAfterConnect) {
                    runAfterConnect(stompclient);
                }
            }, function(frame) {
                console.log('Error', frame);
                if (stompclient.onError) {
                    stompclient.onError(frame);
                }
            }, function(frame) {
                console.log('Disconnected', frame);
                if (stompclient.onDisconnect) {
                    stompclient.onDisconnect(frame);
                }
            });
        },
        // object containing:
        // - 'subscription-location': callback(msg) (transformed to form below), or
        // - 'subscription-location': {callback: fn(msg), subscriptionArgs: {}, subscription: obj (generated)}
        subscriptions: {},
        subscribe: function(subscriptions, resubscribeToEverything, afterSubscription) {
            resubscribeToEverything = (typeof resubscribeToEverything !== 'boolean') ? false : resubscribeToEverything;
            var reconnect = this.reconnectionRequired();
            for (var sub in subscriptions) {
                // unsubscribe if already subscribed and connected
                if (this.subscriptions[sub] && !reconnect && this.subscriptions[sub].subscription) {
                    this.subscriptions[sub].subscription.unsubscribe();
                }
                var subArg = (typeof subscriptions[sub] == 'function') ? {callback: subscriptions[sub], subArgs: {}} : subscriptions[sub];
                this.subscriptions[sub] = subArg;
            }
            if (reconnect) {
                this.connect(function(stompclient) {
                    stompclient.subscribe({}, true);
                    if (afterSubscription) {
                        afterSubscription();
                    }
                });
            } else {
                subscriptions = resubscribeToEverything ? this.subscriptions : subscriptions;
                for (var sub in subscriptions) {
                    var subscription = this.stompClient.subscribe(sub, this.subscriptions[sub].callback);
                    this.subscriptions[sub].subscription = subscription;
                }
                if (afterSubscription) {
                    afterSubscription();
                }
            }
        }, disconnect: function() {
            if (this.stompClient != null) {
                var stompclient = this;
                this.stompClient.disconnect(function() {
                    console.log('Disconnected from Airsonic websocket');
                    if (stompclient.onDisconnect) {
                        stompclient.onDisconnect();
                    }
                });
            }
            this.stompClient = null;
        }
    }
</script>
