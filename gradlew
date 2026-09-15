#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

DIRNAME=`dirname "$0"` || exit 1
APP_HOME="`cd "$DIRNAME" >/dev/null 2>&1 && pwd`"
APP_HOME=`expr "$APP_HOME" : '\(.*\)/[^/]*$'` || APP_HOME="$APP_HOME"

if [ ! -d "$APP_HOME" ] ; then
    exit 1
fi

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Attempt to determine APP_HOME again, workaround for possible symlink issues
expr "/$0" : '/\(.*\)/.//*$' > /dev/null
APP_HOME=`expr "/$0" : '/\(.*\)/.//*$'` || APP_HOME="$DIRNAME"

if [ ! -d "$APP_HOME" ]; then
    APP_HOME=`cd "$DIRNAME" && pwd -P` || exit 1
fi

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=`find "$APP_HOME" -name "gradle-wrapper.jar" 2>/dev/null`
GRADLE_OPTS="$(concat_lines "$GRADLE_OPTS") $(java_opts_of_property_file "$APP_HOME/gradle.properties")"

exec "$JAVACMD" $DEFAULT_JVM_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
