# RRSS

## Ring Redis Session Store

A simple *Ring* session store implemented on top of
[Redis](http://code.google.com/p/redis/) using
[Jedis](https://github.com/xetorthio/jedis).

[Ring](https://github.com/mmcgrana/ring) is the fantastic Clojure web application
library.

### Usage

rrss is a drop-in replacement for Ring native stores:

    (use 'rrss)
    (def app
      (-> ....
        ... other middlewares ...
        (wrap-session {:store (redis-store)})
        ....))

You can pass options such as:

    (def store
      (redis-store {:host "localhost" :port 6379}))

### Installation

Add [rrss/rrss "0.2.1-SNAPSHOT"] to your leiningen dependencies

### License

Copyright (C) 2010 Sebastián Galkin

Distributed under the Eclipse Public License, the same as Clojure.
