# Blizzard

A Flake ID generation service provided as a Ring middleware.

![A blizzard](http://upload.wikimedia.org/wikipedia/commons/0/0d/Blizzard2_-_NOAA.jpg)

## Installation
`blizzard` is available via [Clojars](https://clojars.org/blizzard):

```clojure
[blizzard "0.3.1"]
```

## Usage

Blizzard may be used either as a standalone HTTP service or as a Ring
middleware. In the case of the former, simply invoking `lein run` is sufficient
to start the Ring server.

To use the Ring middleware, simply wrap the handler:

```clojure
=> (require '[blizzard.core :as blizzard])
=> (def handler (-> (constantly {:status 404 :body "Not Found"})
                    blizzard/wrap-flake))
```

In either the standalone or middleware use case, the Ring application will
provide two routes, /flake and /flake/:n which generate a single flake and N
flakes, respectively.

### Consumers

Note that blizzard produces responses encoded with [transit](https://github.com/cognitect/transit-clj).
Either JSON or Msgpack encodings may be produced. By default, JSON is used. To
request a specific format, specify either application/json or
application/x-msgpack in the Accept header, for JSON or Msgpack respectively.

### Environment Variables

When using the standalone server, environment variables provide runtime
configuration:

  BLIZZARD_HOST       - Defaults to localhost.

  BLIZZARD_PORT       - Defaults to 3000.

  BLIZZARD_MAX_IDS    - The maximum allowed IDs per batch request. Default
                        1000.

  BLIZZARD_JETTY_OPTS - Defaults to {:join? true}.
