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
MQTT_PORT=1883
PID_FILE="hivemq_pid"

[ -f "$PID_FILE" ] && rm "$PID_FILE"

# --- 1. SETUP BROKER ---
./scripts/start-broker.sh --daemon
if [ ! -f "$PID_FILE" ]; then
  echo "❌ Error: $PID_FILE not found. Broker failed to start."
  exit 1
fi
HIVEMQ_PID=$(cat "$PID_FILE")

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
