/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.gateway.impl.configuration;

import io.zeebe.test.util.TestConfigurationFactory;
import io.zeebe.util.Environment;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class GatewayCfgTest {

  private static final String DEFAULT_CFG_FILENAME = "/configuration/gateway.default.yaml";
  private static final GatewayCfg DEFAULT_CFG = new GatewayCfg();
  private static final String EMPTY_CFG_FILENAME = "/configuration/gateway.empty.yaml";
  private static final String CUSTOM_CFG_FILENAME = "/configuration/gateway.custom.yaml";
  private static final GatewayCfg CUSTOM_CFG = new GatewayCfg();

  static {
    DEFAULT_CFG.init();
    CUSTOM_CFG.init();
    CUSTOM_CFG.getNetwork().setHost("192.168.0.1").setPort(123);
    CUSTOM_CFG
        .getCluster()
        .setContactPoint("foobar:1234")
        .setRequestTimeout(Duration.ofHours(123))
        .setClusterName("testCluster")
        .setMemberId("testMember")
        .setHost("1.2.3.4")
        .setPort(12321);
    CUSTOM_CFG
        .getSecurity()
        .setEnabled(true)
        .setCertificateChainPath("certificateChainPath")
        .setPrivateKeyPath("privateKeyPath");
    CUSTOM_CFG.getMonitoring().setEnabled(true).setHost("monitoringHost").setPort(1234);
    CUSTOM_CFG.getThreads().setManagementThreads(100);
  }

  private final Map<String, String> environment = new HashMap<>();

  @Test
  public void shouldHaveDefaultValues() {
    // when
    final GatewayCfg gatewayCfg = readDefaultConfig();

    // then
    Assertions.assertThat(gatewayCfg).isEqualTo(DEFAULT_CFG);
  }

  @Test
  public void shouldLoadEmptyConfig() {
    // when
    final GatewayCfg gatewayCfg = readEmptyConfig();

    // then
    Assertions.assertThat(gatewayCfg).isEqualTo(DEFAULT_CFG);
  }

  @Test
  public void shouldLoadCustomConfig() {
    // when
    final GatewayCfg gatewayCfg = readCustomConfig();

    // then
    Assertions.assertThat(gatewayCfg).isEqualTo(CUSTOM_CFG);
  }

  @Test
  public void shouldUseEnvironmentVariables() {
    // given
    setEnv("ZEEBE_GATEWAY_NETWORK_HOST", "zeebe");
    setEnv("ZEEBE_GATEWAY_NETWORK_PORT", "5432");
    setEnv("ZEEBE_GATEWAY_CLUSTER_CONTACT_POINT", "broker:432");
    setEnv("ZEEBE_GATEWAY_THREADS_MANAGEMENT_THREADS", "32");
    setEnv("ZEEBE_GATEWAY_CLUSTER_REQUEST_TIMEOUT", Duration.ofMinutes(43).toString());
    setEnv("ZEEBE_GATEWAY_CLUSTER_CLUSTER_NAME", "envCluster");
    setEnv("ZEEBE_GATEWAY_CLUSTER_MEMBER_ID", "envMember");
    setEnv("ZEEBE_GATEWAY_CLUSTER_HOST", "envHost");
    setEnv("ZEEBE_GATEWAY_CLUSTER_PORT", "12345");
    setEnv("ZEEBE_GATEWAY_MONITORING_ENABLED", "true");
    setEnv("ZEEBE_GATEWAY_MONITORING_HOST", "monitorHost");
    setEnv("ZEEBE_GATEWAY_MONITORING_PORT", "231");
    setEnv("ZEEBE_GATEWAY_SECURITY_ENABLED", String.valueOf(false));
    setEnv(
        "ZEEBE_GATEWAY_SECURITY_PRIVATE_KEY_PATH",
        GatewayCfgTest.class
            .getClassLoader()
            .getResource("security/test-server.key.pem")
            .getPath());
    setEnv(
        "ZEEBE_GATEWAY_SECURITY_CERTIFICATE_CHAIN_PATH",
        GatewayCfgTest.class
            .getClassLoader()
            .getResource("security/test-chain.cert.pem")
            .getPath());
    setEnv("ZEEBE_GATEWAY_NETWORK_MIN_KEEP_ALIVE_INTERVAL", Duration.ofSeconds(30).toString()); //

    final GatewayCfg expected = new GatewayCfg();
    expected
        .getNetwork()
        .setHost("zeebe")
        .setPort(5432)
        .setMinKeepAliveInterval(Duration.ofSeconds(30));
    expected
        .getCluster()
        .setContactPoint("broker:432")
        .setRequestTimeout(Duration.ofMinutes(43))
        .setClusterName("envCluster")
        .setMemberId("envMember")
        .setHost("envHost")
        .setPort(12345);
    expected.getThreads().setManagementThreads(32);
    expected.getMonitoring().setEnabled(true).setHost("monitorHost").setPort(231);
    expected
        .getSecurity()
        .setEnabled(false)
        .setPrivateKeyPath(
            getClass().getClassLoader().getResource("security/test-server.key.pem").getPath())
        .setCertificateChainPath(
            getClass().getClassLoader().getResource("security/test-chain.cert.pem").getPath());

    // when
    final GatewayCfg gatewayCfg = readCustomConfig();

    // then
    Assertions.assertThat(gatewayCfg).isEqualTo(expected);
  }

  private void setEnv(final String key, final String value) {
    environment.put(key, value);
  }

  private GatewayCfg readDefaultConfig() {
    return readConfig(DEFAULT_CFG_FILENAME);
  }

  private GatewayCfg readEmptyConfig() {
    return readConfig(EMPTY_CFG_FILENAME);
  }

  private GatewayCfg readCustomConfig() {
    return readConfig(CUSTOM_CFG_FILENAME);
  }

  private GatewayCfg readConfig(final String filename) {
    try (final InputStream inputStream = GatewayCfgTest.class.getResourceAsStream(filename)) {
      if (inputStream != null) {
        final GatewayCfg gatewayCfg =
            new TestConfigurationFactory()
                .create(new Environment(environment), "zeebe-gateway", filename, GatewayCfg.class);
        gatewayCfg.init();
        return gatewayCfg;
      } else {
        throw new AssertionError("Unable to find configuration file: " + filename);
      }
    } catch (final IOException e) {
      throw new AssertionError("Failed to read configuration from file: " + filename, e);
    }
  }
}
