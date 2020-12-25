# Gift List App

This is a work in progress web app that allows people to build and share gift lists. Invited participants can claim gifts without the creator knowing, similar to a baby or wedding registry. The development process is being documented on [my blog](https://chrisodonnell.dev); see the [introductory post](https://chrisodonnell.dev/posts/giftlist/intro/) for context.

## Setup

In order to run this application, you need to have the following installed:
* [node.js](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)
* [java](https://adoptopenjdk.net/)
* [clojure cli](https://clojure.org/guides/getting_started)
* [docker](https://docs.docker.com/get-docker/)
* [docker compose](https://docs.docker.com/compose/install/)

With these installed, run
```bash
npm install
```

to install javascript dependencies.

## Running

### Database

We run a local postgres database inside docker-compose. To start the database, run
```bash
docker-compose up -d
```
After starting the database, you'll need to run migrations, which you can do with
```bash
make migrate
```
There's also a convenience script available at `./scripts/psql` to open up a psql client connected to the database. There are resources to learn more about working with a database inside docker compose in the [documentation](https://docs.docker.com/compose/).

### Application

To run this application in development mode, start a shadow-cljs server with
```bash
npx shadow-cljs -A:dev:backend:frontend:test -d nrepl:0.8.2 -d cider/piggieback:0.5.1 -d refactor-nrepl:2.5.0 -d cider/cider-nrepl:0.25.3 server
```

With this running, you can control compilation by accessing the shadow-cljs server at http://localhost:9630. In addition, this command will start up an nrepl server, which you should connect to with your preferred REPL. Alternatively, CIDER users can run `cider-jack-in-clj&cljs` and choose `shadow-cljs`.

In your clojure repl, make sure you are in the `user` namespace and evaluate `(go)`. This will start our web server. With the web server running, you can access the application at http://localhost:3000.

## Tests

To run the test suite from the command line, run
```bash
make test
```

In order to run tests from the repl, you need to start up the test database. You can do this with
```bash
make test-up
```

With the test database up and running, you should be able to run tests. You can shut down the test database with
```bash
make test-down
```

## Deployment

To create an uberjar `target/mygiftlistrocks.jar` that includes production frontend assets, run
```
make uberjar
```

You can then run this uberjar with
```
java -cp target/mygiftlistrocks.jar clojure.main -m rocks.mygiftlist.main
```

You can run database migrations with
```
clojure -X:migrate :database-url '"postgresql://me:password@mydbhost:port/dbname"'
```

You can deploy to dokku with
```
git push dokku master
```

## Maintenance

To find outdated dependencies, you can run
```
make outdated
```

To create a build report documenting how large frontend dependencies are in your bundle, run
```
make build-report
```
