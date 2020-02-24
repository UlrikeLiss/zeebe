/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.legacy.tomlconfig;

import io.zeebe.legacy.tomlconfig.util.Loggers;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public class LegacyConfigurationSupport {
  private static final Map<String, String> MAPPING_GATEWAY = new HashMap<>();
  private static final Map<String, Function<String, String>> VALUE_CONVERTERS_GATEWAY =
      new HashMap<>();
  private static final Map<String, String> MAPPING_BROKER = new HashMap<>();
  private static final int DEFAULT_CONTACT_POINT_PORT = 26502;

  { // static initialization for mapping tables

    // zeebe-gateway.network
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_HOST", "ZEEBE_GATEWAY_NETWORK_HOST");
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_PORT", "ZEEBE_GATEWAY_NETWORK_PORT");
    MAPPING_GATEWAY.put(
        "ZEEBE_GATEWAY_KEEP_ALIVE_INTERVAL", "ZEEBE_GATEWAY_NETWORK_MIN_KEEP_ALIVE_INTERVAL");

    // zeebe-gateway.cluster
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_REQUEST_TIMEOUT", "ZEEBE_GATEWAY_CLUSTER_REQUEST_TIMEOUT");
    MAPPING_GATEWAY.put(
        "ZEEBE_GATEWAY_MANAGEMENT_THREADS", "ZEEBE_GATEWAY_THREADS_MANAGEMENT_THREADS");
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_CONTACT_POINT", "ZEEBE_GATEWAY_CLUSTER_CONTACT_POINT");
    VALUE_CONVERTERS_GATEWAY.put(
        "ZEEBE_GATEWAY_CONTACT_POINT",
        value -> value.contains(":") ? value : value + ":" + DEFAULT_CONTACT_POINT_PORT);
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_CLUSTER_NAME", "ZEEBE_GATEWAY_CLUSTER_CLUSTER_NAME");
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_CLUSTER_MEMBER_ID", "ZEEBE_GATEWAY_CLUSTER_MEMBER_ID");
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_CLUSTER_HOST", "ZEEBE_GATEWAY_CLUSTER_HOST");
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_CLUSTER_PORT", "ZEEBE_GATEWAY_CLUSTER_PORT");

    // zeebe-gateway.monitoring
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_MONITORING_ENABLED", "ZEEBE_GATEWAY_MONITORING_ENABLED");
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_MONITORING_HOST", "ZEEBE_GATEWAY_MONITORING_HOST");
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_MONITORING_PORT", "ZEEBE_GATEWAY_MONITORING_PORT");

    // zeebe-gateway.security
    MAPPING_GATEWAY.put("ZEEBE_GATEWAY_SECURITY_ENABLED", "ZEEBE_GATEWAY_SECURITY_ENABLED");
    MAPPING_GATEWAY.put(
        "ZEEBE_GATEWAY_CERTIFICATE_PATH", "ZEEBE_GATEWAY_SECURITY_CERTIFICATE_CHAIN_PATH");
    MAPPING_GATEWAY.put(
        "ZEEBE_GATEWAY_PRIVATE_KEY_PATH", "ZEEBE_GATEWAY_SECURITY_PRIVATE_KEY_PATH");

    // TODO check docker files and shell scripts and change those as well
    // change environment variables in zeebe.cfg.yaml

    MAPPING_BROKER.putAll(
        MAPPING_GATEWAY); // TODO this is not correct: the new keys for the embedded gateway are
    // different from the keys in the standalone gateway
  }

  private final Scope scope;

  public LegacyConfigurationSupport(Scope scope) {
    this.scope = scope;
  }

  /**
   * This method checks whether the program was called with a toml configuration file. If so, it
   * prints out a warning.
   */
  public void checkForLegacyTomlConfigurationArgument(String[] args, String recommendedSetting) {
    if (args.length == 1 && args[0].endsWith(".toml")) {
      final String configFileArgument = args[0];
      Loggers.LEGACY_LOGGER.warn(
          "Found command line argument "
              + configFileArgument
              + " which might be a TOML configuration file.");
      Loggers.LEGACY_LOGGER.info(
          "TOML configuration files are no longer supported. Please specify a YAML configuration file"
              + "and set it via environment variable \"spring.config.additional-location\" (e.g. "
              + "\"export spring.config.additional-location='file:./config/"
              + recommendedSetting
              + "'\").");
      Loggers.LEGACY_LOGGER.info(
          "The ./config/ folder contains a configuration file template. Alternatively, you can also use environment variables.");
    }
  }

  /**
   * This method checks for legacy environment variables. If it finds an old environment variable
   * which is set, it looks whether the new counterpart is set. If the new counterpart is set, it
   * does nothing. If it is not set, it sets a system setting under the new key that has the same
   * value as is associated with the old environment variable. This effectively makes the old
   * environment variable value visible under the new environment variable. It also prints out a
   * warning to the log.
   */
  public void checkForLegacyEnvironmentVariables() {

    final Map<String, String> mappingTable;
    final Map<String, Function<String, String>> valueConverters;

    switch (scope) {
      case GATEWAY:
        mappingTable = MAPPING_GATEWAY;
        valueConverters = VALUE_CONVERTERS_GATEWAY;
        break;

      case BROKER:
        mappingTable = MAPPING_BROKER;
        valueConverters = Collections.emptyMap();
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + scope);
    }

    for (Entry<String, String> mapping : mappingTable.entrySet()) {
      final String oldEnvironmentVariable = mapping.getKey();
      final String newEnvironmentVariable = mapping.getValue();

      if (System.getenv(oldEnvironmentVariable) != null
          && System.getenv(newEnvironmentVariable) == null) {
        Loggers.LEGACY_LOGGER.warn(
            "Found use of legacy system environment variable '"
                + oldEnvironmentVariable
                + "'. Please use '"
                + newEnvironmentVariable
                + "' instead.");
        Loggers.LEGACY_LOGGER.info(
            "The old environment variable is currently supported as part of our backwards compatibility goals.");
        Loggers.LEGACY_LOGGER.info(
            "However, please note that the support for this environment variable is scheduled to be removed for release 0.25.0.");

        String value = System.getenv(oldEnvironmentVariable);

        final Function<String, String> valueConverter = valueConverters.get(oldEnvironmentVariable);

        if (valueConverter != null) {
          final String oldValue = value;
          value = valueConverter.apply(oldValue);

          Loggers.LEGACY_LOGGER.warn(
              "The old implementation performed an implicit value conversion from '"
                  + oldValue
                  + "' (as given in the environment variable) to '"
                  + value
                  + "' (as applied to the configuration). This automatic conversion will also be removed.");
        }

        System.setProperty(newEnvironmentVariable, value);
      }
    }
  }

  public enum Scope {
    GATEWAY,
    BROKER
  }
}
