/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManager.RoleEvent;
import net.sourceforge.ganttproject.task.ResourceAssignment;

public class ResourceTreeTable extends GPTreeTableBase {
  private final RoleManager myRoleManager;

  private final ResourceTreeTableModel myResourceTreeModel;

  private final UIFacade myUiFacade;

  private static enum DefaultColumn {
    NAME(new ColumnList.ColumnStub("0", null, true, 0, 200)), ROLE(new ColumnList.ColumnStub("1",
        null, true, 1, 75)), EMAIL(new ColumnList.ColumnStub("2", null, false, -1, 75)), PHONE(
        new ColumnList.ColumnStub("3", null, false, -1, 50)), ROLE_IN_TASK(new ColumnList.ColumnStub(
        "4", null, false, -1, 75));

    private final Column myDelegate;

    private DefaultColumn(ColumnList.Column delegate) {
      myDelegate = delegate;
    }

    Column getStub() {
      return myDelegate;
    }

    static List<Column> getColumnStubs() {
      List<Column> result = new ArrayList<Column>();
      for (DefaultColumn dc : values()) {
        result.add(dc.myDelegate);
      }
      return result;
    }
  }

  public ResourceTreeTable(IGanttProject project, ResourceTreeTableModel model, UIFacade uiFacade) {
    super(project, uiFacade, project.getResourceCustomPropertyManager(), model);
    myUiFacade = uiFacade;
    myRoleManager = project.getRoleManager();
    myRoleManager.addRoleListener(new RoleManager.Listener() {
      @Override
      public void rolesChanged(RoleEvent e) {
        setEditor(getTableHeaderUiFacade().findColumnByID(DefaultColumn.ROLE.getStub().getID()));
        setEditor(getTableHeaderUiFacade().findColumnByID(DefaultColumn.ROLE_IN_TASK.getStub().getID()));
      }

      private void setEditor(ColumnImpl column) {
        if (column == null || column.getTableColumnExt() == null) {
          return;
        }
        JComboBox comboBox = new JComboBox(getRoleManager().getEnabledRoles());
        comboBox.setEditable(false);
        column.getTableColumnExt().setCellEditor(new DefaultCellEditor(comboBox));
      }
    });
    myResourceTreeModel = model;
    getTableHeaderUiFacade().createDefaultColumns(DefaultColumn.getColumnStubs());
    setTreeTableModel(model);
    myResourceTreeModel.setSelectionModel(getTreeSelectionModel());
  }

  public boolean isVisible(DefaultMutableTreeTableNode node) {
    return getTreeTable().isVisible(TreeUtil.createPath(node));
  }

  @Override
  protected List<Column> getDefaultColumns() {
    return DefaultColumn.getColumnStubs();
  }

  @Override
  protected Chart getChart() {
    return myUiFacade.getResourceChart();
  }

  /** Initialize the treetable. Addition of various listeners, tree's icons. */
  @Override
  protected void doInit() {
    super.doInit();
    myResourceTreeModel.updateResources();
    getVerticalScrollBar().addAdjustmentListener(new VscrollAdjustmentListener(myUiFacade.getResourceChart(), false));
  }

  @Override
  protected void onProjectOpened() {
    super.onProjectOpened();
    myResourceTreeModel.updateResources();
  }

  private RoleManager getRoleManager() {
    return myRoleManager;
  }

  @Override
  protected TableColumnExt newTableColumnExt(int modelIndex) {
    TableColumnExt tableColumn = super.newTableColumnExt(modelIndex);
    if (modelIndex == DefaultColumn.ROLE.ordinal() || modelIndex == DefaultColumn.ROLE_IN_TASK.ordinal()) {
      JComboBox comboBox = new JComboBox(getRoleManager().getEnabledRoles());
      comboBox.setEditable(false);
      tableColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }
    return tableColumn;
  }

  /** @return the list of the selected nodes. */
  @Deprecated
  public DefaultMutableTreeTableNode[] getSelectedNodes() {
    TreePath[] currentSelection = getTreeSelectionModel().getSelectionPaths();

    if (currentSelection == null || currentSelection.length == 0) {
      return new DefaultMutableTreeTableNode[0];
    }
    DefaultMutableTreeTableNode[] dmtnselected = new DefaultMutableTreeTableNode[currentSelection.length];

    for (int i = 0; i < currentSelection.length; i++) {
      dmtnselected[i] = (DefaultMutableTreeTableNode) currentSelection[i].getLastPathComponent();
    }
    return dmtnselected;
  }

  public boolean isExpanded(HumanResource hr) {
    ResourceNode node = ((ResourceTreeTableModel) getTreeTableModel()).getNodeForResource(hr);
    if (node != null) {
      return getTreeTable().isExpanded(TreeUtil.createPath(node));
    }
    return false;
  }

  public void setAction(Action action) {
    InputMap inputMap = new InputMap();

    inputMap.put((KeyStroke) action.getValue(Action.ACCELERATOR_KEY), action.getValue(Action.NAME));

    inputMap.setParent(getTreeTable().getInputMap(JComponent.WHEN_FOCUSED));
    getTreeTable().setInputMap(JComponent.WHEN_FOCUSED, inputMap);

    // Add the action to the component
    getTreeTable().getActionMap().put(action.getValue(Action.NAME), action);
  }

  public boolean canMoveSelectionUp() {
    final DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes.length != 1) {
      return false;
    }
    DefaultMutableTreeTableNode selectedNode = selectedNodes[0];
    TreeNode previousSibling = TreeUtil.getPrevSibling(selectedNode);
    if (previousSibling == null) {
      return false;
    }
    return true;
  }

  /** Move selected resource up */
  public void upResource() {
    final DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes.length != 1) {
      return;
    }
    DefaultMutableTreeTableNode selectedNode = selectedNodes[0];
    TreeNode previousSibling = TreeUtil.getPrevSibling(selectedNode);
    if (previousSibling == null) {
      return;
    }
    if (selectedNode instanceof ResourceNode) {
      HumanResource people = (HumanResource) selectedNode.getUserObject();
      myResourceTreeModel.moveUp(people);
      getTreeSelectionModel().setSelectionPath(TreeUtil.createPath(selectedNode));
    } else if (selectedNode instanceof AssignmentNode) {
      swapAssignents((AssignmentNode) selectedNode, (AssignmentNode) previousSibling);
    }
  }

  public boolean canMoveSelectionDown() {
    final DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes.length != 1) {
      return false;
    }
    DefaultMutableTreeTableNode selectedNode = selectedNodes[0];
    TreeNode nextSibling = TreeUtil.getNextSibling(selectedNode);
    if (nextSibling == null) {
      return false;
    }
    return true;
  }

  /** Move the selected resource down */
  public void downResource() {
    final DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes.length == 0) {
      return;
    }
    DefaultMutableTreeTableNode selectedNode = selectedNodes[0];
    TreeNode nextSibling = TreeUtil.getNextSibling(selectedNode);
    if (nextSibling == null) {
      return;
    }
    if (selectedNode instanceof ResourceNode) {
      HumanResource people = (HumanResource) selectedNode.getUserObject();
      myResourceTreeModel.moveDown(people);
      getTreeSelectionModel().setSelectionPath(TreeUtil.createPath(selectedNode));
    } else if (selectedNode instanceof AssignmentNode) {
      swapAssignents((AssignmentNode) selectedNode, (AssignmentNode) nextSibling);
    }
  }

  void swapAssignents(AssignmentNode selected, AssignmentNode sibling) {
    ResourceAssignment selectedAssignment = selected.getAssignment();
    ResourceAssignment previousAssignment = sibling.getAssignment();
    selectedAssignment.getResource().swapAssignments(selectedAssignment, previousAssignment);
  }
}
