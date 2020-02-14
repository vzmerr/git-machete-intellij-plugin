package com.virtuslab.branchrelationfile.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface IBranchRelationFile {
  void saveToFile(boolean backupOldFile) throws IOException, BranchRelationFileException;

  Path getPath();

  List<IBranchRelationFileEntry> getRootBranches();

  Character getIndentType();

  int getLevelWidth();

  Optional<IBranchRelationFileEntry> findBranchByName(String branchName);

  IBranchRelationFile withBranchSlidOut(String branchName)
      throws BranchRelationFileException, CloneNotSupportedException, IOException;

  IBranchRelationFile withBranchSlidOut(IBranchRelationFileEntry relationFileEntry)
      throws BranchRelationFileException, CloneNotSupportedException, IOException;
}