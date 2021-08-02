package com.bugreport.curator.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Wrapper around underlying {@link Member} instances that tracks their lifecycles etc.
 */
public class MemberDriver {

  public static final String DEFAULT_NAMESPACE = "LeaderElectionTest";
  public static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofSeconds(30);
  public static final String DEFAULT_MEMBER_PREFIX = "Member";
  public static final int DEFAULT_MEMBER_COUNT = 100;

  public static Builder builder(String nodeId, String servers) {
    return new Builder(nodeId, servers);
  }

  private final CuratorFramework framework;
  private final List<Member> members;

  private MemberDriver(Builder builder) {
    String nodeId = builder.nodeId;

    CuratorFrameworkFactory.Builder frameworkBuilder = CuratorFrameworkFactory.builder().connectString(builder.servers);
    String namespace = getOrDefault(builder.namespace, DEFAULT_NAMESPACE).trim();
    if (namespace.isEmpty()) {
      throw new IllegalArgumentException("The namespace cannot be empty");
    }
    frameworkBuilder.namespace(namespace);
    frameworkBuilder.retryPolicy(new RetryForever((int) getOrDefault(builder.retryInterval, DEFAULT_RETRY_INTERVAL).toMillis()));
    framework = frameworkBuilder.build();

    String memberPrefix = getOrDefault(builder.memberPrefix, DEFAULT_MEMBER_PREFIX).trim();
    if (memberPrefix.isEmpty()) {
      throw new IllegalArgumentException("The member prefix cannot be empty");
    }
    int memberCount = getOrDefault(builder.memberCount, DEFAULT_MEMBER_COUNT);
    if (memberCount < 1) {
      throw new IllegalArgumentException("The member count (" + memberCount + ") must be a positive number");
    }
    members = new ArrayList<>(memberCount);
    for (int i = 0; i < memberCount; i++) {
      members.add(new Member(framework, nodeId, memberPrefix + i));
    }
  }

  public void start() {
    framework.start();
    for (Member member : members) {
      member.start();
    }
  }

  public void shutdown() {
    for (Member member : members) {
      member.shutdown();
    }
    framework.close();
  }

  public List<Member> getMembers() {
    return Collections.unmodifiableList(members);
  }

  private static <T> T getOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  public static class Builder {

    private final String servers;
    private final String nodeId;

    private String memberPrefix;
    private Integer memberCount;
    private String namespace;
    private Duration retryInterval;

    private Builder(String nodeId, String servers) {
      this.nodeId = Objects.requireNonNull(nodeId, "The node id cannot be null").trim();
      this.servers = Objects.requireNonNull(servers, "The servers cannot be null").trim();
      if (nodeId.isEmpty()) {
        throw new IllegalArgumentException("The node id cannot be empty");
      }
      if (servers.isEmpty()) {
        throw new IllegalArgumentException("The servers cannot be empty");
      }
    }

    public Builder namespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder retryInterval(Duration retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    public Builder memberPrefix(String memberPrefix) {
      this.memberPrefix = memberPrefix;
      return this;
    }

    public Builder memberCount(int memberCount) {
      this.memberCount = memberCount;
      return this;
    }

    public MemberDriver build() {
      return new MemberDriver(this);
    }
  }
}
