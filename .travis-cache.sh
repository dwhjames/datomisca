#!/usr/bin/env bash

curl -L https://s3.amazonaws.com/pellucidanalytics-travis-ci-cache/datomisca.tar.gz | tar xzf -

# always succeed: the cache is optional
exit 0
