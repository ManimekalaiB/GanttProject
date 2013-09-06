/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject Team

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
package net.sourceforge.ganttproject.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.PrjInfos;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.parser.FileFormatException;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParsingContext;
import net.sourceforge.ganttproject.parser.ParsingListener;
import net.sourceforge.ganttproject.parser.TagHandler;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import biz.ganttproject.core.time.GanttCalendar;

/**
 * Allows to load a gantt file from xml format, using SAX parser
 */
public class GanttXMLOpen implements GPParser {
  /** 0-->description of project, 1->note for task */
  int typeChar = -1;

  private String indent = "";

  private final ArrayList<TagHandler> myTagHandlers = new ArrayList<TagHandler>();

  private final ArrayList<ParsingListener> myListeners = new ArrayList<ParsingListener>();

  private final ParsingContext myContext;

  private final TaskManager myTaskManager;

  private int viewIndex;

  private int ganttDividerLocation;

  private int resourceDividerLocation;

  private PrjInfos myProjectInfo = null;

  private UIFacade myUIFacade = null;

  private UIConfiguration myUIConfig;

  public GanttXMLOpen(PrjInfos info, UIConfiguration uiConfig, TaskManager taskManager, UIFacade uiFacade) {
    this(taskManager);
    myProjectInfo = info;
    myUIConfig = uiConfig;
    this.viewIndex = 0;

    this.ganttDividerLocation = 300; // TODO is this arbitrary value right ?
    this.resourceDividerLocation = 300;
    myUIFacade = uiFacade;
  }

  public GanttXMLOpen(TaskManager taskManager) {
    myContext = new ParsingContext();
    myTaskManager = taskManager;
  }

  @Override
  public boolean load(InputStream inStream) throws IOException {
    try {
      myTaskManager.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().setEnabled(false);
      myTaskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);
      myTaskManager.getAlgorithmCollection().getScheduler().setEnabled(false);
      return doLoad(inStream);
    } finally {
      myTaskManager.getAlgorithmCollection().getScheduler().setEnabled(true);
      myTaskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(true);
      myTaskManager.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().setEnabled(true);
    }
  }

  public boolean doLoad(InputStream inStream) throws IOException {
    // Use an instance of ourselves as the SAX event handler
    DefaultHandler handler = new GanttXMLParser();

    // Use the default (non-validating) parser
    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      // Parse the input
      SAXParser saxParser;
      saxParser = factory.newSAXParser();
      saxParser.parse(inStream, handler);
    } catch (ParserConfigurationException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
      throw new IOException(e.getMessage());
    } catch (SAXException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
      throw new IOException(e.getMessage());
    } catch (RuntimeException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
      throw new IOException(e.getMessage());
    }
    myUIFacade.setViewIndex(viewIndex);
    myUIFacade.setGanttDividerLocation(ganttDividerLocation);
    if (resourceDividerLocation != 0) {
      myUIFacade.setResourceDividerLocation(resourceDividerLocation);
    }
    return true;

  }

  public boolean load(File file) {

    // Use an instance of ourselves as the SAX event handler
    DefaultHandler handler = new GanttXMLParser();

    // Use the default (non-validating) parser
    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      // Parse the input
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(file, handler);
    } catch (Exception e) {
      myUIFacade.showErrorDialog(e);
      return false;
    }
    return true;
  }

  @Override
  public void addTagHandler(TagHandler handler) {
    myTagHandlers.add(handler);
  }

  @Override
  public void addParsingListener(ParsingListener listener) {
    myListeners.add(listener);
  }

  @Override
  public ParsingContext getContext() {
    return myContext;
  }

  @Override
  public TagHandler getDefaultTagHandler() {
    return new DefaultTagHandler();
  }

  private class DefaultTagHandler implements TagHandler {
    @Override
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) {
      indent += "    ";
      String eName = sName; // element name
      if ("".equals(eName)) {
        eName = qName; // not namespace aware
      }
      if (eName.equals("description")) {
        myCharacterBuffer = new StringBuffer();
        typeChar = 0;
      }
      if (eName.equals("notes")) {
        myCharacterBuffer = new StringBuffer();
        typeChar = 1;
        // barmeier: we know that this tag has only attributes no nested
        // tags
        // we can do we need here.
      }
      if (eName.equals("tasks")) {
        myTaskManager.setZeroMilestones(null);
      }
      if (attrs != null) {
        for (int i = 0; i < attrs.getLength(); i++) {
          String aName = attrs.getLocalName(i); // Attr name
          if ("".equals(aName)) {
            aName = attrs.getQName(i);
            // The project part
          }
          if (eName.equals("project")) {
            if (aName.equals("name")) {
              myProjectInfo.setName(attrs.getValue(i));
            } else if (aName.equals("company")) {
              myProjectInfo.setOrganization(attrs.getValue(i));
            } else if (aName.equals("webLink")) {
              myProjectInfo.setWebLink(attrs.getValue(i));
            }
            // TODO: 1.12 repair scrolling to the saved date
            else if (aName.equals("view-date")) {
              myUIFacade.getScrollingManager().scrollTo(GanttCalendar.parseXMLDate(attrs.getValue(i)).getTime());
            } else if (aName.equals("view-index")) {
              viewIndex = new Integer(attrs.getValue(i)).hashCode();
            } else if (aName.equals("gantt-divider-location")) {
              ganttDividerLocation = new Integer(attrs.getValue(i)).intValue();
            } else if (aName.equals("resource-divider-location")) {
              resourceDividerLocation = new Integer(attrs.getValue(i)).intValue();
            }
          } else if (eName.equals("tasks")) {
            if ("empty-milestones".equals(aName)) {
              myTaskManager.setZeroMilestones(Boolean.parseBoolean(attrs.getValue(i)));
            }
          }
        }
      }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) {
      indent = indent.substring(0, indent.length() - 4);
      if ("description".equals(qName)) {
        myProjectInfo.setDescription(myCharacterBuffer.toString());
      } else if ("notes".equals(qName)) {
        Task currentTask = getContext().peekTask();
        currentTask.setNotes(myCharacterBuffer.toString());
      }
    }

  }

  private StringBuffer myCharacterBuffer = new StringBuffer();

  class GanttXMLParser extends DefaultHandler {
    private final Stack<String> myTagStack = new Stack<String>();

    @Override
    public void startDocument() throws SAXException {
      super.startDocument();
      myTagStack.clear();
    }

    @Override
    public void endDocument() throws SAXException {
      for (ParsingListener l : myListeners) {
        l.parsingFinished();
      }
    }

    @Override
    public void startElement(String namespaceURI, String sName, // simple
        // name
        String qName, // qualified name
        Attributes attrs) throws SAXException {
      myTagStack.push(qName);
      for (TagHandler next : myTagHandlers) {
        try {
          next.startElement(namespaceURI, sName, qName, attrs);
        } catch (FileFormatException e) {
          System.err.println(e.getMessage());
        }
      }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
      for (TagHandler next : myTagHandlers) {
        next.endElement(namespaceURI, sName, qName);
      }
      myTagStack.pop();
    }

    @Override
    public void characters(char buf[], int offset, int len) throws SAXException {
      String s = new String(buf, offset, len);
      if (typeChar >= 0) {
        myCharacterBuffer.append(s);
      }
    }
  }
}
