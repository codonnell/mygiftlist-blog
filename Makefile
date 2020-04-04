.PHONY: test test-up test-down

test:
	POSTGRES_PORT=15433 docker-compose -p mygiftlist-blog-test up -d
	scripts/migrate-local.sh mygiftlist-blog-test_default
	clojure -A:test:run-tests
	docker-compose -p mygiftlist-blog-test down

test-up:
	POSTGRES_PORT=15433 docker-compose -p mygiftlist-blog-test up -d
	scripts/migrate-local.sh mygiftlist-blog-test_default

test-down:
	docker-compose -p mygiftlist-blog-test down
