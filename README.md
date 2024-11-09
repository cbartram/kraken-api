# Kraken API

This repository contains an AWS lambda function which functions as the API for the Kraken
Client.

## Getting Started

To get started clone this project and run a gradle build:

```shell
git clone https://github.com/cbartram/kraken-loader-plugin.git

gradle build
```

### Prerequisites

This project requires Go v1.23.2 installed on your system. You can install [Go here](https://go.dev/doc/install).

If you plan to run this as a lambda function you will need an AWS account, credentials, and the AWS CLI v2 installed on
your machine.

### Running

Build this application by running the following: 

```shell
export GOOS=linux
export GOARCH=amd64
export CGO_ENABLED=0

go build -tags lambda.norpc -o bootstrap main.go
```
*Note: `GOOS` should always be linux regardless of which OS you are running on.*

Next zip the binary so it can be uploaded to the lambda servers. 

```shell
zip deployment.zip bootstrap
```

Finally upload the binary to AWS Lambda so it can be executed:

```shell
aws lambda update-function-code --function-name kraken-api --zip-file fileb://deployment.zip --color on --output table
```

You can also run `./scripts/deploy.sh` to build, zip, and upload the function to AWS.

## API Plugin Fetching Sequence

The following sequence diagram details the process of validating requests and generating a signed url that can be
used to fetch plugin JAR files.

```
sequenceDiagram
    participant Client as Kraken Client
    participant Cognito as AWS Cognito
    participant API as API Gateway/Lambda
    participant S3 as S3 Bucket
    
    Note over Client,S3: Authentication Flow
    Client->>Cognito: 1. User Login
    Cognito-->>Client: 2. ID Token + Access Token
    
    Note over Client,S3: Plugin Download Flow
    Client->>API: 3. Request Plugin Download (ID Token)
    API->>Cognito: 4. Validate Token
    API->>Cognito: 5. Get user attributes
    Cognito-->>API: 6. custom:purchased_plugins
    
    Note over API: 7. Verify plugin access
    
    alt Plugin access allowed
        API->>S3: 8a. Generate pre-signed URL
        API-->>Client: 9a. Return pre-signed URL
        Client->>S3: 10a. Download plugin using pre-signed URL
    else Plugin access denied
        API-->>Client: 8b. Access Denied
    end
```

## Running the tests

No tests yet.

## Deployment

Deployment will come later in this project's lifecycle.

## Built With

- [GoLang](https://go.dev/doc/install) - Programming Language

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code
of conduct, and the process for submitting pull requests to us.

## Versioning

We use [Semantic Versioning](http://semver.org/) for versioning. For the versions
available, see the [tags on this
repository](https://github.com/cbartram/kraken-loader-plugin/tags).

## Authors

- **C. Bartram** - *Initial Project implementation* - [RuneWraith](https://github.com/cbartram)

See also the list of
[contributors](https://github.com/PurpleBooth/a-good-readme-template/contributors)
who participated in this project.

## License

This project is licensed under the [CC0 1.0 Universal](LICENSE.md)
Creative Commons License - see the [LICENSE.md](LICENSE.md) file for
details

## Acknowledgments

- RuneLite for making an incredible piece of software and API.
