/*
 * Copyright 2021 ConsenSys AG.
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

package tech.pegasys.teku.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.teku.spec.SpecMilestone.ALTAIR;
import static tech.pegasys.teku.spec.SpecMilestone.PHASE0;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.config.TestConfigLoader;
import tech.pegasys.teku.spec.networks.Eth2Network;

public class SpecFactoryTest {

  private static final Set<String> ALTAIR_NETWORKS = Set.of("pyrmont", "prater");

  @Test
  public void defaultFactoryShouldOnlySupportPhase0_mainnet() {
    final Spec spec = SpecFactory.create("mainnet");
    assertThat(spec.getForkSchedule().getSupportedMilestones()).containsExactly(PHASE0);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("getKnownConfigNames")
  public void defaultFactoryShouldNotEnableAltairUnlessForkEpochIsSet(final String configName) {
    final Spec spec = SpecFactory.create(configName);
    if (ALTAIR_NETWORKS.contains(configName)) {
      assertThat(spec.getForkSchedule().getSupportedMilestones()).containsExactly(PHASE0, ALTAIR);
    } else {
      assertThat(spec.getForkSchedule().getSupportedMilestones()).containsExactly(PHASE0);
    }
  }

  @Test
  void shouldSupportAltairWhenForkEpochSetInConfig() {
    final SpecConfig config =
        TestConfigLoader.loadConfig(
            "mainnet",
            phase0Builder ->
                phase0Builder.altairBuilder(
                    altairBuilder -> altairBuilder.altairForkEpoch(UInt64.valueOf(10))));
    final Spec spec = SpecFactory.create(config);
    assertThat(spec.getForkSchedule().getSupportedMilestones()).containsExactly(PHASE0, ALTAIR);
    assertThat(spec.getForkSchedule().getSpecMilestoneAtEpoch(UInt64.valueOf(10)))
        .isEqualTo(ALTAIR);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> getKnownConfigNames() {
    return Arrays.stream(Eth2Network.values()).map(Eth2Network::configName).map(Arguments::of);
  }
}
