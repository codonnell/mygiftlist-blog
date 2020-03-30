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
./scripts/migrate-local.sh
```
There's also a convenience script available at `./scripts/psql` to open up a psql client connected to the database. There are resources to learn more about working with a database inside docker compose in the [documentation](https://docs.docker.com/compose/).

### Application

To run this application in development mode, start a shadow-cljs server with
```bash
npx shadow-cljs -d nrepl:0.7.0 -d cider/piggieback:0.4.2 -d refactor-nrepl:2.5.0 -d cider/cider-nrepl:0.25.0-SNAPSHOT server
```

With this running, you can control compilation by accessing the shadow-cljs server at http://localhost:9630. In addition, this command will start up an nrepl server, which you should connect to with your preferred REPL. Alternatively, CIDER users can run `cider-jack-in-clj&cljs`.

In your clojure repl, make sure you are in the `user` namespace and evaluate `(start)`. This will start our web server. With the web server running, you can access the application at http://localhost:3000.
