package com.virtuslab.gitmachete.testcommon;

import static com.virtuslab.gitmachete.testcommon.TestFileUtils.copyScriptFromResources;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.prepareRepoFromScript;

import java.nio.file.Files;
import java.nio.file.Path;

import lombok.SneakyThrows;

public abstract class BaseGitRepositoryBackedIntegrationTestSuite {

  protected final String scriptName;
  protected final Path parentDirectoryPath;
  protected final Path rootDirectoryPath;
  protected final Path mainGitDirectoryPath;
  protected final Path worktreeGitDirectoryPath;

  @SneakyThrows
  protected BaseGitRepositoryBackedIntegrationTestSuite(String scriptName) {
    this.scriptName = scriptName;
    parentDirectoryPath = Files.createTempDirectory("machete-tests-");
    if (scriptName.equals("setup-with-single-remote.sh")) {
      rootDirectoryPath = parentDirectoryPath.resolve("machete-sandbox-worktree");
      mainGitDirectoryPath = parentDirectoryPath.resolve("machete-sandbox").resolve(".git");
      worktreeGitDirectoryPath = mainGitDirectoryPath.resolve("worktrees").resolve("machete-sandbox-worktree");
    } else {
      rootDirectoryPath = parentDirectoryPath.resolve("machete-sandbox");
      mainGitDirectoryPath = rootDirectoryPath.resolve(".git");
      worktreeGitDirectoryPath = rootDirectoryPath.resolve(".git");
    }

    copyScriptFromResources("common.sh", parentDirectoryPath);
    copyScriptFromResources(scriptName, parentDirectoryPath);
    prepareRepoFromScript(scriptName, parentDirectoryPath);
  }
}
