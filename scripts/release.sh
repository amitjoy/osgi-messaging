#!/usr/bin/env bash
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
set -e

echo "🏁 Retrieving Version"
version=$(cat cnf/version/current.version)
version_without_snapshot=${version%".SNAPSHOT"}

read -p "Enter the next version (e.g. 1.2.0): " next_version
if [ -z "$next_version" ]; then
	echo "Error: Next version cannot be empty."
	exit 1
fi

if [[ ! "$next_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
	echo "Error: Next version must be in the format X.Y.Z (e.g., 1.2.0)"
	exit 1
fi

echo ""
echo "--------------------------------------------------"
echo "Current Version (to be released): $version_without_snapshot"
echo "Next Snapshot Version           : $next_version.SNAPSHOT"
echo "--------------------------------------------------"
echo ""

read -p "Do you want to proceed? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
	echo "Release cancelled."
	exit 1
fi

echo "🏁 Updating Version"
version_without_snapshot=${version%".SNAPSHOT"}
echo $version_without_snapshot > cnf/version/current.version

echo "🏁 Committing Changes"
git add .
git commit -m "🏁 REL v$version_without_snapshot Preparation"

echo "🏁 Creating Tag: v$version_without_snapshot"
git tag v$version_without_snapshot

echo "🏁 Pushing Tag: v$version_without_snapshot"
git push origin v$version_without_snapshot

echo "🏁 Cleaning Sonatype Staging Folders"
rm -rf cnf/cache/sonatype-release/* 2>/dev/null || true
rm -rf cnf/target/sonatype-staging/* 2>/dev/null || true

echo "🏁 Releasing Bundles"
./gradlew :in.bytehue.messaging.mqtt5.api:release
./gradlew :in.bytehue.messaging.mqtt5.provider:release
./gradlew :in.bytehue.messaging.mqtt5.remote.adapter:release

baseline_version=$(cat cnf/version/current.version)
echo "🏁 Updating OSGi Messaging Adapter Snapshot Version to $next_version"
echo "🏁 Updating OSGi Messaging Adapter Baseline Version to $baseline_version"

echo $next_version.SNAPSHOT > cnf/version/current.version
echo $baseline_version > cnf/version/baseline.version

echo "🏁 Committing the changes to current branch"

git add .
git commit -m "🏁 Next Development Cycle Preparation"