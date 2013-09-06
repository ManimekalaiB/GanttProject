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
package net.sourceforge.ganttproject.gui;

/**
 * This class is from jedit.org
 * RolloverButton.java - Class for buttons that implement rollovers
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * Special button for tests on TaskPropertiesBeans
 */
public class TestGanttRolloverButton extends JButton {

  private int myAutoRepeatMilliseconds;

  private boolean myTextHidden = false;

  public TestGanttRolloverButton() {
    setBorderPainted(false);
    setMargin(new Insets(0, 0, 0, 0));

    addMouseListener(new MouseOverHandler());
    addMouseListener(new AutoRepeatHandler());
    setHorizontalTextPosition(SwingConstants.CENTER);
    setVerticalTextPosition(SwingConstants.BOTTOM);
    setRolloverEnabled(true);
  }

  public TestGanttRolloverButton(Action action) {
    this();
    setAction(action);
    Icon smallIcon = (Icon) action.getValue(Action.SMALL_ICON);
    if (smallIcon != null) {
      setIcon(smallIcon);
      setTextHidden(true);
    }
  }

  public TestGanttRolloverButton(Icon icon) {
    this();
    setIcon(icon);
  }

  public void setAutoRepeatMousePressedEvent(int milliseconds) {
    myAutoRepeatMilliseconds = milliseconds;
  }

  public void setIconHidden(boolean isHidden) {
  }

  public void setTextHidden(boolean isHidden) {
    myTextHidden = isHidden;
    if (isHidden) {
      setText("");
    } else {
      Action action = getAction();
      if (action != null) {
        setText(String.valueOf(action.getValue(Action.NAME)));
      }
    }
  };

  @Override
  public void setIcon(Icon icon) {
    if (icon != null) {
      setRolloverIcon(icon);
    }
    super.setIcon(icon);
  }

  @Override
  public void setText(String text) {
    // Only set/update text if no icon is present
    if (myTextHidden) {
      super.setText("");
    } else {
      super.setText(text);
    }
  }

  public void setDefaultIcon(Icon iconOn) {
    setIcon(iconOn);
  }

  class MouseOverHandler extends MouseAdapter {
    @Override
    public void mouseEntered(MouseEvent e) {
      if (isEnabled()) {
        setBorderPainted(true);
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      setBorderPainted(false);
    }
  }

  class AutoRepeatHandler extends MouseAdapter {
    private Worker myWorker;

    @Override
    public void mousePressed(MouseEvent e) {
      if (myAutoRepeatMilliseconds > 0) {
        myWorker = new Worker(e);
        myWorker.start();
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (myWorker != null) {
        myWorker.interrupt();
        myWorker = null;
      }
    }
  }

  class Worker extends Thread {
    private ActionEvent myEvent;

    Worker(MouseEvent e) {
      myEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(),
          EventQueue.getMostRecentEventTime(), e.getModifiers());
    }

    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(myAutoRepeatMilliseconds);
        } catch (InterruptedException e) {
          break;
        }
        if (isInterrupted()) {
          break;
        }
        getAction().actionPerformed(myEvent);
      }
    }
  }
}
