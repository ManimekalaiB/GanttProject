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
package biz.ganttproject.core.option;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public abstract class GPAbstractOption<T> implements GPOption<T> {
  public abstract static class I18N {
    private static I18N ourInstance;

    protected static void setI18N(I18N i18n) {
      ourInstance = i18n;
    }
    
    protected abstract String i18n(String key); 
  }
  
  private final String myID;

  private final List<ChangeValueListener> myListeners = new ArrayList<ChangeValueListener>();

  private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

  private boolean isWritable = true;

  private T myValue;
  private T myInitialValue;

  private boolean isScreened;

  private boolean myHasUi = true;

  protected GPAbstractOption(String id) {
    this(id, null);
  }

  protected GPAbstractOption(String id, T initialValue) {
    myID = id;
    myInitialValue = initialValue;
    myValue = initialValue;
  }

  @Override
  public String getID() {
    return myID;
  }

  @Override
  public T getValue() {
    return myValue;
  }

  @Override
  public void setValue(T value) {
    setValue(value, false);
  }

  protected T getInitialValue() {
    return myInitialValue;
  }

  protected void setValue(T value, boolean resetInitial) {
    if (resetInitial) {
      myInitialValue = value;
    }
    ChangeValueEvent event = new ChangeValueEvent(getID(), myValue, value);
    myValue = value;
    fireChangeValueEvent(event);
  }

  @Override
  public boolean isChanged() {
    if (myInitialValue == null) {
      return myValue != null;
    }
    return !myInitialValue.equals(myValue);
  }

  @Override
  public void lock() {
  }

  @Override
  public void commit() {
  }

  @Override
  public void rollback() {
  }

  @Override
  public Runnable addChangeValueListener(final ChangeValueListener listener) {
    myListeners.add(listener);
    return new Runnable() {
      @Override
      public void run() {
        myListeners.remove(listener);
      }
    };
  }

  protected void fireChangeValueEvent(ChangeValueEvent event) {
    for (ChangeValueListener listener : myListeners) {
      listener.changeValue(event);
    }
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    myPropertyChangeSupport.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    myPropertyChangeSupport.removePropertyChangeListener(listener);
  }

  @Override
  public boolean isWritable() {
    return isWritable;
  }

  public void setWritable(boolean isWritable) {
    this.isWritable = isWritable;
    myPropertyChangeSupport.firePropertyChange("isWritable", Boolean.valueOf(!isWritable), Boolean.valueOf(isWritable));
  }

  @Override
  public boolean isScreened() {
    return isScreened;
  }

  @Override
  public void setScreened(boolean value) {
    isScreened = value;
  }

  public boolean hasUi() {
    return myHasUi;
  }
  
  public void setHasUi(boolean hasUi) {
    myHasUi = hasUi;
  }

  protected PropertyChangeSupport getPropertyChangeSupport() {
    return myPropertyChangeSupport;
  }

  protected static String i18n(String key) {
    return I18N.ourInstance.i18n(key);
  }
}