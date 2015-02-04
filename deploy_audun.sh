#!/bin/sh
mvn package
#scp /Users/frecar/code/frecar/obelix/target/obelix-1.0-SNAPSHOT-jar-with-dependencies.jar root@178.62.58.8:~/obelix.jar
#ssh -t -A root@178.62.58.8 ./deploy_obelix.sh

#scp /Users/frecar/code/frecar/obelix/target/obelix-1.0-SNAPSHOT-jar-with-dependencies.jar root@178.62.73.114:~/obelix.jar
#ssh -t -A root@178.62.73.114 ./deploy_obelix.sh

scp /Users/frecar/code/frecar/obelix/target/obelix-1.0-SNAPSHOT-jar-with-dependencies.jar root@obelix5.tind.io:~/obelix.jar
#ssh -t -A root@obelix2.tind.io sudo ./deploy_obelix.sh
