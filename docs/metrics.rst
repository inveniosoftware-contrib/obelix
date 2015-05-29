Metrics
==========

Introduction
------------

The metrics are produced and stored in an ObelixStore object, this may be an internal ObelixStoreImpl or Redis.
By default, this is stored in redis.

Enable metrics
------------
For metrics to be collected and stored, the --enable-metrics argument needs to be passord to the jar.

.. code-block:: bash

    java -jar obelix.jar

    # Enable Metrics
    --enable-metrics
