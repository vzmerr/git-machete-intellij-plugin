package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static java.text.MessageFormat.format;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import io.vavr.collection.List;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.contextmenu.CheckoutSelectedBranchAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.ResetBranchToRemoteInfoDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BaseResetBranchToRemoteAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyProject,
      ISyncToRemoteStatusDependentAction {

  public static final String RESET_INFO_SHOWN = "git-machete.reset.info.shown";

  private static final String VCS_NOTIFIER_TITLE = getString(
      "action.GitMachete.BaseResetBranchToRemoteAction.notification.title");

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BaseResetBranchToRemoteAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getDescriptionActionName() {
    return getString("action.GitMachete.BaseResetBranchToRemoteAction.description-action-name");
  }

  @Override
  public @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getString("action.GitMachete.BaseResetBranchToRemoteAction.description.enabled");
  }

  @Override
  public List<SyncToRemoteStatus.Relation> getEligibleRelations() {
    return List.of(
        SyncToRemoteStatus.Relation.AheadOfRemote,
        SyncToRemoteStatus.Relation.BehindRemote,
        SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote,
        SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);

    syncToRemoteStatusDependentActionUpdate(anActionEvent);

    var branch = getNameOfBranchUnderAction(anActionEvent);
    if (branch.isDefined()) {
      var isResettingCurrent = getCurrentBranchNameIfManaged(anActionEvent)
          .map(bn -> bn.equals(branch.get())).getOrElse(false);
      if (anActionEvent.getPlace().equals(ActionPlaces.ACTION_PLACE_CONTEXT_MENU) && isResettingCurrent) {
        anActionEvent.getPresentation().setText(() -> getActionName());
      }
    }
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    Project project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent);
    var branchName = getNameOfBranchUnderAction(anActionEvent);
    var macheteRepository = getGitMacheteRepositorySnapshotWithLoggingOnEmpty(anActionEvent);

    if (branchName.isEmpty()) {
      VcsNotifier.getInstance(project).notifyError(VCS_NOTIFIER_TITLE,
          "Internal error occurred. For more information see IDE log file");
      return;
    }

    if (gitRepository.isEmpty()) {
      VcsNotifier.getInstance(project).notifyWarning(VCS_NOTIFIER_TITLE,
          "Skipping the action because no Git repository is selected");
      return;
    }

    if (macheteRepository.isEmpty()) {
      VcsNotifier.getInstance(project).notifyError(VCS_NOTIFIER_TITLE,
          "Internal error occurred. For more information see IDE log file");
      return;
    }

    // if key is missing the default value (false) is returned
    if (!PropertiesComponent.getInstance().getBoolean(RESET_INFO_SHOWN)) {
      final var okCancelDialogResult = MessageUtil.showOkCancelDialog(
          getString("action.GitMachete.BaseResetBranchToRemoteAction.info-dialog.title"),
          format(getString("action.GitMachete.BaseResetBranchToRemoteAction.info-dialog.message"), branchName.get()),
          getString("action.GitMachete.BaseResetBranchToRemoteAction.info-dialog.ok-text"),
          Messages.getCancelButton(),
          Messages.getInformationIcon(),
          new ResetBranchToRemoteInfoDialog(),
          project);
      if (okCancelDialogResult != Messages.OK) {
        return;
      }
    }

    doResetToRemoteWithKeep(project, gitRepository.get(), branchName.get(), macheteRepository.get(), anActionEvent);
  }

  protected void doResetToRemoteWithKeep(Project project, GitRepository gitRepository, String branchName,
      IGitMacheteRepositorySnapshot macheteRepository, AnActionEvent anActionEvent) {

    new Task.Backgroundable(project,
        getString("action.GitMachete.BaseResetBranchToRemoteAction.task-title"),
        /* canBeCancelled */ true) {

      @Override
      public void run(ProgressIndicator indicator) {
        log().debug(() -> "Resetting '${branchName}' branch");
        try (AccessToken ignored = DvcsUtil.workingTreeChangeStarted(project,
            getString("action.GitMachete.BaseResetBranchToRemoteAction.task-title"))) {
          // TODO (#444): prohibit reset on conflicts with uncommitted
          GitLineHandler resetHandler = new GitLineHandler(myProject, gitRepository.getRoot(), GitCommand.RESET);
          resetHandler.addParameters("--keep");

          var branchOption = macheteRepository.getManagedBranchByName(branchName);
          assert branchOption.isDefined() : "Can't get branch '${branchName}' from Git Machete repository";
          var remoteTrackingBranchOption = branchOption.get().getRemoteTrackingBranch();
          if (remoteTrackingBranchOption.isDefined()) {
            resetHandler.addParameters(remoteTrackingBranchOption.get().getPointedCommit().getHash());
          } else {
            String message = "Branch '${branchName}' doesn't have remote tracking branch, so cannot be reset";
            log().warn(message);
            VcsNotifier.getInstance(project).notifyWarning(VCS_NOTIFIER_TITLE, message);
            return;
          }

          resetHandler.endOptions();

          // Check if branch to reset is not current branch - if isn't then checkout
          var currentBranchOption = getCurrentBranchNameIfManagedWithLoggingOnEmpty(anActionEvent);
          if (currentBranchOption.isEmpty() || !currentBranchOption.get().equals(branchName)) {
            log().debug(() -> "Checkout to branch '${branchName}' is needed");
            // Checking out given branch
            CheckoutSelectedBranchAction.doCheckout(branchName, gitRepository, project, indicator);

            // Check again if we are in branch to reset to be sure that checkout was successful.
            // This time we are using git4idea because GitMacheteRepositorySnapshot is immutable
            // and it would return previous branch.
            @Nullable GitLocalBranch localBranch = gitRepository.getCurrentBranch();
            if (localBranch == null || !localBranch.getName().equals(branchName)) {
              log().error("Checkout to branch ${branchName} failed");
              VcsNotifier.getInstance(project).notifyError(VCS_NOTIFIER_TITLE,
                  "Error occurred during checkout of the branch that was to be reset! Operation aborted.");
              return;
            } else {
              log().debug(() -> "Checkout to branch '${branchName}' successful");
            }
          }

          GitCommandResult result = Git.getInstance().runCommand(resetHandler);

          // git4idea methods are used instead of our RefreshStatusAction, to refresh state not only in our plugin,
          // but also in whole IDE. Repository status will be refreshed also in our plugin because we are subscribed
          // to IDE event bus.
          refreshRepo(project, gitRepository);

          if (!result.success()) {
            log().error(result.getErrorOutputAsJoinedString());
            VcsNotifier.getInstance(project).notifyError(VCS_NOTIFIER_TITLE, result.getErrorOutputAsHtmlString());
            return;
          }
        }

        // If we are here this means that all went good
        VcsNotifier.getInstance(project).notifySuccess(
            format(getString("action.GitMachete.BaseResetBranchToRemoteAction.notification.success"), branchName));
        log().debug(() -> "Branch '${branchName}' reset to its remote tracking branch");
      }
    }.queue();
  }

  private void refreshRepo(Project project, GitRepository gitRepository) {
    GitRepositoryManager.getInstance(project).updateRepository(gitRepository.getRoot());
    // If `changes` is null the whole root will be refreshed
    GitUtil.refreshVfs(gitRepository.getRoot(), /* changes */ null);
  }
}
