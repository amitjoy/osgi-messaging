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

DAEMON_MODE=0
if [ "$1" == "--daemon" ]; then
  DAEMON_MODE=1
fi

# --- 0. PRE-FLIGHT CHECKS ---

# A. Verify Java 17
JAVA_VER=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VER" != "17" ]]; then
  echo "⚠️  Warning: HiveMQ officially requires Java 17. Found Java $JAVA_VER."
  echo "Attempting to start anyway..."
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

# --- 1. SETUP BROKER ---
if [ ! -d "$HIVEMQ_DIR" ]; then
  echo "--- [SETUP] Downloading HiveMQ CE ${HIVEMQ_VERSION} ---"
  curl -L -O "$HIVEMQ_URL"
  unzip -q "$HIVEMQ_ZIP"
  rm "$HIVEMQ_ZIP"
fi

echo "--- [SETUP] Starting HiveMQ Broker ---"
if [ "$DAEMON_MODE" -eq 1 ]; then
  echo "✅ Broker will run in the background."
  echo "--------------------------------------------------------"
  cd "$HIVEMQ_DIR"
  ./bin/run.sh > ../hivemq.log 2>&1 &
  echo $! > "../$PID_FILE"
else
  echo "✅ Broker will run in the foreground. Press Ctrl+C to stop it."
  echo "--------------------------------------------------------"
  # Run in foreground so Eclipse users can see logs and kill it easily
  cd "$HIVEMQ_DIR"
  exec ./bin/run.sh
fi
