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

MQTT_PORT=1883
PID_FILE="hivemq_pid"

# --- 0. PRE-FLIGHT CHECKS ---

# A. Verify Java 17
JAVA_VER=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VER" != "17" ]]; then
  echo "❌ Error: Java 17 is required. Found Java $JAVA_VER"
  exit 1
fi

# B. Kill existing HiveMQ processes for a clean start
echo "--- [CLEANUP] Scrubbing existing HiveMQ processes ---"
EXISTING_PIDS=$(pgrep -f "$HIVEMQ_DIR")

if [ ! -z "$EXISTING_PIDS" ]; then
  echo "Stopping existing HiveMQ process(es): $EXISTING_PIDS"
  echo "$EXISTING_PIDS" | xargs kill -9 2>/dev/null
  sleep 2
fi

# Final port check
if nc -z localhost $MQTT_PORT; then
  echo "❌ Error: Port $MQTT_PORT is occupied. Please stop any other MQTT services."
  exit 1
fi

[ -f "$PID_FILE" ] && rm "$PID_FILE"

# --- 1. SETUP BROKER ---
if [ ! -d "$HIVEMQ_DIR" ]; then
  echo "--- [SETUP] Downloading HiveMQ CE ${HIVEMQ_VERSION} ---"
  curl -L -O "$HIVEMQ_URL"
  unzip -q "$HIVEMQ_ZIP"
  rm "$HIVEMQ_ZIP"
fi

echo "--- [SETUP] Starting HiveMQ Broker (Java 17) ---"
./"$HIVEMQ_DIR"/bin/run.sh > hivemq.log 2>&1 &
HIVEMQ_PID=$!
echo $HIVEMQ_PID > "$PID_FILE"

# Wait for Startup
echo "Waiting for HiveMQ..."
MAX_RETRIES=30
COUNT=0
while ! nc -z localhost $MQTT_PORT; do
  sleep 1
  ((COUNT++))
  if [ $COUNT -ge $MAX_RETRIES ]; then
    echo "❌ Timeout waiting for Broker"
    cat hivemq.log
    kill "$HIVEMQ_PID" 2>/dev/null
    exit 1
  fi
  echo -n "."
done
echo -e "\n✅ Broker is UP"

# --- 2. EXECUTE BUILD ---
# Use all script arguments as the command, or default to build
if [ $# -gt 0 ]; then
    echo "--- [EXEC] Running Custom Command: $@ ---"
    "$@"
else
    echo "--- [EXEC] Running Default: ./gradlew build ---"
    ./gradlew clean build
fi
EXIT_CODE=$?

# --- 3. TEARDOWN ---
if [ -f "$PID_FILE" ]; then
  echo "--- [CLEANUP] Stopping Broker ---"
  PID=$(cat "$PID_FILE")
  kill "$PID" 2>/dev/null
  rm "$PID_FILE"
fi

exit $EXIT_CODE
