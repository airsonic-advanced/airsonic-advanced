<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<script type="text/javascript" src="<c:url value='/script/sockjs-client-1.4.0.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/stomp-4.0.8.min.js'/>"></script>
<script type="text/javascript">
    var csrfheaderName = "${_csrf.headerName}";
    var csrftoken = "${_csrf.token}";
    var StompClient = {
        stompClient: null,
        onConnect: null,
        onDisconnect: null,
        onError: null,
        reconnectionRequired: function() {
            return this.stompClient == null || !this.stompClient.connected;
        },
        send: function(dest, msg) {
            this.connect(function(stompclient) {
                stompclient.stompClient.send(dest, {}, msg);
            });
        },
        state: 'dc',
        outstandingConnectionCallbacks: [],
        __doConnectionCallbacks: function(stompclient) {
            while(stompclient.outstandingConnectionCallbacks.length > 0) {
                var callback = stompclient.outstandingConnectionCallbacks.pop();
                try {
                    callback(stompclient);
                } catch(e) {
                    console.log("Could not do postConnectCallback", e);
                }
            }
        },
        connect: function(postConnectCallback) {
            if (postConnectCallback) {
                this.outstandingConnectionCallbacks.push(postConnectCallback);
            }
            if (this.stompClient == null) {
                var socket = new SockJS("<c:url value='/websocket'/>");
                this.stompClient = Stomp.over(socket);
                this.stompClient.reconnect_delay = 30000;
                this.stompClient.heartbeat.incoming = 25000;
                this.stompClient.heartbeat.outgoing = 25000;
            }

            if (this.state == 'connecting') {
                // whenever the connections is made, the callbacks will happen
                return;
            } else if (this.state == 'connected') {
                this.__doConnectionCallbacks(this);
            } else {
                this.state = 'connecting';
                var stompclient = this;
                var headers = {};
                headers[csrfheaderName] = csrftoken;
                this.stompClient.connect(headers, function(frame) {
                    console.log('Connected', frame);
                    stompclient.state='connected';
                    if (stompclient.onConnect) {
                        stompclient.onConnect(frame);
                    }
                    stompclient.__doConnectionCallbacks(stompclient);
                }, function(frame) {
                    console.log('Error', frame);
                    if (stompclient.onError) {
                        stompclient.onError(frame);
                    }
                }, function(frame) {
                    console.log('Disconnected', frame);
                    stompclient.state='dc';
                    if (stompclient.onDisconnect) {
                        stompclient.onDisconnect(frame);
                    }
                });
            }
        },
        // object containing:
        // - 'subscription-topic': {'owner1': callback1(msg), 'owner2': callback2(msg)}
        subscriptions: {},
        unsubscriptions: {},
        subscribe: function(owner, subscriptions, postSubCallback) {
            this.connect(function(stompclient) {
                //add subscriptions to the callback obj
                for (var topic in subscriptions) {
                    var subCallbacks = stompclient.subscriptions[topic];
                    if (!subCallbacks || stompclient.resubscribeEveryTime(topic)) {
                        // subscribe to topic on server
                        var subCallbacks = {};
                        stompclient.__doSubscription(stompclient, topic, subCallbacks);
                    }

                    // add the actual callback
                    subCallbacks[owner] = subscriptions[topic];
                }

                if (postSubCallback) {
                    postSubCallback();
                }
            });
        },
        __doSubscription: function(stompclient, topic, subCallbacks) {
            stompclient.subscriptions[topic] = subCallbacks;
            stompclient.unsubscriptions[topic] = stompclient.stompClient.subscribe(topic, function(msg) {
                for (var owner in subCallbacks) {
                    try {
                        subCallbacks[owner](msg);
                    } catch(e) {
                        console.log("Couldn't execute callback", owner, e);
                    }
                }
            });
        },
        resubscribeEveryTime: function(topic) {
            return topic.startsWith("/app/");
        },
        unsubscribe: function(topic, owner) {
            if (topic && owner && this.subscriptions[topic][owner]) {
                delete this.subscriptions[topic][owner];
            }
            // can also delete the subscription to the topic itself (if that is the last owner), but not necessary
        },
        disconnect: function() {
            if (this.stompClient != null) {
                var stompclient = this;
                this.stompClient.disconnect(function() {
                    console.log('Disconnected from Airsonic websocket');
                    stompclient.state='dc';
                    if (stompclient.onDisconnect) {
                        stompclient.onDisconnect();
                    }
                });
            }
        }
    }
</script>
