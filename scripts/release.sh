#!/bin/bash
#-------------------------------------------------------------------------------
# Copyright 2021-2026 Amit Kumar Mondal
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy
# of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.
#-------------------------------------------------------------------------------
# --- CONFIGURATION ---
HIVEMQ_VERSION="2023.5"
HIVEMQ_ZIP="hivemq-ce-${HIVEMQ_VERSION}.zip"
HIVEMQ_DIR="hivemq-ce-${HIVEMQ_VERSION}"
HIVEMQ_URL="https://github.com/hivemq/hivemq-community-edition/releases/download/${HIVEMQ_VERSION}/${HIVEMQ_ZIP}"

# JRE 11 Setup
JRE_DIR="portable-jre-11"
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

if [[ "$OS" == "darwin" ]]; then OS="mac"; fi
if [[ "$ARCH" == "x86_64" ]]; then ARCH="x64"; fi
if [[ "$ARCH" == "arm64" ]]; then ARCH="aarch64"; fi

JRE_URL="https://api.adoptium.net/v3/binary/latest/11/ga/${OS}/${ARCH}/jre/hotspot/normal/eclipse"

MQTT_PORT=1883
PID_FILE="hivemq_pid"

# --- 1. SETUP BROKER ---
if ! nc -z localhost $MQTT_PORT; then

    # A. Download HiveMQ if missing
    if [ ! -d "$HIVEMQ_DIR" ]; then
      echo "--- [SETUP] Downloading HiveMQ CE ${HIVEMQ_VERSION} ---"
      curl -L -O $HIVEMQ_URL
      unzip -q $HIVEMQ_ZIP
      rm $HIVEMQ_ZIP
    fi

    # B. Download Portable Java 11 (If missing)
    if [ ! -d "$JRE_DIR" ]; then
      echo "--- [SETUP] Downloading Portable Java 11 ($OS / $ARCH) ---"
      curl -L -o jre11.tar.gz "$JRE_URL"
      if [ ! -s jre11.tar.gz ]; then echo "❌ Download failed"; exit 1; fi
      mkdir -p "$JRE_DIR"
      tar -xzf jre11.tar.gz -C "$JRE_DIR"
      rm jre11.tar.gz
    fi

    # C. Locate Java 11 Binary
    JAVA_BIN=$(find "$JRE_DIR" -name java -type f | grep "/bin/java$" | head -n 1)
    REAL_JAVA_HOME=$(dirname $(dirname "$JAVA_BIN"))

    echo "--- [SETUP] Starting HiveMQ Broker (using local Java 11) ---"
    
    # Fix permissions
    chmod +x "$JAVA_BIN"
    if [[ "$OS" == "mac" ]]; then xattr -d com.apple.quarantine "$JAVA_BIN" 2>/dev/null || true; fi

    # FIX: Run HiveMQ with custom JAVA_HOME explicitly, WITHOUT exporting it globally
    (
        export JAVA_HOME="$REAL_JAVA_HOME"
        export PATH="$REAL_JAVA_HOME/bin:$PATH"
        ./$HIVEMQ_DIR/bin/run.sh > hivemq.log 2>&1 &
        echo $! > ../$PID_FILE 
        # Note: writing to ../$PID_FILE because we are inside a subshell? 
        # Actually, simpler to just grab PID here before subshell exits won't work easily.
    ) &
    
    # Alternative Clean Execution for Background Process:
    env JAVA_HOME="$REAL_JAVA_HOME" ./$HIVEMQ_DIR/bin/run.sh > hivemq.log 2>&1 &
    HIVEMQ_PID=$!
    echo $HIVEMQ_PID > $PID_FILE

    # D. Wait for Startup
    echo "Waiting for HiveMQ..."
    MAX_RETRIES=30
    COUNT=0
    while ! nc -z localhost $MQTT_PORT; do
      sleep 1
      COUNT=$((COUNT+1))
      if [ $COUNT -ge $MAX_RETRIES ]; then
        echo "❌ Timeout waiting for Broker"
        cat hivemq.log
        kill $HIVEMQ_PID 2>/dev/null
        exit 1
      fi
      echo -n "."
    done
    echo -e "\n✅ Broker is UP"
else
    echo "⚠️  Broker likely already running."
fi

# --- 2. EXECUTE BUILD ---
# The environment is now CLEAN. It uses whatever Java version allowed your manual build to pass.
RELEASE_CMD="${@:-./gradlew :in.bytehue.messaging.mqtt5.provider:release}"

echo "--- [EXEC] Running: $RELEASE_CMD ---"
$RELEASE_CMD -Dmqtt.server=localhost -Dmqtt.port=$MQTT_PORT
EXIT_CODE=$?

# --- 3. TEARDOWN ---
if [ -f $PID_FILE ]; then
  echo "--- [CLEANUP] Stopping Broker ---"
  PID=$(cat $PID_FILE)
  kill $PID 2>/dev/null
  rm $PID_FILE
fi

exit $EXIT_CODE