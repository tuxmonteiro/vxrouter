/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.platform.Verticle;

import static lbaas.Constants.CONF_PORT;
import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

public class RouterVerticle extends Verticle {

  @Override
  public void start() {

      final Logger log = container.logger();
      final JsonObject conf = container.config();
      final Long keepAliveTimeOut = conf.getLong("keepAliveTimeOut", 2000L);
      final Long keepAliveMaxRequest = conf.getLong("maxKeepAliveRequests", 100L);
      final Integer clientRequestTimeOut = conf.getInteger("clientRequestTimeOut", 60000);
      final Integer clientConnectionTimeOut = conf.getInteger("clientConnectionTimeOut", 60000);
      final Boolean clientForceKeepAlive = conf.getBoolean("clientForceKeepAlive", true);
      final Integer clientMaxPoolSize = conf.getInteger("clientMaxPoolSize",1);

      final Map<String, Set<Client>> vhosts = new HashMap<>();
      final Map<String, Set<Client>> badVhosts = new HashMap<>();
      final QueueMap queueMap = new QueueMap(this, vhosts, badVhosts);

      queueMap.registerQueueAdd();
      queueMap.registerQueueDel();

      final Handler<HttpServerRequest> handlerHttpServerRequest = new Handler<HttpServerRequest>() {
         @Override
         public void handle(final HttpServerRequest sRequest) {

             sRequest.response().setChunked(true);

             final Long requestTimeoutTimer = vertx.setTimer(clientRequestTimeOut, new Handler<Long>() {
                 @Override
                 public void handle(Long event) {
                     serverShowErrorAndClose(sRequest.response(), new java.util.concurrent.TimeoutException());
                 }
             });

             String headerHost;
             if (sRequest.headers().contains("Host")) {
                 headerHost = sRequest.headers().get("Host").split(":")[0];
                 if (!vhosts.containsKey(headerHost)) {
                     log.error(String.format("Host: %s UNDEF", headerHost));
                     serverShowErrorAndClose(sRequest.response(), new BadRequestException());
                     return;
                 }
             } else {
                 log.error("Host UNDEF");
                 serverShowErrorAndClose(sRequest.response(), new BadRequestException());
                 return;
             }

             final Set<Client> clients = vhosts.get(headerHost);
             if (clients==null ? true : clients.isEmpty()) {
                 log.error(String.format("Host %s without endpoints", headerHost));
                 serverShowErrorAndClose(sRequest.response(), new BadRequestException());
                 return;
             }

             final boolean connectionKeepalive = sRequest.headers().contains("Connection") ?
                     !"close".equalsIgnoreCase(sRequest.headers().get("Connection")) : 
                     sRequest.version().equals(HttpVersion.HTTP_1_1);

             final Client client = ((Client)clients.toArray()[getChoice(clients.size())])
                     .setKeepAlive(connectionKeepalive||clientForceKeepAlive)
                     .setKeepAliveTimeOut(keepAliveTimeOut)
                     .setKeepAliveMaxRequest(keepAliveMaxRequest)
                     .setConnectionTimeout(clientConnectionTimeOut)
                     .setMaxPoolSize(clientMaxPoolSize);

             final Handler<HttpClientResponse> handlerHttpClientResponse = new Handler<HttpClientResponse>() {

                     @Override
                     public void handle(HttpClientResponse cResponse) {

                         vertx.cancelTimer(requestTimeoutTimer);

                         // Pump cResponse => sResponse
                         sRequest.response().headers().set(cResponse.headers());
                         if (!connectionKeepalive) {
                             sRequest.response().headers().set("Connection", "close");
                         }

                         Pump.createPump(cResponse, sRequest.response()).start();

                         cResponse.endHandler(new VoidHandler() {
                             @Override
                             public void handle() {
                                 sRequest.response().end();
                                 if (connectionKeepalive) {
                                     if (client.isKeepAliveLimit()) {
                                         client.close();
                                         serverNormalClose(sRequest.response());
                                     }
                                 } else {
                                     if (!clientForceKeepAlive) {
                                         client.close();
                                     }
                                     serverNormalClose(sRequest.response());
                                 }
                             }
                         });

                         cResponse.exceptionHandler(new Handler<Throwable>() {
                             @Override
                             public void handle(Throwable event) {
                                 vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, client.toString() );
                                 serverShowErrorAndClose(sRequest.response(), event);
                                 client.close();
                             }
                         });
                 }
             };

             final HttpClient httpClient = client.connect();
             final HttpClientRequest cRequest = httpClient
                     .request(sRequest.method(), sRequest.uri(),handlerHttpClientResponse)
                     .setChunked(true);

             changeHeader(sRequest, headerHost);

             cRequest.headers().set(sRequest.headers());
             if (clientForceKeepAlive) {
                 cRequest.headers().set("Connection", "keep-alive");
             }

             // Pump sRequest => cRequest
             Pump.createPump(sRequest, cRequest).start();

             cRequest.exceptionHandler(new Handler<Throwable>() {
                 @Override
                 public void handle(Throwable event) {
                     vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, client.toString() );

                     serverShowErrorAndClose(sRequest.response(), event);
                     try {
                         client.close();
                     } catch (RuntimeException e) {
                         // Ignore double client close
                         return;
                     }
                 }
              });

             sRequest.endHandler(new VoidHandler() {
                 @Override
                 public void handle() {
                     cRequest.end();
                 }
              });
         }
     };

     vertx.createHttpServer().requestHandler(handlerHttpServerRequest)
         .setTCPKeepAlive(conf.getBoolean("serverTCPKeepAlive",true))
         .listen(conf.getInteger(CONF_PORT,9000));

     log.info(String.format("Instance %s started", this.toString()));

   }

   private void changeHeader(final HttpServerRequest sRequest, final String vhost) {
       String xff;
       String remote = sRequest.remoteAddress().getAddress().getHostAddress();
       sRequest.headers().set("X-Real-IP", remote);

       if (sRequest.headers().contains("X-Forwarded-For")) {
           xff = String.format("%s, %s", sRequest.headers().get("X-Forwarded-For"),remote);
           sRequest.headers().remove("X-Forwarded-For");
       } else {
           xff = remote;
       }
       sRequest.headers().set("X-Forwarded-For", xff);

       if (sRequest.headers().contains("Forwarded-For")) {
           xff = String.format("%s, %s" , sRequest.headers().get("Forwarded-For"), remote);
           sRequest.headers().remove("Forwarded-For");
       } else {
           xff = remote;
       }
       sRequest.headers().set("Forwarded-For", xff);

       if (!sRequest.headers().contains("X-Forwarded-Host")) {
           sRequest.headers().set("X-Forwarded-Host", vhost);
       }

       if (!sRequest.headers().contains("X-Forwarded-Proto")) {
           sRequest.headers().set("X-Forwarded-Proto", "http");
       }
   }

   private int getChoice(int size) {
       return (int) (Math.random() * (size - Float.MIN_VALUE));
   }

   private void serverShowErrorAndClose(final HttpServerResponse serverResponse, final Throwable event) {

       if (event instanceof java.util.concurrent.TimeoutException) {
           serverResponse.setStatusCode(504);
           serverResponse.setStatusMessage("Gateway Time-Out");
       } else if (event instanceof BadRequestException) {
           serverResponse.setStatusCode(400);
           serverResponse.setStatusMessage("Bad Request");
       } else {
           serverResponse.setStatusCode(502);
           serverResponse.setStatusMessage("Bad Gateway");
       }

       try {
           serverResponse.end();
       } catch (java.lang.IllegalStateException e) {
           // Response has already been written ?
           return;
       }

       try {
           serverResponse.close();
       } catch (RuntimeException e) {
           // Socket null or already closed
           return;
       }
   }

   private void serverNormalClose(final HttpServerResponse serverResponse) {
       try {
           serverResponse.close();
       } catch (RuntimeException e) {
           // Ignore already closed
           return;
       }
   }

}
