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

Build this application by running: 

```shell
go build -tags lambda.norpc -o bootstrap main.go
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
