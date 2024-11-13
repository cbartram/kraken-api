# Kraken API

This repository contains an AWS lambda function which functions as the API for the [Kraken
Client](https://github.com/cbartram/kraken-client). This API handles:

- Any user account updates (CRUD) through AWS Cognito
- Linking Discord accounts via OAuth flow
- Purchasing new Kraken plugins
- Allowing JAR file plugin downloads
- Validating running plugins

This API requires that you have an AWS account with a lambda function, API gateway, and S3 bucket setup to fully utilize this.

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

## Plugin Verification & Loading

When the Kraken client needs to load a plugin it will make a request on behalf of the authenticated user to generate
signed URL's for plugin JAR files in S3. Signed URL's provide temporary (30s) access to JAR files. Once a signed URL is 
generated the client will read the JAR file directly into memory and load each Kraken Plugin class from the JAR before the signed
URL expires. 

Once the classes are loaded into memory the plugin can be registered with RuneLite and starts normally.

## Discord Tools

Since plugin purchases are managed through the Discord ticketing system we have a special tool which makes generating
license keys easy. Make sure you have [JQ installed](https://github.com/jqlang/jq) on your system.

Run:

```shell
./scripts/discord_grant_plugin.sh <discord_user_id> <refresh_token>
```

The discord id can be found in the discord support ticket for the user while the refresh token can be found for the user in
the AWS cognito console.

The tool will also prompt for the plugin name and a valid integer duration where the plugin is active. Make sure the name
matches a prefix of the JAR file in S3. i.e:

- "Alchemical-Hydra"
- "Cox-Helper"
- etc...

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

There are a few layers of additional security beyond signed URL's for plugin downloads:

- First, the plugin expiration timestamp is checked before any signed URL's are generated and returned to the client Thus, after a plugin expires it should
no longer be loaded into the client. 
- Second, users will enter a license key for each plugin they purchase. An additional API
call is made to the server to validate the users license key **after** the plugin is loaded but **before** the plugin is registered with RuneLite & started
- Finally, when a license key is generated during the plugin purchase process a hardware ID is associated with it. When the API call is made to validate the license
the hardware ID is also sent to verify that the plugin is: not expired, valid license, and running on the right hardware.

## Running the tests

No tests yet.

## Deployment

Deployment is managed through AWS lambda and API gateway. To package and upload the go binary to Lambda you can run the
deployment script: `./scripts/deploy.sh` with proper AWS credentials.

To make any adjustments to the API spec (i.e adding a new route, removing a route, or updating an HTTP method)
you will need to deploy the stage within the API Gateway console in AWS.

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
