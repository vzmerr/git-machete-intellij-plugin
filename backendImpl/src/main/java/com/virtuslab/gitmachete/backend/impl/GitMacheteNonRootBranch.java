package com.virtuslab.gitmachete.backend.impl;

import java.util.Optional;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteCommit;
import com.virtuslab.gitmachete.backend.api.ISyncToRemoteStatus;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;

@Getter
@ToString
public final class GitMacheteNonRootBranch extends BaseGitMacheteNonRootBranch {
  private final String name;
  @ToString.Exclude
  @MonotonicNonNull
  private BaseGitMacheteBranch upstreamBranch = null;
  private final List<GitMacheteNonRootBranch> downstreamBranches;
  @Nullable
  private final IGitMacheteCommit forkPoint;
  private final IGitMacheteCommit pointedCommit;
  private final List<IGitMacheteCommit> commits;
  private final ISyncToRemoteStatus syncToRemoteStatus;
  private final SyncToParentStatus syncToParentStatus;
  @Nullable
  private final String customAnnotation;

  public GitMacheteNonRootBranch(String name,
      List<GitMacheteNonRootBranch> downstreamBranches,
      @Nullable IGitMacheteCommit forkPoint,
      IGitMacheteCommit pointedCommit,
      List<IGitMacheteCommit> commits,
      ISyncToRemoteStatus syncToRemoteStatus,
      SyncToParentStatus syncToParentStatus,
      @Nullable String customAnnotation) {
    this.name = name;
    this.downstreamBranches = downstreamBranches;
    this.forkPoint = forkPoint;
    this.pointedCommit = pointedCommit;
    this.commits = commits;
    this.syncToRemoteStatus = syncToRemoteStatus;
    this.syncToParentStatus = syncToParentStatus;
    this.customAnnotation = customAnnotation;

    // Note: since the class is final, `this` is already @Initialized at this point.

    // This is a hack necessary to create an immutable cyclic structure (children pointing at the parent & parent
    // pointing at the children).
    // This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
    // upstream data somewhere outside of GitMacheteBranch (e.g. in GitMacheteRepository).
    for (GitMacheteNonRootBranch branch : downstreamBranches) {
      branch.setUpstreamBranch(this);
    }
  }

  @Override
  public BaseGitMacheteBranch getUpstreamBranch() {
    assert upstreamBranch != null : "upstreamBranch hasn't been set yet";
    return upstreamBranch;
  }

  void setUpstreamBranch(BaseGitMacheteBranch givenUpstreamBranch) {
    assert upstreamBranch == null : "upstreamBranch has already been set";
    upstreamBranch = givenUpstreamBranch;
  }

  @Override
  public List<BaseGitMacheteNonRootBranch> getDownstreamBranches() {
    return List.narrow(downstreamBranches);
  }

  @Override
  public List<IGitMacheteCommit> getCommits() {
    return List.narrow(commits);
  }

  @Override
  public Optional<String> getCustomAnnotation() {
    return Optional.ofNullable(customAnnotation);
  }

  @Override
  public Optional<IGitMacheteCommit> getForkPoint() {
    return Optional.ofNullable(forkPoint);
  }
}
