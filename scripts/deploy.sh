#!/usr/bin/env sh

GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -tags lambda.norpc -o bootstrap main.go

zip deployment.zip bootstrap

aws lambda update-function-code --function-name kraken-api --zip-file fileb://deployment.zip
