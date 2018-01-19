#!/bin/bash

./build.sh

rsync -vrP --delete ./target/dist/lib ubuntu@basil.accur8.io:/opt/s3-backup