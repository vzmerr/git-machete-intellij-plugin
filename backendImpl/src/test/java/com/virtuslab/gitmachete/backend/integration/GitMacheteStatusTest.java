package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.AheadOfRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.Untracked;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.branchlayout.api.manager.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.BaseGitRepositoryTest;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryFactory;

public class GitMacheteStatusTest extends BaseGitRepositoryTest {
  IGitMacheteRepository gitMacheteRepository = null;
  GitMacheteRepositoryFactory gitMacheteRepositoryFactory = new GitMacheteRepositoryFactory();
  IBranchLayoutReader branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);

  public GitMacheteStatusTest() throws IOException {}

  protected void init(String scriptName) throws Exception {
    super.init(scriptName);
    IBranchLayout branchLayout = branchLayoutReader.read(repositoryGitDir.resolve("machete"));
    gitMacheteRepository = gitMacheteRepositoryFactory.create(repositoryMainDir, repositoryGitDir, branchLayout);
  }

  @Test
  public void statusTest() throws Exception {
    init("setup-with-single-remote.sh");

    String ourResult = repositoryStatus();
    String gitMacheteCliStatus = gitMacheteCliStatus();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println("OUR OUTPUT:");
    System.out.println(ourResult);

    Assert.assertEquals(gitMacheteCliStatus, ourResult);
  }

  @Test
  public void statusTestWithMultiRemotes() throws Exception {
    init("setup-with-multiple-remotes.sh");

    String ourResult = repositoryStatus();
    String gitMacheteCliStatus = gitMacheteCliStatus();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println("OUR OUTPUT:");
    System.out.println(ourResult);

    Assert.assertEquals(gitMacheteCliStatus, ourResult);
  }

  @Test
  public void statusTestForDivergedAndOlderThan() throws Exception {
    init("setup-for-diverged-and-older-than.sh");

    String ourResult = repositoryStatus();
    String gitMacheteCliStatus = gitMacheteCliStatus();

    System.out.println("CLI OUTPUT:");
    System.out.println(gitMacheteCliStatus);
    System.out.println("OUR OUTPUT:");
    System.out.println(ourResult);

    Assert.assertEquals(gitMacheteCliStatus, ourResult);
  }

  private String gitMacheteCliStatus() throws IOException {
    var gitMacheteProcessBuilder = new ProcessBuilder();
    gitMacheteProcessBuilder.command("git", "machete", "status", "-l");
    gitMacheteProcessBuilder.directory(repositoryMainDir.toFile());
    var gitMacheteProcess = gitMacheteProcessBuilder.start();
    return convertStreamToString(gitMacheteProcess.getInputStream());
  }

  private String repositoryStatus() {
    var sb = new StringBuilder();
    var branches = gitMacheteRepository.getRootBranches();
    int lastRootBranchIndex = branches.size() - 1;
    for (int currentRootBranch = 0; currentRootBranch <= lastRootBranchIndex; currentRootBranch++) {
      var b = branches.get(currentRootBranch);
      printRootBranch(b, sb);
      if (currentRootBranch < lastRootBranchIndex)
        sb.append(System.lineSeparator());
    }

    return sb.toString();
  }

  private void printRootBranch(BaseGitMacheteRootBranch branch, StringBuilder sb) {
    sb.append("  ");
    printCommonParts(branch, /* level */ 0, sb);
  }

  private void printNonRootBranch(BaseGitMacheteNonRootBranch branch, int level, StringBuilder sb) {
    sb.append("  ");

    sb.append("| ".repeat(level));

    sb.append(System.lineSeparator());

    var commits = branch.getCommits().reverse();

    for (var c : commits) {
      sb.append("  ");
      sb.append("| ".repeat(level));
      sb.append(c.getMessage().split("\n", 2)[0]);
      sb.append(System.lineSeparator());
    }

    sb.append("  ");
    sb.append("| ".repeat(level - 1));

    var parentStatus = branch.getSyncToParentStatus();
    if (parentStatus == SyncToParentStatus.InSync)
      sb.append("o");
    else if (parentStatus == SyncToParentStatus.OutOfSync)
      sb.append("x");
    else if (parentStatus == SyncToParentStatus.InSyncButForkPointOff)
      sb.append("?");
    else if (parentStatus == SyncToParentStatus.MergedToParent)
      sb.append("m");
    sb.append("-");

    printCommonParts(branch, level, sb);
  }

  private void printCommonParts(BaseGitMacheteBranch branch, int level, StringBuilder sb) {
    sb.append(branch.getName());

    var currBranch = gitMacheteRepository.getCurrentBranchIfManaged();
    if (currBranch.isDefined() && currBranch.get().equals(branch))
      sb.append(" *");

    var customAnnotation = branch.getCustomAnnotation();
    if (customAnnotation.isDefined()) {
      sb.append("  ");
      sb.append(customAnnotation.get());
    }
    var syncToRemote = branch.getSyncToRemoteStatus();
    if (syncToRemote.getRelation() != InSyncToRemote) {
      sb.append(" (");
      sb.append(Match(syncToRemote.getRelation()).of(
          Case($(AheadOfRemote), "ahead of " + syncToRemote.getRemoteName()),
          Case($(BehindRemote), "behind " + syncToRemote.getRemoteName()),
          Case($(Untracked), "untracked"),
          Case($(DivergedFromAndNewerThanRemote), "diverged from " + syncToRemote.getRemoteName()),
          Case($(DivergedFromAndOlderThanRemote), "diverged from & older than " + syncToRemote.getRemoteName())));
      sb.append(")");
    }
    sb.append(System.lineSeparator());

    for (var b : branch.getDownstreamBranches()) {
      printNonRootBranch(b, /* level */ level + 1, sb);
    }
  }

  private static String convertStreamToString(InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
