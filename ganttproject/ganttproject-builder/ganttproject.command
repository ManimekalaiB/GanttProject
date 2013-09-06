#!/bin/bash

SCRIPT_PATH=$0
GP_HOME=`dirname "$SCRIPT_PATH"`


GP_LOG_DIR="$HOME/.ganttproject.d"
# Check if log dir is present (or create it)
if [ ! -d $GP_LOG_DIR ]; then
  if [ -e  $GP_LOG_DIR ]; then
    echo "file $GP_LOG_DIR exists and is not a directory" >&2
    exit 1
  fi
  if ! mkdir $GP_LOG_DIR ; then
    echo "Could not create $GP_LOG_DIR directory" >&2
    exit 1
  fi
fi

# Create unique name for log file
LOG_FILE="$GP_LOG_DIR/.ganttproject-"$(date +%Y%m%d%H%M%S)".log"
if [ -e "$LOG_FILE" ] && [ ! -w "$LOG_FILE" ]; then
  echo "Log file $LOG_FILE is not writable" >2
  exit 1
fi

# Find usable java executable
if [ -z "$JAVA_HOME" ]; then
  JAVA_COMMAND=$(which java)
  if [ "1" = "$?" ]; then
    echo "No executable java found. Please set JAVA_HOME variable" >&2
    exit 1
  fi
else
  JAVA_COMMAND=$JAVA_HOME/bin/java
fi
if [ ! -x "$JAVA_COMMAND" ]; then
  echo "$JAVA_COMMAND is not executable. Please check the permissions." >&2
  exit 1
fi

LOCAL_CLASSPATH=${GP_HOME}/eclipsito.jar:${GP_HOME}
CONFIGURATION_FILE=ganttproject-eclipsito-config.xml
BOOT_CLASS=org.bardsoftware.eclipsito.Boot

if [ -n "$(echo \"$*\" | sed -n '/\(^\|\s\)-/{p;}')" ]; then
  $JAVA_COMMAND -Xmx256m -classpath $CLASSPATH:$LOCAL_CLASSPATH $BOOT_CLASS $CONFIGURATION_FILE -log -log_file $LOG_FILE "$@"
else
  exec $JAVA_COMMAND -Xmx256m -classpath $CLASSPATH:$LOCAL_CLASSPATH $BOOT_CLASS $CONFIGURATION_FILE -log -log_file $LOG_FILE "$@" &
fi

