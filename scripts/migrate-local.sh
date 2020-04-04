#!/bin/bash

pushd "$(git rev-parse --show-toplevel)"

docker build \
       -f Dockerfile.flyway \
       -t cpodonnell/mygiftlist-blog:migrate-local .

docker run --rm \
       --network "${1:-mygiftlist-blog_default}" \
       -e POSTGRES_USER=postgres \
       -e POSTGRES_PASSWORD=password \
       -e POSTGRES_HOSTNAME=postgres \
       -e POSTGRES_PORT=5432 \
       -e POSTGRES_DB=postgres \
       cpodonnell/mygiftlist-blog:migrate-local

popd
