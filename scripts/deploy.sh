#!/usr/bin/bash

# Build the Go app
echo -e "\033[1;34m[INFO] Building Go binary...\033[0m"
GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -tags lambda.norpc -o bootstrap main.go

# Zip the bootstrap
echo -e "\033[1;34m[INFO] Zipping bootstrap...\033[0m"
zip deployment.zip bootstrap

# Publish to Lambda
echo -e "\033[1;34m[INFO] Publishing to Lambda...\033[0m"
if output=$(aws lambda update-function-code --function-name kraken-api --zip-file fileb://deployment.zip 2>&1); then
    echo -e "\033[1;32m[INFO] Upload to Lambda successful!\033[0m"
    echo "$output" > /dev/null
else
    echo -e "\033[1;31m[ERROR] Error uploading to AWS Lambda:\033[0m"
    echo "$output"
fi

rm ./bootstrap
rm ./deployment.zip

