.PHONY: test test-up test-down migrate uberjar run build-report outdated

test:
	POSTGRES_PORT=15433 docker-compose -p mygiftlist-blog-test up -d
	DATABASE_URL=postgresql://postgres:password@localhost:15433/postgres clojure -A:migrate
	clojure -A:backend:dev:test:run-tests
	docker-compose -p mygiftlist-blog-test down

test-up:
	POSTGRES_PORT=15433 docker-compose -p mygiftlist-blog-test up -d
	DATABASE_URL=postgresql://postgres:password@localhost:15433/postgres clojure -A:migrate

test-down:
	docker-compose -p mygiftlist-blog-test down

migrate:
	DATABASE_URL=postgresql://postgres:password@localhost:15432/postgres clojure -A:migrate

uberjar:
	npm install
	npx shadow-cljs release prod
	npm run css-build
	clojure -A:backend:uberjar

run:
	java -cp target/mygiftlistrocks.jar clojure.main -m rocks.mygiftlist.main

build-report:
	npx shadow-cljs run shadow.cljs.build-report prod report.html

outdated:
	clojure -A:outdated -a backend,frontend,dev,test,uberjar,outdated
