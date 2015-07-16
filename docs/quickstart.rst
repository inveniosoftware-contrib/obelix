Quickstart
==========

Installation
------------

The installation is as simple as downloading the jar file
Note: This jar-file require java8 to be installed.

.. code-block:: bash

    wget http://obelix.io/obelix.jar


Dependencies
------------

Obelix require the following software to be installed

 - Redis
 - Java8

Usage
-----

Using Obelix can be as little effort as calling ``java -jar obelix.jar``.
However, for most projects it is required to configure more options.

.. code-block:: bash

    java -jar obelix.jar

    # Configure where to store the graph database, the default location is the same folder as the jar file
    --neo4jstore /path/to/store/graph.db

    # Set a maximum number of relationships per node, this will remove the oldest when the limit is reached
    --max-relationships 100

    # Set the number of workers to read from the log queue
    --workers 1

    # Set the name of the redis queue to look for new log entries
    --redis-queue-name logentries

    # Set the http port for the Obelix HTTP API
    --web-port 4000

    # Set the recommendations depth, this means how deep the graph will be traversed
    --recommendation-depth 3

    # Set Obelix in batch import mode, this means that it will import all entries in the logentries queue very effeciently. However, it does not handles duplicates.
    --batch-import-all

    # Tell Obelix to rebuild all recommendations
    --build-cache-for-all-users-on-startup

    # Enable Metrics
    --enable-metrics

    # Enable Neo4j web server on localhost, for debugging and exploring the database only. PORT is optional, default is 7575.
    --neo4j-webserver PORT

Running Obelix as a Daemon (background service) on Ubuntu 14.04
--------------------------
In a production environment it is wise to run Obelix as a background service.
You can do this easily by using supervisor.

To set up supervisor, first you need to install the package

.. code-block:: bash

    sudo apt-get install supervisor


Then you need to create a configuration file named /etc/supervisor/conf.d/obelix.conf with the following content

.. code-block:: bash

    [program:obelix]
    user = someUserWithAccessToTheDirectory
    autostart = true
    autorestart = true
    command = java -jar /mnt/obelix/obelix.jar --option1 value1 --option2 value2...
    stdout_logfile = /var/log/obelix.log
    stderr_logfile = /var/log/obelix.error.log


Then you simply restart the supervisor service

.. code-block:: bash

    sudo service supervisor restart


Then you can tail the log to see that Obelix is running


.. code-block:: bash

    sudo tail -f /var/log/obelix.log
    sudo tail -f /var/log/obelix.error.log


Recommended JVM settings
------------------------
For Obelix to perform well, it is recommended to enable the ``-XX:+UseConcMarkSweepGC`` option on the JVM.

It is also recommended to set your ``-Xmx`` and ``Xms`` settings to apropriate values for your host.

An example of a configuration may be:

.. code-block:: bash

    java -Xmx5000m -Xms5000m -XX:+UseConcMarkSweepGC -jar /mnt/obelix/obelix.jar --neo4jstore /mnt/obelix/graph.db
