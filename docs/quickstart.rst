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

Obelix require redis to run on the host, where redis run will beconfigurable later.


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
