package com.virtuslab.gitmachete.frontend.graph.repositorygraph;

import static com.virtuslab.gitmachete.frontend.graph.repositorygraph.RepositoryGraphBuilder.DEFAULT_COMPUTE_COMMITS;
import static com.virtuslab.gitmachete.frontend.graph.repositorygraph.RepositoryGraphBuilder.EMPTY_COMPUTE_COMMITS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

public class RepositoryGraphFactory {

  @Getter
  public static final RepositoryGraph nullRepositoryGraph = RepositoryGraph.getNullRepositoryGraph();

  private RepositoryGraph repositoryGraphWithCommits;
  private RepositoryGraph repositoryGraphWithoutCommits;
  private IGitMacheteRepository repository;

  @Nonnull
  public RepositoryGraph getRepositoryGraph(@Nullable IGitMacheteRepository repository, boolean isListingCommits) {
    if (repository == null) {
      return nullRepositoryGraph;
    } else {
      if (repository != this.repository) {
        this.repository = repository;

        // TODO (#40): use io.vavr.Lazy (?)
        RepositoryGraphBuilder repositoryGraphBuilder = new RepositoryGraphBuilder().repository(repository);
        repositoryGraphWithCommits = repositoryGraphBuilder.branchComputeCommitsStrategy(DEFAULT_COMPUTE_COMMITS)
            .build();
        repositoryGraphWithoutCommits = repositoryGraphBuilder.branchComputeCommitsStrategy(EMPTY_COMPUTE_COMMITS)
            .build();
      }
      return isListingCommits ? repositoryGraphWithCommits : repositoryGraphWithoutCommits;
    }
  }
}