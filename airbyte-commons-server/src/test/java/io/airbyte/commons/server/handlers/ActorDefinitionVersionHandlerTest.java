/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.server.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.airbyte.api.model.generated.ActorDefinitionVersionBreakingChanges;
import io.airbyte.api.model.generated.ActorDefinitionVersionRead;
import io.airbyte.api.model.generated.DestinationIdRequestBody;
import io.airbyte.api.model.generated.SourceIdRequestBody;
import io.airbyte.commons.version.Version;
import io.airbyte.config.ActorDefinitionBreakingChange;
import io.airbyte.config.ActorDefinitionVersion;
import io.airbyte.config.ActorDefinitionVersion.SupportState;
import io.airbyte.config.DestinationConnection;
import io.airbyte.config.ReleaseStage;
import io.airbyte.config.SourceConnection;
import io.airbyte.config.StandardDestinationDefinition;
import io.airbyte.config.StandardSourceDefinition;
import io.airbyte.config.persistence.ActorDefinitionVersionHelper;
import io.airbyte.config.persistence.ConfigNotFoundException;
import io.airbyte.config.persistence.ConfigRepository;
import io.airbyte.validation.json.JsonValidationException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ActorDefinitionVersionHandlerTest {

  private static final UUID WORKSPACE_ID = UUID.randomUUID();
  private static final UUID ACTOR_DEFINITION_ID = UUID.randomUUID();
  private static final StandardSourceDefinition SOURCE_DEFINITION = new StandardSourceDefinition()
      .withSourceDefinitionId(UUID.randomUUID());
  private static final StandardDestinationDefinition DESTINATION_DEFINITION = new StandardDestinationDefinition()
      .withDestinationDefinitionId(UUID.randomUUID());

  private ConfigRepository mConfigRepository;
  private ActorDefinitionVersionHelper mActorDefinitionVersionHelper;

  private ActorDefinitionVersionHandler actorDefinitionVersionHandler;

  @BeforeEach
  void setUp() {
    mConfigRepository = mock(ConfigRepository.class);
    mActorDefinitionVersionHelper = mock(ActorDefinitionVersionHelper.class);
    actorDefinitionVersionHandler = new ActorDefinitionVersionHandler(mConfigRepository, mActorDefinitionVersionHelper);
  }

  private ActorDefinitionVersion createActorDefinitionVersion() {
    return new ActorDefinitionVersion()
        .withActorDefinitionId(ACTOR_DEFINITION_ID)
        .withVersionId(UUID.randomUUID())
        .withReleaseStage(ReleaseStage.BETA)
        .withSupportState(SupportState.SUPPORTED)
        .withDockerRepository("airbyte/source-faker")
        .withDockerImageTag("1.0.2")
        .withDocumentationUrl("https://docs.airbyte.io");
  }

  @ParameterizedTest
  @CsvSource({"true", "false"})
  void testGetActorDefinitionVersionForSource(final boolean isSourceVersionDefault)
      throws JsonValidationException, ConfigNotFoundException, IOException {
    final UUID sourceId = UUID.randomUUID();
    final ActorDefinitionVersion actorDefinitionVersion = createActorDefinitionVersion();
    final SourceConnection sourceConnection = new SourceConnection()
        .withSourceId(sourceId)
        .withWorkspaceId(WORKSPACE_ID);

    if (isSourceVersionDefault) {
      sourceConnection.withDefaultVersionId(actorDefinitionVersion.getVersionId());
    }

    when(mConfigRepository.getSourceConnection(sourceId))
        .thenReturn(sourceConnection);
    when(mConfigRepository.getSourceDefinitionFromSource(sourceId))
        .thenReturn(SOURCE_DEFINITION);
    when(mActorDefinitionVersionHelper.getSourceVersion(SOURCE_DEFINITION, WORKSPACE_ID, sourceId))
        .thenReturn(actorDefinitionVersion);

    final SourceIdRequestBody sourceIdRequestBody = new SourceIdRequestBody().sourceId(sourceId);
    final ActorDefinitionVersionRead actorDefinitionVersionRead =
        actorDefinitionVersionHandler.getActorDefinitionVersionForSourceId(sourceIdRequestBody);
    final ActorDefinitionVersionRead expectedRead = new ActorDefinitionVersionRead()
        .isActorDefaultVersion(isSourceVersionDefault)
        .supportState(io.airbyte.api.model.generated.SupportState.SUPPORTED)
        .dockerRepository(actorDefinitionVersion.getDockerRepository())
        .dockerImageTag(actorDefinitionVersion.getDockerImageTag());

    assertEquals(expectedRead, actorDefinitionVersionRead);
    verify(mConfigRepository).getSourceConnection(sourceId);
    verify(mConfigRepository).getSourceDefinitionFromSource(sourceId);
    verify(mActorDefinitionVersionHelper).getSourceVersion(SOURCE_DEFINITION, WORKSPACE_ID, sourceId);
    verify(mConfigRepository).listBreakingChangesForActorDefinitionVersion(actorDefinitionVersion);
    verifyNoMoreInteractions(mConfigRepository);
    verifyNoMoreInteractions(mActorDefinitionVersionHelper);
  }

  @ParameterizedTest
  @CsvSource({"true", "false"})
  void testGetActorDefinitionVersionForDestination(final boolean isDestinationVersionDefault)
      throws JsonValidationException, ConfigNotFoundException, IOException {
    final UUID destinationId = UUID.randomUUID();
    final ActorDefinitionVersion actorDefinitionVersion = createActorDefinitionVersion();
    final DestinationConnection destinationConnection = new DestinationConnection()
        .withDestinationId(destinationId)
        .withWorkspaceId(WORKSPACE_ID);

    if (isDestinationVersionDefault) {
      destinationConnection.withDefaultVersionId(actorDefinitionVersion.getVersionId());
    }

    when(mConfigRepository.getDestinationConnection(destinationId))
        .thenReturn(destinationConnection);
    when(mConfigRepository.getDestinationDefinitionFromDestination(destinationId))
        .thenReturn(DESTINATION_DEFINITION);
    when(mActorDefinitionVersionHelper.getDestinationVersion(DESTINATION_DEFINITION, WORKSPACE_ID, destinationId))
        .thenReturn(actorDefinitionVersion);

    final DestinationIdRequestBody destinationIdRequestBody = new DestinationIdRequestBody().destinationId(destinationId);
    final ActorDefinitionVersionRead actorDefinitionVersionRead =
        actorDefinitionVersionHandler.getActorDefinitionVersionForDestinationId(destinationIdRequestBody);
    final ActorDefinitionVersionRead expectedRead = new ActorDefinitionVersionRead()
        .isActorDefaultVersion(isDestinationVersionDefault)
        .supportState(io.airbyte.api.model.generated.SupportState.SUPPORTED)
        .dockerRepository(actorDefinitionVersion.getDockerRepository())
        .dockerImageTag(actorDefinitionVersion.getDockerImageTag());

    assertEquals(expectedRead, actorDefinitionVersionRead);
    verify(mConfigRepository).getDestinationConnection(destinationId);
    verify(mConfigRepository).getDestinationDefinitionFromDestination(destinationId);
    verify(mActorDefinitionVersionHelper).getDestinationVersion(DESTINATION_DEFINITION, WORKSPACE_ID, destinationId);
    verify(mConfigRepository).listBreakingChangesForActorDefinitionVersion(actorDefinitionVersion);
    verifyNoMoreInteractions(mConfigRepository);
    verifyNoMoreInteractions(mActorDefinitionVersionHelper);
  }

  @Test
  void testCreateActorDefinitionVersionReadWithBreakingChange() throws IOException {
    final List<ActorDefinitionBreakingChange> breakingChanges = List.of(
        new ActorDefinitionBreakingChange()
            .withActorDefinitionId(ACTOR_DEFINITION_ID)
            .withMigrationDocumentationUrl("https://docs.airbyte.io/2")
            .withVersion(new Version("2.0.0"))
            .withUpgradeDeadline("2023-01-01")
            .withMessage("This is a breaking change"),
        new ActorDefinitionBreakingChange()
            .withActorDefinitionId(ACTOR_DEFINITION_ID)
            .withMigrationDocumentationUrl("https://docs.airbyte.io/3")
            .withVersion(new Version("3.0.0"))
            .withUpgradeDeadline("2023-05-01")
            .withMessage("This is another breaking change"));

    final ActorDefinitionVersion actorDefinitionVersion = createActorDefinitionVersion().withSupportState(SupportState.DEPRECATED);
    when(mConfigRepository.listBreakingChangesForActorDefinitionVersion(actorDefinitionVersion)).thenReturn(breakingChanges);

    final ActorDefinitionVersionRead actorDefinitionVersionRead =
        actorDefinitionVersionHandler.createActorDefinitionVersionRead(actorDefinitionVersion, true);

    final ActorDefinitionVersionRead expectedRead = new ActorDefinitionVersionRead()
        .isActorDefaultVersion(true)
        .supportState(io.airbyte.api.model.generated.SupportState.DEPRECATED)
        .dockerRepository(actorDefinitionVersion.getDockerRepository())
        .dockerImageTag(actorDefinitionVersion.getDockerImageTag())
        .breakingChanges(new ActorDefinitionVersionBreakingChanges()
            .minUpgradeDeadline(LocalDate.parse("2023-01-01"))
            .upcomingBreakingChanges(List.of(
                new io.airbyte.api.model.generated.ActorDefinitionBreakingChange()
                    .migrationDocumentationUrl("https://docs.airbyte.io/2")
                    .version("2.0.0")
                    .upgradeDeadline(LocalDate.parse(("2023-01-01")))
                    .message("This is a breaking change"),
                new io.airbyte.api.model.generated.ActorDefinitionBreakingChange()
                    .migrationDocumentationUrl("https://docs.airbyte.io/3")
                    .version("3.0.0")
                    .upgradeDeadline(LocalDate.parse("2023-05-01"))
                    .message("This is another breaking change"))));

    assertEquals(expectedRead, actorDefinitionVersionRead);
    verify(mConfigRepository).listBreakingChangesForActorDefinitionVersion(actorDefinitionVersion);
    verifyNoMoreInteractions(mConfigRepository);
    verifyNoInteractions(mActorDefinitionVersionHelper);
  }

}
