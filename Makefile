.PHONY: test test-up test-down migrate uberjar run build-report outdated

test:
	POSTGRES_PORT=15433 docker-compose -p mygiftlist-blog-test up -d
	clojure -X:migrate :database-url '"postgresql://postgres:password@localhost:15433/postgres"'
	clojure -M:backend:dev:test:run-tests
	docker-compose -p mygiftlist-blog-test down

test-up:
	POSTGRES_PORT=15433 docker-compose -p mygiftlist-blog-test up -d
	clojure -X:migrate :database-url '"postgresql://postgres:password@localhost:15433/postgres"'

test-down:
	docker-compose -p mygiftlist-blog-test down

migrate:
	clojure -X:migrate :database-url '"postgresql://postgres:password@localhost:15432/postgres"'

uberjar:
	npm install
	npx shadow-cljs release prod
	npm run css-build
	clojure -X:depstar uberjar :jar target/mygiftlistrocks.jar :aliases '[:backend]'

run:
	java -cp target/mygiftlistrocks.jar clojure.main -m rocks.mygiftlist.main

build-report:
	npx shadow-cljs run shadow.cljs.build-report prod report.html

outdated:
	clojure -M:outdated -a backend,frontend,dev,test,depstar,migrate,outdated
