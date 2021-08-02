package com.bugreport.curator.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents a member in a cluster
 */
public class Member {

  private static final Logger LOGGER = LoggerFactory.getLogger(Member.class);

  private static final Duration ALIVE_HEARTBEAT_INTERVAL = Duration.ofSeconds(30);

  private final String id;
  private final LeaderSelector selector;
  private volatile boolean started;
  private volatile boolean shutdown;

  Member(CuratorFramework client, String nodeId, String clusterId) {
    Objects.requireNonNull(nodeId, "The node id cannot be null");
    Objects.requireNonNull(clusterId, "The cluster id cannot be null");
    clusterId = clusterId.startsWith("/") ? clusterId : "/" + clusterId;
    id = nodeId + clusterId;
    selector = new LeaderSelector(client, clusterId, new LeaderTask());
    selector.autoRequeue();
  }

  void start() {
    if (shutdown) {
      throw new IllegalStateException("Cluster member cannot be started after shutdown");
    }
    if (!started) {
      started = true;
      selector.start();
    }
  }

  void shutdown() {
    if (!shutdown) {
      shutdown = true;
      if (started) {
        selector.close();
      }
    }
  }

  public String getId() {
    return id;
  }

  public int getClusterMemberCount() {
    try {
      return selector.getParticipants().size();
    } catch (Exception e) {
      LOGGER.error("Unable to determine cluster member count for member '{}'", id, e);
      return -1;
    }
  }

  private class LeaderTask extends LeaderSelectorListenerAdapter {

    @Override
    public void takeLeadership(CuratorFramework curatorFramework) throws Exception {
      LOGGER.info("Taking leadership -- '{}'", id);
      while (started && !shutdown) {
        try {
          Thread.sleep(ALIVE_HEARTBEAT_INTERVAL.toMillis());
          LOGGER.debug("Leadership heartbeat -- '{}'", id);
        } catch (InterruptedException e) {
          LOGGER.debug("Interrupted, will relinquish leadership -- '{}'", id);
          Thread.currentThread().interrupt();
          break;
        }
      }
      LOGGER.info("Relinquishing leadership -- '{}'", id);
    }
  }
}
