package org.exoplatform.microservices;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.picocontainer.Startable;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;

public class GRPCServer implements Startable {

  private static final Log    LOG = ExoLogger.getLogger(GRPCServer.class);

  private HealthStatusManager health;

  private Server              server;

  private int                 port;

  public GRPCServer(InitParams params) {
    if (params != null && params.containsKey("port")) {
      this.port = Integer.parseInt(params.getValueParam("port").getValue());
    }
  }

  @Override
  public void start() {
    try {
      LOG.info("Starting GRPC Server on port: {}", port);
      this.health = new HealthStatusManager();
      this.server = ServerBuilder.forPort(port)
                                 .addService(ProtoReflectionService.newInstance())
                                 .addService(health.getHealthService())
                                 .build()
                                 .start();
      this.health.setStatus("", ServingStatus.SERVING);
      LOG.info("GRPC Server started on port: {}", port);
    } catch (IOException e) {
      LOG.error("GRPC Server isn't started", e);
    }
  }

  @Override
  public void stop() {
    if (this.health != null) {
      server.shutdown();
      try {
        // Wait for RPCs to complete processing
        if (!server.awaitTermination(30, TimeUnit.SECONDS)) {
          // That was plenty of time. Let's cancel the remaining RPCs
          server.shutdownNow();
          // shutdownNow isn't instantaneous, so give a bit of time to clean
          // resources up
          // gracefully. Normally this will be well under a second.
          server.awaitTermination(5, TimeUnit.SECONDS);
        }
      } catch (InterruptedException ex) {
        server.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

}
