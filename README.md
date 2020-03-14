# Gift List App

This is a work in progress web app that allows people to build and share gift lists. Invited participants can claim gifts without the creator knowing, similar to a baby or wedding registry. The development process is being documented on [my blog](https://chrisodonnell.dev); see the [introductory post](https://chrisodonnell.dev/posts/giftlist/intro/) for context.

## Setup

In order to run this application, you need to have the following installed:
* [node.js](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)
* [java](https://adoptopenjdk.net/)
* [clojure cli](https://clojure.org/guides/getting_started)

With these installed, run
```bash
npm install
```

to install javascript dependencies.

## Running

To run this application in development mode, start a shadow-cljs server with
```bash
npx shadow-cljs -d nrepl:0.7.0-beta1 -d cider/piggieback:0.4.2 -d refactor-nrepl:2.5.0 -d cider/cider-nrepl:0.25.0-SNAPSHOT server
```

Alternatively, CIDER users can run `cider-jack-in-cljs`. With this running, you can control compilation by accessing the shadow-cljs server at http://localhost:9630 and access the application at http://localhost:8080.
