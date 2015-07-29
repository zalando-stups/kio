# Kio

[![Build Status](https://travis-ci.org/zalando-stups/kio.svg?branch=master)](https://travis-ci.org/zalando-stups/kio)

Kio is the application registry in the [STUPS ecosystem](http://zalando-stups.github.io). It manages the basic
information about an organisation's applications.

## Download

Releases are pushed as Docker images in the [public Docker registry](https://registry.hub.docker.com/u/stups/kio/):

* Image: [stups/kio](https://registry.hub.docker.com/u/stups/kio/tags/manage/)

You can run Kio by starting it with Docker:

    $ docker run -it stups/kio

## Requirements

* PostgreSQL 9.3+

## Configuration

Configuration is provided via environment variables during start.

Variable         | Default                | Description
---------------- | ---------------------- | -----------
HTTP_PORT        | `8080`                 | TCP port to provide the HTTP API.
HTTP_CORS_ORIGIN |                        | Domain for cross-origin JavaScript requests. If set, the Access-Control headers will be set.
DB_SUBNAME       | `//localhost:5432/kio` | JDBC connection information of your database.
DB_USER          | `postgres`             | Database user.
DB_PASSWORD      | `postgres`             | Database password.

Example:

```
$ docker run -it \
    -e HTTP_CORS_ORIGIN="*.zalando.de" \
    -e DB_USER=kio \
    -e DB_PASSWORD=kio123 \
    stups/kio
```

## Building

    $ lein uberjar
    $ lein docker build

## Releasing

    $ lein release :minor

## Developing

Kio embeds the [reloaded](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded) workflow for interactive
development:

    $ lein repl
    user=> (go)
    user=> (reset)

## License

Copyright © 2015 Zalando SE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
