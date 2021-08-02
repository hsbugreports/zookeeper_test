package com.bugreport.curator;

import com.bugreport.curator.cluster.Member;
import com.bugreport.curator.cluster.MemberDriver;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private static final Duration DEFAULT_TEST_INTERVAL = Duration.ofSeconds(30);

  private final String nodeId;
  private final Duration testInterval;
  private final MemberDriver driver;
  private volatile boolean shutdown;

  private Main(Builder builder) {
    nodeId = Objects.requireNonNull(builder.nodeId, "The node id cannot be null");
    LOGGER.info("servers {}", builder.servers);
    MemberDriver.Builder memberBuilder = MemberDriver.builder(nodeId, builder.servers);
    setIfPresent(memberBuilder::namespace, builder.namespace);
    setIfPresent(memberBuilder::memberPrefix, builder.memberPrefix);
    setIfPresent(memberBuilder::memberCount, builder.memberCount);
    setIfPresent(memberBuilder::retryInterval, builder.retryInterval);
    testInterval = builder.testInterval == null ? DEFAULT_TEST_INTERVAL : builder.testInterval;
    driver = memberBuilder.build();
  }

  public static void main(String[] args) {
    CommandLineParser parser = new DefaultParser();
    Options supportedOptions = getSupportedOptions();
    CommandLine line;
    try {
      line = parser.parse(supportedOptions, args);
    } catch (ParseException exp) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("curator-test", supportedOptions);
      return;
    }

    Builder builder = new Builder();
    builder.nodeId(line.getOptionValue(getNodeIdOption().getOpt()));
    builder.servers(line.getOptionValue(getServerOption().getOpt()));
    if (line.hasOption(getNamespaceOption().getOpt())) {
      builder.namespace(line.getOptionValue(getNamespaceOption().getOpt()));
    }
    if (line.hasOption(getRetryOption().getOpt())) {
      builder.retryInterval(Duration.ofMillis(Long.parseLong(line.getOptionValue(getRetryOption().getOpt()))));
    }
    if (line.hasOption(getTestOption().getOpt())) {
      builder.testInterval(Duration.ofMillis(Long.parseLong(line.getOptionValue(getTestOption().getOpt()))));
    }
    if (line.hasOption(getPrefixOption().getOpt())) {
      builder.memberPrefix(line.getOptionValue(getPrefixOption().getOpt()));
    }
    if (line.hasOption(getCountOption().getOpt())) {
      builder.memberCount(Integer.parseInt(line.getOptionValue(getCountOption().getOpt())));
    }
    Main main = builder.build();

    Runtime.getRuntime().addShutdownHook(new Thread(main::requestShutdown));

    Thread runner = new Thread(main::run);
    runner.start();
    try {
      runner.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void run() {
    driver.start();
    LOGGER.info("Beginning test for node {}..", nodeId);
    while (!shutdown) {
      try {
        Thread.sleep(testInterval.toMillis());
        for (Member member : driver.getMembers()) {
          int participants = member.getClusterMemberCount();
          if (participants > 2) {
            LOGGER.info("Possible orphaned ephemeral node, member '{}' is reporting '{}' participants", member.getId(), participants);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    LOGGER.info("Ending test for node {}...", nodeId);
  }

  private void requestShutdown() {
    if (!shutdown) {
      shutdown = true;
      driver.shutdown();
    }
  }

  private static <T> void setIfPresent(Consumer<T> sink, T value) {
    if (value != null) {
      sink.accept(value);
    }
  }

  private static Options getSupportedOptions() {
    Options options = new Options();
    options.addOption(getNodeIdOption());
    options.addOption(getServerOption());
    options.addOption(getNamespaceOption());
    options.addOption(getRetryOption());
    options.addOption(getTestOption());
    options.addOption(getPrefixOption());
    options.addOption(getCountOption());
    return options;
  }

  private static Option getNodeIdOption() {
    Option rtValue = new Option("i", "id", true, "The unique node identifier");
    rtValue.setRequired(true);
    return rtValue;
  }

  private static Option getServerOption() {
    Option rtValue = new Option("s", "servers", true, "The Zookeeper server connection string");
    rtValue.setRequired(true);
    return rtValue;
  }

  private static Option getNamespaceOption() {
    return new Option("n", "namespace", true, formatWithDefault("The Curator framework namespace to be used", MemberDriver.DEFAULT_NAMESPACE));
  }

  private static Option getRetryOption() {
    return new Option("r", "retry", true, formatWithDefault("The Curator retry interval millis.", MemberDriver.DEFAULT_RETRY_INTERVAL.toMillis()));
  }

  private static Option getTestOption() {
    return new Option("t", "test", true, formatWithDefault("The test interval millis.", DEFAULT_TEST_INTERVAL.toMillis()));
  }

  private static Option getPrefixOption() {
    return new Option("p", "prefix", true, formatWithDefault("The member prefix.", MemberDriver.DEFAULT_MEMBER_PREFIX));
  }

  private static Option getCountOption() {
    return new Option("c", "count", true, formatWithDefault("The member count", MemberDriver.DEFAULT_MEMBER_COUNT));
  }

  private static String formatWithDefault(String message, Object defaultValue) {
    return String.format("%s Default: '%s'", message, defaultValue);
  }

  private static class Builder {
    private String nodeId;
    private String servers;
    private String namespace;
    private String memberPrefix;
    private Integer memberCount;
    private Duration retryInterval;
    private Duration testInterval;

    private Builder() {
    }

    private Builder nodeId(String nodeId) {
      this.nodeId = nodeId;
      return this;
    }

    private Builder servers(String servers) {
      this.servers = servers;
      return this;
    }

    private Builder namespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    private Builder memberPrefix(String memberPrefix) {
      this.memberPrefix = memberPrefix;
      return this;
    }

    private Builder memberCount(Integer memberCount) {
      this.memberCount = memberCount;
      return this;
    }

    private Builder retryInterval(Duration retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    private Builder testInterval(Duration testInterval) {
      this.testInterval = testInterval;
      return this;
    }

    private Main build() {
      return new Main(this);
    }
  }
}
