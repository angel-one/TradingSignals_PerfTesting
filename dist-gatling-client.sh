#!/usr/bin/env bash
#
# Copyright 2016
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# 		http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

USER_ARGS="$@"

JAVA_OPTS="-server -XX:+UseThreadPriorities  -XX:ThreadPriorityPolicy=42 -Xms1G -Xmx1G -Xmn100M -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ${USER_ARGS}"

export SPRING_CONFIG_NAME=config.yml
CLIENT_PATH="libs/gatling-client-1.0.2-SNAPSHOT.jar"
# Run Gatling
java $JAVA_OPTS -jar ${CLIENT_PATH} Engine --spring.config.location=config.yml

#Example /bin/bash dist-gatling-client.sh -Dclient.userName=Command -Dclient.parallelism=1