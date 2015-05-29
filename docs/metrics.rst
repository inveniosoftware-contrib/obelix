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

Stored Metrics
------------
By default the metrics from Obelix are gathered from different modules and stored as json in redis.

An example of the stored data:

.. code-block:: javascript

    "metric" : {
        "total_feeded" : 736,
        "feeded" : 7,
        "total_recommendations_built" : 651,
        "recommendations_built" : 7,
        "all_relationships_count" : 9837,
        "all_users_count" : 2442,
        "all_items_count" : 5154,
        "cache_queue_size" : 3,
        "logentries_queue_size" : 1
        "timestamp" : "2015-05-25T02:20:45.637367",
    }

The `total_feeded` and `total_recommendations_built` are aggregated over time, where as the others are snapshots.

total_feeded: Number of interactions sent to Obelix (user x viewed item y) since the beggning.

feeded: The same as total_feeded, but the number represent the number of feeded since last time checked (typically a 5 minute interval).

total_recommendations_built: Number of recommendations built, it will be close to the number of total_feeded,
but if a user view several items in a short amount of time, Obelix will try to only build the recommendations for that user once.

recommendations_built: The same as total_recommendations_built, but the number represent the number of recommendations since last time checked (typically a 5 minute interval).

all_relationships_count: The current number of relationships in Obelix

all_users_count: The current number of users in Obelix

all_items_count: The current number of items in Obelix

cache_queue_size: The number of items in the cache queue (to build recommendations).

logentries_queue_size: The number of items in the queue ready for feeding.
l