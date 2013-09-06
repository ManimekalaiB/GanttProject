/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

public class ResourceTreeTableModel extends DefaultTreeTableModel {
  public static final int INDEX_RESOURCE_NAME = 0;

  public static final int INDEX_RESOURCE_ROLE = 1;

  public static final int INDEX_RESOURCE_EMAIL = 2;

  public static final int INDEX_RESOURCE_PHONE = 3;

  public static final int INDEX_RESOURCE_ROLE_TASK = 4;

  /** all the columns */
  // private final Map<Integer, ResourceColumn> columns = new
  // LinkedHashMap<Integer, ResourceColumn>();

  /** Column indexer */
  private static int index = -1;

  private DefaultMutableTreeTableNode root = null;

  private final HumanResourceManager myResourceManager;

  private final TaskManager myTaskManager;

  private TreeSelectionModel mySelectionModel;

  private final CustomPropertyManager myCustomPropertyManager;

  private String[] myDefaultColumnTitles;

  public ResourceTreeTableModel(HumanResourceManager resMgr, TaskManager taskManager,
      CustomPropertyManager customPropertyManager) {
    super();
    myCustomPropertyManager = customPropertyManager;
    myResourceManager = resMgr;
    myTaskManager = taskManager;
    myTaskManager.addTaskListener(new TaskListenerAdapter() {
      @Override
      public void taskRemoved(TaskHierarchyEvent e) {
        fireResourceChange(e.getTask());
      }

      @Override
      public void taskScheduleChanged(TaskScheduleEvent e) {
        fireResourceChange(e.getTask());
      }

      void fireResourceChange(Task task) {
        ResourceAssignment[] assignments = task.getAssignments();
        for (int i = 0; i < assignments.length; i++) {
          assignments[i].getResource().resetLoads();
          resourceAssignmentsChanged(new HumanResource[] { assignments[i].getResource() });
        }
      }
    });
    root = buildTree();
    this.setRoot(root);
    changeLanguage();
    GanttLanguage.getInstance().addListener(new GanttLanguage.Listener() {
      @Override
      public void languageChanged(Event event) {
        changeLanguage();
      }
    });
  }

  public int useNextIndex() {
    index++;
    return index;
  }

  public MutableTreeTableNode getNodeForAssigment(ResourceAssignment assignement) {
    for (MutableTreeTableNode an : ImmutableList.copyOf(Iterators.forEnumeration(getNodeForResource(
        assignement.getResource()).children()))) {
      if (assignement.equals(an.getUserObject())) {
        return an;
      }
    }
    return null;
  }

  private DefaultMutableTreeTableNode buildTree() {

    DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode();
    List<HumanResource> listResources = myResourceManager.getResources();
    Iterator<HumanResource> itRes = listResources.iterator();

    while (itRes.hasNext()) {
      HumanResource hr = itRes.next();
      ResourceNode rnRes = new ResourceNode(hr); // the first for the resource
      root.add(rnRes);
    }
    return root;
  }

  public void updateResources() {
    HumanResource[] listResources = myResourceManager.getResourcesArray();

    for (int idxResource = 0; idxResource < listResources.length; idxResource++) {
      HumanResource hr = listResources[idxResource];

      ResourceNode rnRes = getNodeForResource(hr);
      if (rnRes == null) {
        rnRes = new ResourceNode(hr);
      }
      buildAssignmentsSubtree(rnRes);
      // for (int i = 0; i < tra.length; i++) {
      // AssignmentNode an = exists(rnRes, tra[i]);
      // if (an == null) {
      // an = new AssignmentNode(tra[i]);
      // rnRes.add(an);
      // }
      // }
      if (getNodeForResource(hr) == null) {
        root.add(rnRes);
      }
    }
    // this.setRoot(root);

  }

  public ResourceNode getNodeForResource(final HumanResource hr) {
    try {
      return (ResourceNode) Iterators.find(Iterators.forEnumeration(root.children()),
          new Predicate<MutableTreeTableNode>() {
            @Override
            public boolean apply(MutableTreeTableNode input) {
              return input.getUserObject().equals(hr);
            }
          });
    } catch (NoSuchElementException e) {
      return null;
    }
  }

  /**
   * Changes the language.
   *
   * @param ganttLanguage
   *          New language to use.
   */
  public void changeLanguage() {
    GanttLanguage language = GanttLanguage.getInstance();
    myDefaultColumnTitles = new String[] { language.getText("tableColResourceName"),
        language.getText("tableColResourceRole"), language.getText("tableColResourceEMail"),
        language.getText("tableColResourcePhone"), language.getText("tableColResourceRoleForTask") };
  }

  public void changePeople(List<HumanResource> people) {
    Iterator<HumanResource> it = people.iterator();
    while (it.hasNext()) {
      addResource(it.next());
    }
  }

  public DefaultMutableTreeTableNode addResource(HumanResource people) {
    DefaultMutableTreeTableNode result = new ResourceNode(people);
    insertNodeInto(result, root, root.getChildCount());
    myResourceManager.toString();
    return result;
  }

  public void deleteResources(HumanResource[] peoples) {
    for (int i = 0; i < peoples.length; i++) {
      deleteResource(peoples[i]);
    }
  }

  public void deleteResource(HumanResource people) {
    removeNodeFromParent(getNodeForResource(people));
    // myResourceManager.remove(people);
  }

  /** Move Up the selected resource */
  public boolean moveUp(HumanResource resource) {
    myResourceManager.up(resource);
    ResourceNode rn = getNodeForResource(resource);
    int index = TreeUtil.getPrevSibling(root, rn);
    if (index == -1) {
      return false;
    }
    removeNodeFromParent(rn);
    insertNodeInto(rn, root, index);
    return true;
  }

  public boolean moveDown(HumanResource resource) {
    myResourceManager.down(resource);
    ResourceNode rn = getNodeForResource(resource);
    int index = TreeUtil.getNextSibling(root, rn);
    if (index == -1) {
      return false;
    }
    removeNodeFromParent(rn);
    insertNodeInto(rn, root, index);
    return true;
  }

  public void reset() {
    myResourceManager.clear();
  }

  public List<HumanResource> getAllResouces() {
    return myResourceManager.getResources();
  }

  @Override
  public int getColumnCount() {
    return myDefaultColumnTitles.length + myCustomPropertyManager.getDefinitions().size();
  }

  // public ArrayList<ResourceColumn> getColumns()
  // {
  // return new ArrayList<ResourceColumn>(columns.values());
  // }
  //
  // /** @return the ResourceColumn associated to the given index */
  // public ResourceColumn getColumn(int index) {
  // return columns.get(new Integer(index));
  // }

  private CustomPropertyDefinition getCustomProperty(int columnIndex) {
    return myCustomPropertyManager.getDefinitions().get(columnIndex - myDefaultColumnTitles.length);
  }

  @Override
  public Class<?> getColumnClass(int colIndex) {
    if (colIndex == 0) {
      return TreeNode.class;
    }
    if (colIndex < myDefaultColumnTitles.length) {
      return String.class;
    }
    return getCustomProperty(colIndex).getType();
  }

  @Override
  public String getColumnName(int column) {
    if (column < myDefaultColumnTitles.length) {
      return myDefaultColumnTitles[column];
    }
    CustomPropertyDefinition customColumn = getCustomProperty(column);
    return customColumn.getName();
  }

  @Override
  public boolean isCellEditable(Object node, int column) {
    return (node instanceof ResourceNode && (column == INDEX_RESOURCE_EMAIL || column == INDEX_RESOURCE_NAME
        || column == INDEX_RESOURCE_PHONE || column == INDEX_RESOURCE_ROLE))
        || (node instanceof AssignmentNode && (column == INDEX_RESOURCE_ROLE_TASK)
        /* assumes the INDEX_RESOURCE_ROLE_TASK is the last mandatory column */
        || column > INDEX_RESOURCE_ROLE_TASK);
  }

  @Override
  public Object getValueAt(Object node, int column) {
    Object res = null;
    ResourceNode rn = null;
    AssignmentNode an = null;

    if (node instanceof ResourceNode) {
      rn = (ResourceNode) node;
    } else if (node instanceof AssignmentNode) {
      an = (AssignmentNode) node;
    } else {
      return "";
    }

    boolean hasChild = rn != null;

    switch (column) {
    case 0: // name
      if (hasChild) {
        res = rn.getName();
      } else {
        res = an.getTask().getName();
      }
      break;
    case 1: // def role
      if (hasChild) {
        res = rn.getDefaultRole();
      } else {
        res = "";
      }
      break;
    case 2: // mail
      if (hasChild) {
        res = rn.getEMail();
      } else {
        res = "";
      }
      break;
    case 3: // phone
      if (hasChild) {
        res = rn.getPhone();
      } else {
        res = "";
      }
      break;
    case 4: // assign role
      if (hasChild) {
        res = "";
      } else {
        res = an.getRoleForAssigment();
      }
      break;
    default: // custom column
      if (hasChild) {
        res = rn.getCustomField(getCustomProperty(column));
      } else
        res = "";
      break;
    }
    return res;
  }

  @Override
  public void setValueAt(Object value, Object node, int column) {
    if (isCellEditable(node, column))
      switch (column) {
      case INDEX_RESOURCE_NAME:
        ((ResourceNode) node).setName(value.toString());
        break;
      case INDEX_RESOURCE_EMAIL:
        ((ResourceNode) node).setEMail(value.toString());
        break;
      case INDEX_RESOURCE_PHONE:
        ((ResourceNode) node).setPhone(value.toString());
        break;
      case INDEX_RESOURCE_ROLE:
        ((ResourceNode) node).setDefaultRole((Role) value);
        break;
      case INDEX_RESOURCE_ROLE_TASK:
        ((AssignmentNode) node).setRoleForAssigment((Role) value);
        break;
      default:
        ((ResourceNode) node).setCustomField(getCustomProperty(column), value);
        break;
      }
  }


  public void resourceChanged(HumanResource resource) {
    ResourceNode node = getNodeForResource(resource);
    if (node == null) {
      return;
    }
    modelSupport.firePathChanged(TreeUtil.createPath(node));
  }

  public void resourceAssignmentsChanged(HumanResource[] resources) {
    for (int i = 0; i < resources.length; i++) {
      ResourceNode nextNode = getNodeForResource(resources[i]);
      SelectionKeeper selectionKeeper = new SelectionKeeper(mySelectionModel, nextNode);
      buildAssignmentsSubtree(nextNode);
      selectionKeeper.restoreSelection();
    }
  }

  private void buildAssignmentsSubtree(ResourceNode resourceNode) {
    HumanResource resource = resourceNode.getResource();
    resourceNode.removeAllChildren();
    ResourceAssignment[] assignments = resource.getAssignments();
    int[] indices = new int[assignments.length];
    TreeNode[] children = new TreeNode[assignments.length];
    if (assignments.length > 0) {
      for (int i = 0; i < assignments.length; i++) {
        indices[i] = i;
        AssignmentNode an = new AssignmentNode(assignments[i]);
        children[i] = an;
        resourceNode.add(an);
      }
    }
    modelSupport.fireTreeStructureChanged(TreeUtil.createPath(resourceNode));
  }

  void decreaseCustomPropertyIndex(int i) {
    index -= i;
  }

  void setSelectionModel(TreeSelectionModel selectionModel) {
    mySelectionModel = selectionModel;
  }

  private class SelectionKeeper {
    private final DefaultMutableTreeTableNode myChangingSubtreeRoot;
    private final TreeSelectionModel mySelectionModel;
    private boolean hasWork = false;
    private Object myModelObject;

    SelectionKeeper(TreeSelectionModel selectionModel, DefaultMutableTreeTableNode changingSubtreeRoot) {
      mySelectionModel = selectionModel;
      myChangingSubtreeRoot = changingSubtreeRoot;
      TreePath selectionPath = mySelectionModel.getSelectionPath();
      if (selectionPath != null && TreeUtil.createPath(myChangingSubtreeRoot).isDescendant(selectionPath)) {
        hasWork = true;
        DefaultMutableTreeTableNode lastNode = (DefaultMutableTreeTableNode) selectionPath.getLastPathComponent();
        myModelObject = lastNode.getUserObject();
      }
    }

    void restoreSelection() {
      if (!hasWork) {
        return;
      }
      for (MutableTreeTableNode node : TreeUtil.collectSubtree(myChangingSubtreeRoot)) {
        if (node.getUserObject().equals(myModelObject)) {
          mySelectionModel.setSelectionPath(TreeUtil.createPath(node));
          break;
        }
      }
    }
  }
}
