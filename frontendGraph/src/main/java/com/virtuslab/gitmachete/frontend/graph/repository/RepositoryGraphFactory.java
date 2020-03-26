package com.virtuslab.gitmachete.frontend.graph.repository;

import static com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphBuilder.DEFAULT_GET_COMMITS;
import static com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphBuilder.EMPTY_GET_COMMITS;

import javax.annotation.Nullable;

import lombok.Getter;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public class RepositoryGraphFactory {

  @Getter
  public static final RepositoryGraph nullRepositoryGraph = RepositoryGraph.getNullRepositoryGraph();

  private RepositoryGraph repositoryGraphWithCommits;
  private RepositoryGraph repositoryGraphWithoutCommits;
  private IGitMacheteRepository repository;

  public RepositoryGraph getRepositoryGraph(@Nullable IGitMacheteRepository givenRepository, boolean isListingCommits) {
    if (givenRepository == null) {
      return nullRepositoryGraph;
    }

    if (givenRepository != this.repository) {
      this.repository = givenRepository;

      RepositoryGraphBuilder repositoryGraphBuilder = new RepositoryGraphBuilder().repository(givenRepository);
      repositoryGraphWithCommits = repositoryGraphBuilder.branchGetCommitsStrategy(DEFAULT_GET_COMMITS).build();
      repositoryGraphWithoutCommits = repositoryGraphBuilder.branchGetCommitsStrategy(EMPTY_GET_COMMITS).build();
    }
    return isListingCommits ? repositoryGraphWithCommits : repositoryGraphWithoutCommits;
  }
}
