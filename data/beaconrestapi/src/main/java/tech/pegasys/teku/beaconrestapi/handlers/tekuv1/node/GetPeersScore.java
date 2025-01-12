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

package tech.pegasys.teku.beaconrestapi.handlers.tekuv1.node;

import static tech.pegasys.teku.beaconrestapi.RestApiConstants.CACHE_NONE;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_INTERNAL_ERROR;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_OK;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.TAG_TEKU;

import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.teku.api.DataProvider;
import tech.pegasys.teku.api.NetworkDataProvider;
import tech.pegasys.teku.api.response.v1.teku.GetPeerScoresResponse;
import tech.pegasys.teku.api.response.v1.teku.PeerScore;
import tech.pegasys.teku.provider.JsonProvider;

public class GetPeersScore implements Handler {
  public static final String ROUTE = "/teku/v1/nodes/peer_scores";
  private final JsonProvider jsonProvider;
  private final NetworkDataProvider network;

  public GetPeersScore(final DataProvider provider, final JsonProvider jsonProvider) {
    this.jsonProvider = jsonProvider;
    this.network = provider.getNetworkDataProvider();
  }

  GetPeersScore(final NetworkDataProvider network, final JsonProvider jsonProvider) {
    this.network = network;
    this.jsonProvider = jsonProvider;
  }

  @OpenApi(
      path = ROUTE,
      method = HttpMethod.GET,
      summary = "Get peer scores",
      tags = {TAG_TEKU},
      description = "Retrieves data about the node's network peers.",
      responses = {
        @OpenApiResponse(
            status = RES_OK,
            content = @OpenApiContent(from = GetPeerScoresResponse.class)),
        @OpenApiResponse(status = RES_INTERNAL_ERROR)
      })
  @Override
  public void handle(@NotNull final Context ctx) throws Exception {
    ctx.header(Header.CACHE_CONTROL, CACHE_NONE);
    List<PeerScore> peerScores = network.getPeerScores();

    ctx.result(jsonProvider.objectToJSON(new GetPeerScoresResponse(peerScores)));
  }
}
