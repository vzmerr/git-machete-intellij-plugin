package com.virtuslab.gitcore.impl.jgit;

import java.time.Instant;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.eclipse.jgit.revwalk.RevCommit;

import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreCommitHash;
import com.virtuslab.gitcore.api.IGitCoreTreeHash;

@ExtensionMethod({GitCoreCommitHash.class, GitCoreTreeHash.class})
@Getter
public class GitCoreCommit implements IGitCoreCommit {
  private final String shortMessage;
  private final String fullMessage;
  private final Instant commitTime;
  private final IGitCoreCommitHash hash;
  private final IGitCoreTreeHash treeHash;

  public GitCoreCommit(@NonLeaked RevCommit commit) {
    this.shortMessage = commit.getShortMessage();
    this.fullMessage = commit.getFullMessage();
    this.commitTime = Instant.ofEpochSecond(commit.getCommitTime());
    this.hash = commit.getId().toGitCoreCommitHash();
    this.treeHash = commit.getTree().getId().toGitCoreTreeHash();
  }

  @Override
  public String toString() {
    return hash.getShortHashString() + " ('" + shortMessage + "')";
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return IGitCoreCommit.defaultEquals(this, other);
  }

  @Override
  public final int hashCode() {
    return IGitCoreCommit.defaultHashCode(this);
  }
}
