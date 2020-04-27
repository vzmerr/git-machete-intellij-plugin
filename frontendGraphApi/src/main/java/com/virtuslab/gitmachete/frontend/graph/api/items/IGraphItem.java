package com.virtuslab.gitmachete.frontend.graph.api.items;

import com.intellij.ui.SimpleTextAttributes;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.graph.api.coloring.GraphItemColor;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;

/**
 * Graph items ({@link IGraphItem}, {@link IBranchItem}, {@link ICommitItem}) represent the DATA of the graph.
 * There is no strict concept of edge here (unlike {@link com.virtuslab.gitmachete.frontend.graph.api.elements.IGraphElement}
 * and {@link IRenderPart}).
 * Items relation is defined by indices of cells in the graph table.
 * */
public interface IGraphItem {

  /**
   * @return the index of previous sibling item (above in table) that is connected directly
   * and thus at the same indent level in graph as this one.
   */
  @GTENegativeOne
  int getPrevSiblingItemIndex();

  /**
   * @return the index of next sibling item (below in table) that is connected directly
   * and thus at the same indent level in graph as this one (hence it is not its child item).
   * <ul>
   *     <li>for a commit item, it's either another commit item or the containing branch (never null),</li>
   *     <li>for a branch item, it's either next sibling branch item or null (if none left)</li>
   * </ul>
   */
  @Nullable
  @Positive
  Integer getNextSiblingItemIndex();

  void setNextSiblingItemIndex(@Positive int i);

  /** @return the text (commit message/branch name) to be displayed in the table */
  String getValue();

  /** @return the attributes (eg. boldness) to be used by the displayed text */
  SimpleTextAttributes getAttributes();

  GraphItemColor getGraphItemColor();

  @NonNegative
  int getIndentLevel();

  boolean hasBulletPoint();

  /** @return the childItem which is an indented item (branch or commit) */
  boolean hasChildItem();

  boolean isBranchItem();

  IBranchItem asBranchItem();

  ICommitItem asCommitItem();
}