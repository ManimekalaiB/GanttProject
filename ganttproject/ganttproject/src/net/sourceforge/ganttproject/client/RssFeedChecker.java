/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import biz.ganttproject.core.option.DateOption;
import biz.ganttproject.core.option.DefaultDateOption;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.NotificationItem;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Checks GanttProject RSS news feeds once per day
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class RssFeedChecker {
  private static enum CheckOption {
    YES, NO, UNDEFINED
  }

  private final UIFacade myUiFacade;
  private final DefaultEnumerationOption<CheckOption> myCheckRssOption = new DefaultEnumerationOption<CheckOption>(
      "check", CheckOption.values()) {
    @Override
    protected String objectToString(CheckOption obj) {
      return obj.toString();
    }

    @Override
    protected CheckOption stringToObject(String value) {
      return CheckOption.valueOf(value);
    }
  };
  private final DateOption myLastCheckOption = new DefaultDateOption("lastCheck", null);
  private final GPOptionGroup myOptionGroup = new GPOptionGroup("updateRss", new GPOption[] { myCheckRssOption,
      myLastCheckOption });
  private GPTimeUnitStack myTimeUnitStack;
  private static final String RSS_URL = "http://www.ganttproject.biz/my/feed";
  protected static final int MAX_ATTEMPTS = 10;
  private final RssParser parser = new RssParser();
  private final NotificationItem myRssProposalNotification = new NotificationItem("",
      GanttLanguage.getInstance().formatText("updateRss.question",
          GanttLanguage.getInstance().getText("updateRss.question.learnUrl")), new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
          if (e.getEventType() != EventType.ACTIVATED) {
            return;
          }
          if ("yes".equals(e.getURL().getHost())) {
            onYes();
            getNotificationManager().hideNotification();
          } else if ("no".equals(e.getURL().getHost())) {
            onNo();
            getNotificationManager().hideNotification();
          } else {
            NotificationManager.DEFAULT_HYPERLINK_LISTENER.hyperlinkUpdate(e);
            getNotificationManager().hideNotification();
          }
        }
      });

  public RssFeedChecker(GPTimeUnitStack timeUnitStack, UIFacade uiFacade) {
    myCheckRssOption.setValue(CheckOption.UNDEFINED.toString());
    myUiFacade = uiFacade;
    myTimeUnitStack = timeUnitStack;
  }

  private NotificationManager getNotificationManager() {
    return myUiFacade.getNotificationManager();
  }

  public GPOptionGroup getOptions() {
    return myOptionGroup;
  }

  public void run() {
    Runnable command = null;
    CheckOption checkOption = CheckOption.valueOf(myCheckRssOption.getValue());
    if (CheckOption.NO == checkOption) {
      NotificationChannel.RSS.setDefaultNotification(myRssProposalNotification);
      return;
    }
    Date lastCheck = myLastCheckOption.getValue();
    if (lastCheck == null) {
      // It is the first time we run, just mark it. We want to suggest
      // subscribing to updates only to
      // those who runs GP at least twice.
      markLastCheck();
    } else if (wasToday(lastCheck)) {
      // It is not the first run of GP but it was last run today and RSS
      // proposal has not been shown yet.
      // Add it to RSS button but don't promote it, wait until tomorrow.
      if (CheckOption.UNDEFINED == checkOption) {
        NotificationChannel.RSS.setDefaultNotification(myRssProposalNotification);
      }
    } else {
      // So it is not the first time and even not the first day we start GP.
      // If no decision about subscribing, let's proactively suggest it,
      // otherwise
      // run check RSS.
      if (CheckOption.UNDEFINED == checkOption) {
        command = createRssProposalCommand();
      } else {
        command = createRssReadCommand();
      }
    }
    if (command == null) {
      return;
    }
    new Thread(command).start();
  }

  private Runnable createRssReadCommand() {
    return new Runnable() {
      @Override
      public void run() {
        HttpClient httpClient = new DefaultHttpClient();
        String url = RSS_URL;
        try {
          for (int i = 0; i < MAX_ATTEMPTS; i++) {
            HttpGet getRssUrl = new HttpGet(url);
            getRssUrl.addHeader("User-Agent", "GanttProject " + GPVersion.CURRENT);
            HttpResponse result = httpClient.execute(getRssUrl);

            switch (result.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
              processResponse(result.getEntity().getContent());
              return;
            }
          }
        } catch (MalformedURLException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          httpClient.getConnectionManager().shutdown();
        }
      }

      private void processResponse(InputStream responseStream) {
        RssFeed feed = parser.parse(responseStream, myLastCheckOption.getValue());
        List<NotificationItem> items = new ArrayList<NotificationItem>();
        for (RssFeed.Item item : feed.getItems()) {
          items.add(new NotificationItem(item.title, item.body, NotificationManager.DEFAULT_HYPERLINK_LISTENER));
        }
        Collections.reverse(items);
        if (!items.isEmpty()) {
          getNotificationManager().addNotifications(NotificationChannel.RSS, items);
        }
        markLastCheck();
      }
    };
  }

  private Runnable createRssProposalCommand() {
    return new Runnable() {
      @Override
      public void run() {
        getNotificationManager().addNotifications(NotificationChannel.RSS,
            Collections.singletonList(myRssProposalNotification));
      }
    };
  }

  private boolean wasToday(Date date) {
    return myTimeUnitStack.createDuration(GPTimeUnitStack.DAY, date, GPTimeUnitStack.DAY.adjustLeft(new Date())).getLength() == 0;
  }

  private void onYes() {
    myCheckRssOption.setValue(CheckOption.YES.toString());
    new Thread(createRssReadCommand()).start();
  }

  private void onNo() {
    myCheckRssOption.setValue(CheckOption.NO.toString());
  }

  private void markLastCheck() {
    myLastCheckOption.setValue(new Date());
  }
}
