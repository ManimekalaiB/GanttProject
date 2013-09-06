/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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

import java.io.IOException;

import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.gui.about.AboutDialog2;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class OSXAdapter extends ApplicationAdapter {
  private static OSXAdapter osxAdapter;
  private static com.apple.eawt.Application theApp;
  private GanttProject myProj;

  private OSXAdapter(GanttProject myProj) {
    this.myProj = myProj;
  }

  /**
   * This method handles the case when a file in the Finder is dropped onto the
   * app, or GanttProject is selected via the open-with menu option. The event
   * argument contains the path of the file in either case.
   */
  @Override
  public void handleOpenFile(ApplicationEvent event) {
    String file;
    Document myDocument;

    if (myProj.getProjectUIFacade().ensureProjectSaved(myProj)) {
      file = event.getFilename();
      myDocument = myProj.getDocumentManager().getDocument(file);
      try {
        myProj.getProjectUIFacade().openProject(myDocument, myProj.getProject());
      } catch (DocumentException e) {
        myProj.getUIFacade().showErrorDialog(e);
      } catch (IOException e) {
        myProj.getUIFacade().showErrorDialog(e);
      }
    }
    event.setHandled(true);
  }

  /** Handle the Mac OSX "about" menu option. */
  @Override
  public void handleAbout(ApplicationEvent event) {
    AboutDialog2 abd = new AboutDialog2(myProj.getUIFacade());
    abd.show();
    // Indicate we've handled this event ourselves
    event.setHandled(true);
  }

  /**
   * Handles the quit menu option (defaults to command-q) the same way choosing
   * Project->Quit does.
   */
  @Override
  public void handleQuit(ApplicationEvent event) {
    myProj.quitApplication();
    /*
     * Not a typo. Must set handled to false else the app will still quit even
     * if we say "cancel" on confirmation.
     */
    event.setHandled(false);
  }

  public static void registerMacOSXApplication(GanttProject myProj) {
    if (theApp == null) {
      theApp = new com.apple.eawt.Application();
    }

    if (osxAdapter == null) {
      osxAdapter = new OSXAdapter(myProj);
    }

    theApp.addApplicationListener(osxAdapter);
  }
}
