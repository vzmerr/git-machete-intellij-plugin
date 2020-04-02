package com.virtuslab.gitmachete.frontend.actions;

import java.util.Optional;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;

import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_SELECTED_BRANCH_NAME}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public class RebaseSelectedBranchOntoParentAction extends BaseRebaseBranchOntoParentAction {

  @Override
  public void update(AnActionEvent anActionEvent) {
    super.update(anActionEvent);
    prohibitRootBranchRebase(anActionEvent);
    updateDescriptionIfApplicable(anActionEvent);
  }

  /**
   * Assumption to following code:
   * - the result of {@link RebaseSelectedBranchOntoParentAction#getSelectedMacheteBranch}
   * is present and it is not a root branch because if it was not the user wouldn't be able to perform action in the first place
   */
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    Optional<BaseGitMacheteBranch> selectedGitMacheteBranch = getSelectedMacheteBranch(anActionEvent);
    assert selectedGitMacheteBranch.isPresent();

    var branchToRebase = selectedGitMacheteBranch.get().asNonRootBranch();
    doRebase(anActionEvent, branchToRebase);
  }

  private void updateDescriptionIfApplicable(AnActionEvent anActionEvent) {
    var presentation = anActionEvent.getPresentation();
    Optional<BaseGitMacheteBranch> selectedBranch = getSelectedMacheteBranch(anActionEvent);
    if (presentation.isEnabledAndVisible() && selectedBranch.isPresent()) {
      var nonRootBranch = selectedBranch.get().asNonRootBranch();
      BaseGitMacheteBranch upstream = nonRootBranch.getUpstreamBranch();
      String description = String.format("Rebase \"%s\" onto \"%s\"", nonRootBranch.getName(), upstream.getName());
      presentation.setDescription(description);
    }
  }

  private void prohibitRootBranchRebase(AnActionEvent anActionEvent) {
    Optional<BaseGitMacheteBranch> selectedBranch = getSelectedMacheteBranch(anActionEvent);

    var presentation = anActionEvent.getPresentation();
    if (presentation.isVisible() && selectedBranch.isPresent() && selectedBranch.get().isRootBranch()) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
    }
  }

  private Optional<BaseGitMacheteBranch> getSelectedMacheteBranch(AnActionEvent anActionEvent) {
    IGitMacheteRepository gitMacheteRepository = getMacheteRepository(anActionEvent);
    String selectedBranchName = anActionEvent.getData(DataKeys.KEY_SELECTED_BRANCH_NAME);
    assert selectedBranchName != null : "Can't get selected branch";
    return gitMacheteRepository.getBranchByName(selectedBranchName);
  }
}
