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

package tech.pegasys.teku.statetransition.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.pegasys.teku.infrastructure.unsigned.UInt64.ONE;

import java.util.List;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.bls.BLSKeyGenerator;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.bls.BLSTestUtil;
import tech.pegasys.teku.core.ChainBuilder;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.SignedBlockAndState;
import tech.pegasys.teku.statetransition.BeaconChainUtil;
import tech.pegasys.teku.storage.client.ChainUpdater;
import tech.pegasys.teku.storage.client.MemoryOnlyRecentChainData;
import tech.pegasys.teku.storage.client.RecentChainData;
import tech.pegasys.teku.storage.storageSystem.InMemoryStorageSystemBuilder;
import tech.pegasys.teku.storage.storageSystem.StorageSystem;

public class BlockValidatorTest {
  private final Spec spec = TestSpecFactory.createMinimalPhase0();
  private final RecentChainData recentChainData =
      MemoryOnlyRecentChainData.builder().specProvider(spec).build();
  private final BeaconChainUtil beaconChainUtil = BeaconChainUtil.create(spec, 10, recentChainData);

  private BlockValidator blockValidator;

  @BeforeEach
  void setUp() {
    beaconChainUtil.initializeStorage();
    blockValidator = new BlockValidator(spec, recentChainData);
  }

  @Test
  void shouldReturnValidForValidBlock() throws Exception {
    final UInt64 nextSlot = recentChainData.getHeadSlot().plus(ONE);
    beaconChainUtil.setSlot(nextSlot);
    final SignedBeaconBlock block = beaconChainUtil.createBlockAtSlot(nextSlot);

    InternalValidationResult result = blockValidator.validate(block).join();
    assertTrue(result.isAccept());
  }

  @Test
  void shouldIgnoreAlreadyImportedBlock() throws Exception {
    final SignedBeaconBlock block =
        beaconChainUtil.createAndImportBlockAtSlot(recentChainData.getHeadSlot().plus(ONE));

    InternalValidationResult result = blockValidator.validate(block).join();
    assertTrue(result.isIgnore());
  }

  @Test
  void shouldReturnInvalidForSecondValidBlockForSlotAndProposer() throws Exception {
    final UInt64 nextSlot = recentChainData.getHeadSlot().plus(ONE);
    beaconChainUtil.setSlot(nextSlot);
    final SignedBeaconBlock block = beaconChainUtil.createBlockAtSlot(nextSlot);

    InternalValidationResult result1 = blockValidator.validate(block).join();
    assertTrue(result1.isAccept());

    InternalValidationResult result2 = blockValidator.validate(block).join();
    assertTrue(result2.isIgnore());
  }

  @Test
  void shouldReturnSavedForFutureForBlockFromFuture() throws Exception {
    final UInt64 nextSlot = recentChainData.getHeadSlot().plus(ONE);
    final SignedBeaconBlock block = beaconChainUtil.createBlockAtSlot(nextSlot);

    InternalValidationResult result = blockValidator.validate(block).join();
    assertTrue(result.isSaveForFuture());
  }

  @Test
  void shouldReturnSavedForFutureForBlockWithParentUnavailable() throws Exception {
    final UInt64 nextSlot = recentChainData.getHeadSlot().plus(ONE);
    beaconChainUtil.setSlot(nextSlot);

    final SignedBeaconBlock signedBlock = beaconChainUtil.createBlockAtSlot(nextSlot);
    final UInt64 proposerIndex = signedBlock.getMessage().getProposerIndex();
    final BeaconBlock block =
        new BeaconBlock(
            spec.getGenesisSchemaDefinitions().getBeaconBlockSchema(),
            signedBlock.getSlot(),
            proposerIndex,
            Bytes32.ZERO,
            signedBlock.getMessage().getStateRoot(),
            signedBlock.getMessage().getBody());

    BLSSignature blockSignature =
        beaconChainUtil
            .getSigner(proposerIndex.intValue())
            .signBlock(block, recentChainData.getBestState().orElseThrow().getForkInfo())
            .join();
    final SignedBeaconBlock blockWithNoParent =
        SignedBeaconBlock.create(spec, block, blockSignature);

    InternalValidationResult result = blockValidator.validate(blockWithNoParent).join();
    assertTrue(result.isSaveForFuture());
  }

  @Test
  void shouldReturnInvalidForBlockOlderThanFinalizedSlot() throws Exception {
    UInt64 finalizedEpoch = UInt64.valueOf(10);
    UInt64 finalizedSlot = spec.computeStartSlotAtEpoch(finalizedEpoch);
    final SignedBeaconBlock block = beaconChainUtil.createBlockAtSlot(finalizedSlot.minus(ONE));
    beaconChainUtil.finalizeChainAtEpoch(finalizedEpoch);
    beaconChainUtil.setSlot(recentChainData.getHeadSlot());

    InternalValidationResult result = blockValidator.validate(block).join();
    assertTrue(result.isIgnore());
  }

  @Test
  void shouldReturnInvalidForBlockWithWrongProposerIndex() throws Exception {
    final UInt64 nextSlot = recentChainData.getHeadSlot().plus(ONE);
    beaconChainUtil.setSlot(nextSlot);

    final SignedBeaconBlock signedBlock = beaconChainUtil.createBlockAtSlot(nextSlot);

    UInt64 invalidProposerIndex = signedBlock.getMessage().getProposerIndex().minus(ONE);

    final BeaconBlock block =
        new BeaconBlock(
            spec.getGenesisSchemaDefinitions().getBeaconBlockSchema(),
            signedBlock.getSlot(),
            invalidProposerIndex,
            signedBlock.getParentRoot(),
            signedBlock.getMessage().getStateRoot(),
            signedBlock.getMessage().getBody());

    BLSSignature blockSignature =
        beaconChainUtil
            .getSigner(invalidProposerIndex.intValue())
            .signBlock(block, recentChainData.getBestState().orElseThrow().getForkInfo())
            .join();
    final SignedBeaconBlock invalidProposerSignedBlock =
        SignedBeaconBlock.create(spec, block, blockSignature);

    InternalValidationResult result = blockValidator.validate(invalidProposerSignedBlock).join();
    assertTrue(result.isReject());
  }

  @Test
  void shouldReturnInvalidForBlockWithWrongSignature() throws Exception {
    final UInt64 nextSlot = recentChainData.getHeadSlot().plus(ONE);
    beaconChainUtil.setSlot(nextSlot);

    final SignedBeaconBlock block =
        SignedBeaconBlock.create(
            spec,
            beaconChainUtil.createBlockAtSlot(nextSlot).getMessage(),
            BLSTestUtil.randomSignature(0));

    InternalValidationResult result = blockValidator.validate(block).join();
    assertTrue(result.isReject());
  }

  @Test
  void shouldReturnInvalidForBlockThatDoesNotDescendFromFinalizedCheckpoint() {
    List<BLSKeyPair> VALIDATOR_KEYS = BLSKeyGenerator.generateKeyPairs(4);

    StorageSystem storageSystem = InMemoryStorageSystemBuilder.buildDefault();
    ChainBuilder chainBuilder = ChainBuilder.create(VALIDATOR_KEYS);
    ChainUpdater chainUpdater = new ChainUpdater(storageSystem.recentChainData(), chainBuilder);

    BlockValidator blockValidator = new BlockValidator(spec, storageSystem.recentChainData());
    chainUpdater.initializeGenesis();

    chainUpdater.updateBestBlock(chainUpdater.advanceChainUntil(1));

    ChainBuilder chainBuilderFork = chainBuilder.fork();
    ChainUpdater chainUpdaterFork =
        new ChainUpdater(storageSystem.recentChainData(), chainBuilderFork);

    UInt64 startSlotOfFinalizedEpoch = spec.computeStartSlotAtEpoch(UInt64.valueOf(4));

    chainUpdaterFork.advanceChain(20);

    chainUpdater.finalizeEpoch(4);

    SignedBlockAndState blockAndState =
        chainBuilderFork.generateBlockAtSlot(startSlotOfFinalizedEpoch.increment());
    chainUpdater.saveBlockTime(blockAndState);
    final SafeFuture<InternalValidationResult> result =
        blockValidator.validate(blockAndState.getBlock());
    assertThat(result).isCompletedWithValueMatching(InternalValidationResult::isReject);
  }
}
