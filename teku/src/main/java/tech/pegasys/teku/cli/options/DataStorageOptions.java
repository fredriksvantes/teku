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

import picocli.CommandLine;
import picocli.CommandLine.Help.Visibility;
import tech.pegasys.teku.config.TekuConfiguration;
import tech.pegasys.teku.storage.server.DatabaseVersion;
import tech.pegasys.teku.storage.server.StateStorageMode;
import tech.pegasys.teku.storage.server.VersionedDatabaseFactory;

public class DataStorageOptions {
  @CommandLine.Option(
      names = {"--data-storage-mode"},
      paramLabel = "<STORAGE_MODE>",
      description =
          "Sets the strategy for handling historical chain data.  (Valid values: ${COMPLETION-CANDIDATES})",
      arity = "1")
  private StateStorageMode dataStorageMode = StateStorageMode.PRUNE;

  @CommandLine.Option(
      names = {"--data-storage-archive-frequency"},
      paramLabel = "<FREQUENCY>",
      description =
          "Sets the frequency, in slots, at which to store finalized states to disk. "
              + "This option is ignored if --data-storage-mode is set to PRUNE",
      arity = "1")
  private long dataStorageFrequency = VersionedDatabaseFactory.DEFAULT_STORAGE_FREQUENCY;

  @CommandLine.Option(
      names = {"--Xdata-storage-create-db-version"},
      paramLabel = "<VERSION>",
      description = "Database version to create",
      arity = "1",
      hidden = true)
  private String createDbVersion = DatabaseVersion.DEFAULT_VERSION.getValue();

  @CommandLine.Option(
      names = {"--data-storage-non-canonical-blocks-enabled"},
      paramLabel = "<BOOLEAN>",
      showDefaultValue = Visibility.ALWAYS,
      description = "Store non-canonical blocks",
      fallbackValue = "true",
      arity = "0..1")
  private boolean storeNonCanonicalBlocksEnabled = false;

  public StateStorageMode getDataStorageMode() {
    return dataStorageMode;
  }

  public void configure(final TekuConfiguration.Builder builder) {
    builder.storageConfiguration(
        b ->
            b.dataStorageMode(dataStorageMode)
                .dataStorageFrequency(dataStorageFrequency)
                .dataStorageCreateDbVersion(parseDatabaseVersion())
                .storeNonCanonicalBlocks(storeNonCanonicalBlocksEnabled));
  }

  private DatabaseVersion parseDatabaseVersion() {
    return DatabaseVersion.fromString(createDbVersion).orElse(DatabaseVersion.DEFAULT_VERSION);
  }

  public boolean isStoreNonCanonicalBlocks() {
    return storeNonCanonicalBlocksEnabled;
  }
}
