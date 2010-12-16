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

This will create a default store, connecting a Redis server in the given host
and port. Every sessions will be saved under a key of the form `sessions:ID`
where `ID` is a random UUID. If you want to customize the keys, you can pass
a `key-mapper` option, with a function that maps session ids to Redis keys.
For instance

    (defn key-mapper [key]
      (str "sessions:" key ":data"))

    (def store
      (redis-store {:key-mapper key-mapper}))

The default store doesn't handle any kind of session expiration, it will only
save every session under its own key.

### Expiring sessions

If you want to delete old sessions you can use:

    (def store (expiring-redis-store))

This store will delete sessions older than a week by running a background thread.
You can customize this time with options:

    (def one-minute 60)
    (def one-hour (* 60 one-minute))
    (def store (expiring-redis-store :duration one-hour :resolution one-minute))

This way, sessions will last for an hour since the last change, and the background
thread will search for old sessions every minute.

### Advanced use

rrss is highly configurable. You can create your own Redis session store, using
rrss `Steps`. Steps are phases during the session store operation in the store.
For example, key translation and persistence, are implemented as Steps in rrss.
Other example of Step in rrss code is session expiring. You can create your
own steps to extend rrss functionality.

Read the documentation for more information on Steps.

### Installation

Add [rrss/rrss "0.2.2"] to your leiningen dependencies

### Documentation

You can check the autodoc generated documentation
[here](http://paraseba.github.com/rrss/rrss-api.html)

### License

Copyright (C) 2010 Sebastián Galkin

Distributed under the Eclipse Public License, the same as Clojure.
