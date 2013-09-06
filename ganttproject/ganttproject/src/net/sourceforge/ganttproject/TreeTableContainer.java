/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.TreeUiFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.util.collect.Pair;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import biz.ganttproject.core.table.ColumnList;

/**
 *
 * @author dbarashev (Dmitry Barashev)
 *
 * @param <ModelObject>
 * @param <TreeTableClass>
 * @param <TreeTableModelClass>
 */
public abstract class TreeTableContainer<ModelObject, TreeTableClass extends GPTreeTableBase, TreeTableModelClass extends DefaultTreeTableModel>
    extends JPanel implements TreeUiFacade<ModelObject> {
  private final TreeTableClass myTreeTable;
  private final TreeTableModelClass myTreeTableModel;
  private GPAction myNewAction;
  private GPAction myPropertiesAction;
  private GPAction myDeleteAction;

  private class ExpandCollapseAction extends GPAction {
    ExpandCollapseAction() {
      super("tree.expand");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      TreePath currentSelection = getTree().getTreeSelectionModel().getSelectionPath();
      if (currentSelection != null) {
        if (getTree().isCollapsed(currentSelection)) {
          getTree().expandPath(currentSelection);
        } else {
          getTree().collapsePath(currentSelection);
        }
      }
    }
  }

  private class ExpandAllAction extends GPAction {
    ExpandAllAction() {
      super("tree.expandAll");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      TreePath currentSelection = getTree().getTreeSelectionModel().getSelectionPath();
      if (currentSelection != null) {
        expandAll(currentSelection);
      }
    }
  }

  private class CollapseAllAction extends GPAction {
    CollapseAllAction() {
      super("tree.collapseAll");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      TreePath currentSelection = getTree().getTreeSelectionModel().getSelectionPath();
      if (currentSelection != null) {
        collapseAll(currentSelection);
      }
    }
  }
  public TreeTableContainer(Pair<TreeTableClass, TreeTableModelClass> tableAndModel) {
    super(new BorderLayout());
    myTreeTableModel = tableAndModel.second();
    myTreeTable = tableAndModel.first();
    myTreeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    getTreeTable().setBackground(new Color(1.0f, 1.0f, 1.0f));

    myTreeTable.getTree().getTreeTableModel().addTreeModelListener(new ChartUpdater());
    GPAction[] nodeActions = new GPAction[] {new ExpandCollapseAction(), new ExpandAllAction(), new CollapseAllAction()};
    for (GPAction nodeAction : nodeActions) {
      for (KeyStroke ks : GPAction.getAllKeyStrokes(nodeAction.getID())) {
        UIUtil.pushAction(myTreeTable, false, ks, nodeAction);
      }
    }
    ExpandAllAction expandAll = new ExpandAllAction();

    this.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            getTreeTable().getTable().requestFocusInWindow();
          }
        });
      }
    });
    MouseListener ml = new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent e) {
        handlePopupTrigger(e);
      }

      @Override
      public void mousePressed(MouseEvent e) {
        handlePopupTrigger(e);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
          if (getTreeTable().getTable().getSelectedRow() != -1) {
            e.consume();
            getPropertiesAction().actionPerformed(null);
          }
          return;
        }
        handlePopupTrigger(e);
      }

    };
    getTreeTable().addMouseListener(ml);
    getTree().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        onSelectionChanged(Arrays.asList(getSelectedNodes()));
      }
    });
  }

  public void expandAll(TreePath root) {
    getTree().expandPath(root);
    TreeTableNode node = (TreeTableNode) root.getLastPathComponent();
    for (int i = 0; i < node.getChildCount(); i++) {
      expandAll(root.pathByAddingChild(node.getChildAt(i)));
    }
  }

  private void collapseAll(TreePath root) {
    TreeTableNode node = (TreeTableNode) root.getLastPathComponent();
    for (int i = 0; i < node.getChildCount(); i++) {
      collapseAll(root.pathByAddingChild(node.getChildAt(i)));
    }
    getTree().collapsePath(root);
  }

  protected abstract void init();

  protected void onSelectionChanged(List<DefaultMutableTreeTableNode> selection) {
  }

  protected abstract void handlePopupTrigger(MouseEvent e);

  protected JXTreeTable getTree() {
    return getTreeTable().getTree();
  }

  protected TreeTableClass getTreeTable() {
    return myTreeTable;
  }

  protected TreeTableModelClass getTreeModel() {
    return myTreeTableModel;
  }

  @Override
  public Component getTreeComponent() {
    return this;
  }

  @Override
  public ColumnList getVisibleFields() {
    return myTreeTable.getVisibleFields();
  }

  @Override
  public boolean isExpanded(ModelObject modelObject) {
    MutableTreeTableNode treeNode = getNode(modelObject);
    return treeNode == null ? false : !myTreeTable.getTree().isCollapsed(TreeUtil.createPath(treeNode));
  }

  @Override
  public void setExpanded(ModelObject modelObject) {
    MutableTreeTableNode treeNode = getNode(modelObject);
    if (treeNode != null) {
      myTreeTable.getTree().expandPath(TreeUtil.createPath(treeNode));
    }
  }

  @Override
  public boolean isVisible(ModelObject modelObject) {
    MutableTreeTableNode node = getNode(modelObject);
    if (node == null) {
      return false;
    }
    return getTreeTable().getTree().isVisible(TreeUtil.createPath(node));
  }

  public void commitIfEditing() {
    if (myTreeTable.getTable().isEditing()) {
      myTreeTable.getTable().getCellEditor().stopCellEditing();
    }
  }

  public int getRowHeight() {
    return myTreeTable.getTable().getRowHeight();
  }

  protected MutableTreeTableNode getNode(ModelObject modelObject) {
    for (MutableTreeTableNode nextNode : TreeUtil.collectSubtree(getRootNode())) {
      if (nextNode.getUserObject() != null && nextNode.getUserObject().equals(modelObject)) {
        return nextNode;
      }
    }
    return null;
  }

  protected DefaultMutableTreeTableNode getSelectedNode() {
    TreePath currentSelection = getTree().getTreeSelectionModel().getSelectionPath();
    return (currentSelection == null) ? null : (DefaultMutableTreeTableNode) currentSelection.getLastPathComponent();
  }

  public DefaultMutableTreeTableNode[] getSelectedNodes() {
    TreePath[] currentSelection = getTree().getTreeSelectionModel().getSelectionPaths();

    if (currentSelection == null || currentSelection.length == 0) {
      return new DefaultMutableTreeTableNode[0];
    }
    DefaultMutableTreeTableNode[] result = new DefaultMutableTreeTableNode[currentSelection.length];
    for (int i = 0; i < currentSelection.length; i++) {
      result[i] = (DefaultMutableTreeTableNode) currentSelection[i].getLastPathComponent();
    }
    return result;
  }

  protected abstract DefaultMutableTreeTableNode getRootNode();

  protected abstract Chart getChart();

  private class ChartUpdater implements TreeModelListener {
    @Override
    public void treeNodesChanged(TreeModelEvent e) {
      getChart().reset();
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
      getChart().reset();
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
      getChart().reset();
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
      getChart().reset();
    }
  }

  @Override
  public GPAction getNewAction() {
    return myNewAction;
  }

  @Override
  public GPAction getPropertiesAction() {
    return myPropertiesAction;
  }

  @Override
  public GPAction getDeleteAction() {
    return myDeleteAction;
  }

  protected void setArtefactActions(GPAction newAction, GPAction propertiesAction, GPAction deleteAction) {
    myNewAction = newAction;
    myPropertiesAction = propertiesAction;
    myDeleteAction = deleteAction;
  }
}
