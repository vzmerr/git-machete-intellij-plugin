package com.virtuslab.gitmachete.frontend.ui.impl.table;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.util.ModalityUiUtil;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.val;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.ui.api.gitrepositoryselection.IGitRepositorySelectionProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.BranchLayoutWriterProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

@ExtensionMethod(GitVfsUtils.class)
@AllArgsConstructor
@CustomLog
public class GitMacheteRepositoryDiscoverer {

  private final Project project;
  private final IGitRepositorySelectionProvider gitRepositorySelectionProvider;
  private final Consumer<Path> onFailurePathConsumer;
  private final Consumer<IGitMacheteRepositorySnapshot> onSuccessRepositoryConsumer;

  public void enqueue(Path macheteFilePath) {
    LOG.info("Enqueuing automatic discover");
    val selectedRepository = gitRepositorySelectionProvider.getSelectedGitRepository();
    if (selectedRepository == null) {
      LOG.error("Can't do automatic discover because of undefined selected repository");
      return;
    }
    Path rootDirPath = selectedRepository.getRootDirectoryPath().toAbsolutePath();
    Path mainGitDirPath = selectedRepository.getMainGitDirectoryPath().toAbsolutePath();
    Path worktreeGitDirPath = selectedRepository.getWorktreeGitDirectoryPath().toAbsolutePath();

    new Task.Backgroundable(project, getString("string.GitMachete.EnhancedGraphTable.automatic-discover.task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        LOG.debug("Running automatic discover task");
        val discoverRunResult = Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
            .getInstance(rootDirPath, mainGitDirPath, worktreeGitDirPath).discoverLayoutAndCreateSnapshot());

        if (discoverRunResult.isFailure()) {
          LOG.debug("Discover and snapshot creation failed");
          val exception = discoverRunResult.getCause();
          ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  exception.getMessage() != null ? exception.getMessage() : ""));
          return;
        }

        val repositorySnapshot = discoverRunResult.get();

        if (repositorySnapshot.getRootBranches().size() == 0) {
          LOG.debug("No root branches discovered - executing on-failure consumer");
          onFailurePathConsumer.accept(macheteFilePath);
          return;
        }

        val branchLayoutWriter = project.getService(BranchLayoutWriterProvider.class).getBranchLayoutWriter();
        val branchLayout = repositorySnapshot.getBranchLayout();

        try {
          LOG.debug("Writing branch layout & executing on-success consumer");
          branchLayoutWriter.write(macheteFilePath, branchLayout, /* backupOldLayout */ true);
          onSuccessRepositoryConsumer.accept(repositorySnapshot);
        } catch (BranchLayoutException exception) {
          LOG.debug("Handling branch layout exception");
          ModalityUiUtil.invokeLaterIfNeeded(NON_MODAL, () -> VcsNotifier.getInstance(project)
              .notifyError(
                  /* displayId */ null,
                  getString(
                      "string.GitMachete.EnhancedGraphTable.automatic-discover.notification.title.cannot-discover-layout-error"),
                  exception.getMessage() != null ? exception.getMessage() : ""));
        }
      }
    }.queue();
  }
}
