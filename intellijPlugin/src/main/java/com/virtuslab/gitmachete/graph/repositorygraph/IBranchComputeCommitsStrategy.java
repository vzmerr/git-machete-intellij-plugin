package com.virtuslab.gitmachete.graph.repositorygraph;

import com.virtuslab.gitcore.gitcoreapi.GitException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import java.util.List;

public interface IBranchComputeCommitsStrategy {
  List<IGitMacheteCommit> computeCommitsOf(IGitMacheteBranch branch) throws GitException;
}