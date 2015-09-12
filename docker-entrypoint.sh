#!/bin/bash
set -e

OBELIX_DEFAULT_PARAMETER=${OBELIX_DEFAULT_PARAMETER:="--neo4jStore '/app/obelix/database/graph.db' --enable-metrics --max-relationships 100 --redis-host cache --redis-queue-name logentries --redis-queue-prefix ''"}

if [ "$1" = 'obelix' ]; then
    OBELIX_JAVA="java -Xmx5000m -Xms5000m -XX:+UseConcMarkSweepGC"
    OBELIX_PATH="/app/obelix/obelix.jar"

    exec $OBELIX_JAVA -jar $OBELIX_PATH $OBELIX_PARAMETER $OBELIX_DEFAULT_PARAMETER
fi

exec "$@"
