#!/bin/sh
mvn package
scp /Users/frecar/code/frecar/obelix/target/obelix-1.0-SNAPSHOT-jar-with-dependencies.jar frecar@lxplus.cern.ch:~/obelix.jar
ssh -t -A frecar@lxplus.cern.ch ssh -t -A frecar@cds-csp-test.cern.ch ./deploy_obelix.sh
