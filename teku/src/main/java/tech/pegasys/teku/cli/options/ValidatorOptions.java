/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.cli.options;

import java.nio.file.Path;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes32;
import picocli.CommandLine;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import tech.pegasys.teku.cli.converter.GraffitiConverter;
import tech.pegasys.teku.config.TekuConfiguration;
import tech.pegasys.teku.validator.api.FileBackedGraffitiProvider;
import tech.pegasys.teku.validator.api.ValidatorPerformanceTrackingMode;

public class ValidatorOptions {

  @CommandLine.Mixin(name = "Validator Keys")
  private ValidatorKeysOptions validatorKeysOptions;

  @Option(
      names = {"--validators-graffiti"},
      converter = GraffitiConverter.class,
      paramLabel = "<GRAFFITI STRING>",
      description =
          "Graffiti value to include during block creation. Value gets converted to bytes and padded to Bytes32.",
      arity = "1")
  private Bytes32 graffiti;

  @Option(
      names = {"--validators-graffiti-file"},
      paramLabel = "<GRAFFITI FILE>",
      description =
          "File to load graffiti value to include during block creation. Value gets converted to bytes and padded to Bytes32. "
              + "Takes precedence over --validators-graffiti. If the file can not be read, the --validators-graffiti value is used as a fallback.",
      arity = "1")
  private Path graffitiFile;

  @Option(
      names = {"--validators-performance-tracking-enabled"},
      paramLabel = "<BOOLEAN>",
      description = "Enable validator performance tracking",
      fallbackValue = "true",
      arity = "0..1",
      hidden = true)
  private Boolean validatorPerformanceTrackingEnabled = null;

  @Option(
      names = {"--validators-performance-tracking-mode"},
      paramLabel = "<TRACKING_MODE>",
      description =
          "Set strategy for handling performance tracking."
              + "Valid values: ${COMPLETION-CANDIDATES}",
      arity = "1")
  private ValidatorPerformanceTrackingMode validatorPerformanceTrackingMode =
      ValidatorPerformanceTrackingMode.ALL;

  @Option(
      names = {"--validators-keystore-locking-enabled"},
      paramLabel = "<BOOLEAN>",
      showDefaultValue = Visibility.ALWAYS,
      description = "Enable locking validator keystore files",
      arity = "1")
  private boolean validatorKeystoreLockingEnabled = true;

  @Option(
      names = {"--validators-external-signer-slashing-protection-enabled"},
      paramLabel = "<BOOLEAN>",
      showDefaultValue = Visibility.ALWAYS,
      description = "Enable internal slashing protection for external signers",
      fallbackValue = "true",
      arity = "0..1")
  private boolean validatorExternalSignerSlashingProtectionEnabled = true;

  @Option(
      names = {"--Xvalidators-dependent-root-enabled"},
      paramLabel = "<BOOLEAN>",
      description =
          "Invalidate validator duties based on the dependent root information instead of chain re-org events",
      hidden = true,
      fallbackValue = "true",
      arity = "0..1")
  private boolean useDependentRoots = true;

  @Option(
      names = {"--validators-early-attestations-enabled"},
      paramLabel = "<BOOLEAN>",
      showDefaultValue = Visibility.ALWAYS,
      description =
          "Generate attestations as soon as a block is known, rather than delaying until the attestation is due",
      fallbackValue = "true",
      arity = "0..1")
  private boolean generateEarlyAttestations = true;

  @Option(
      names = {"--Xvalidators-batch-attestations-enabled"},
      paramLabel = "<BOOLEAN>",
      description =
          "Send all attestations for a slot to the beacon node in a single batch. Default: false",
      fallbackValue = "true",
      hidden = true,
      arity = "0..1")
  private boolean sendAttestationsAsBatch = false;

  public void configure(TekuConfiguration.Builder builder) {
    if (validatorPerformanceTrackingEnabled != null) {
      if (validatorPerformanceTrackingEnabled) {
        this.validatorPerformanceTrackingMode = ValidatorPerformanceTrackingMode.ALL;
      } else {
        this.validatorPerformanceTrackingMode = ValidatorPerformanceTrackingMode.NONE;
      }
    }

    builder
        .validator(
            config ->
                config
                    .validatorKeystoreLockingEnabled(validatorKeystoreLockingEnabled)
                    .validatorPerformanceTrackingMode(validatorPerformanceTrackingMode)
                    .validatorExternalSignerSlashingProtectionEnabled(
                        validatorExternalSignerSlashingProtectionEnabled)
                    .graffitiProvider(
                        new FileBackedGraffitiProvider(
                            Optional.ofNullable(graffiti), Optional.ofNullable(graffitiFile)))
                    .useDependentRoots(useDependentRoots)
                    .generateEarlyAttestations(generateEarlyAttestations)
                    .sendAttestationsAsBatch(sendAttestationsAsBatch))
        // We don't need to update head for empty slots when using dependent roots
        .store(b -> b.updateHeadForEmptySlots(!useDependentRoots));
    validatorKeysOptions.configure(builder);
  }
}
