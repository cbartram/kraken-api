# Kraken API

This repository contains an AWS lambda function which functions as the API for the [Kraken
Client](https://github.com/cbartram/kraken-client). This API handles:

- Any user account updates (CRUD) through AWS Cognito
- Linking Discord accounts via OAuth flow
- Purchasing new Kraken plugins
- Allowing JAR file plugin downloads
- Validating running plugins

## Getting Started

To get started clone this project and run a gradle build:

```shell
git clone https://github.com/cbartram/kraken-loader-plugin.git

gradle build
```

### Prerequisites

This project requires Go v1.23.2 installed on your system. You can install [Go here](https://go.dev/doc/install).

### Running

Build this application by running the following: 

```shell
export GOOS=linux
export GOARCH=amd64
export CGO_ENABLED=0

go build -o bootstrap main.go
```
*Note: `GOOS` should always be linux regardless of which OS you are running on.*

## Deployment

Deployment is managed through Kubernetes. To package and deploy the container you can run the
deployment script: `./scripts/deploy.sh 0.0.1` with a new image tag. 

This script will both build and release the API. Check that it was successful with `kubectl get pods`. You should
see a running pod called `kraken-api`.

# Management

The following sections describe how you can manage and configure Kraken plugins elements like:
- Adding plugins
- Plugin Sales
- Beta plugins

## Adding Plugins

- Start by adding the plugin source code to the [Kraken Plugins Repo](https://github.com/cbartram/kraken-plugins)
  - Confirm the source builds and a JAR is produced
  - Deploy the jar file from the [Kraken Plugins Repo](https://github.com/cbartram/kraken-plugins) using `./scripts/deploy.sh` (Note: this will also deploy any other queued plugin updates)
- Within the [Kraken Plugins Repo](https://github.com/cbartram/kraken-db) in the `/data` folder add plugin metadata to `plugin_metadata.json`
  - If this plugin will be part of a plugin pack add it to `plugin_packs.json` as well
  - Create an unlisted YouTube video showcasing the plugin and add the url to the metadata
  - Add an image name for the plugin like: `zuk.png`
- In [Kraken Frontend](https://github.com/cbartram/kraken-frontend) find a suitable picture for the plugin and put it in the `/src/public` folder name it the same as what is in the metadata like: `zuk.png`.
  - Re-deploy the frontend with `./scripts/deploy.sh x.y.z`
- In the Kraken DB repo run the following to update the metadata in prod:
  - `./main -db-name kraken -db-user kraken -db-password <password> -db-port 30306 -db-host kraken-db.duckdns.org`

## Revoking Plugin Access & Beta Plugins

Currently, there is no automated process for handling beta plugins. They are purchased with 0 tokens the same as normal plugins. When it comes time
to release it live you will need to run a script like this in order to revoke user access to their beta plugins:

```mysql
-- Define the plugin name you want to update
SET @plugin_name = 'some_plugin_name';

-- 1. Revoke all user access to the plugin by deleting from the `plugins` table
DELETE FROM plugins
WHERE name = @plugin_name;

-- 2. Update `is_in_beta` to false in `plugin_metadata`
UPDATE plugin_metadata
SET is_in_beta = FALSE
WHERE name = @plugin_name;

-- 3. Update the plugin pricing in `plugin_metadata_price_details`
UPDATE plugin_metadata_price_details pd
    JOIN plugin_metadata pm ON pd.plugin_metadata_id = pm.id
SET pd.month = 1000,
    pd.three_month = 2700,
    pd.year = 10000
WHERE pm.name = @plugin_name;
```

## Sales

To create a new sale for a set of plugins run the following API route. Note: this must be done with the owners discord OAuth token which prevents
normal users from creating plugin sales (for obvious reasons).

```shell
curl --location 'https://kraken-plugins.duckdns.org/api/v1/sale/create' \
--header 'Authorization: Basic <base64(discord_id:refresh_token)>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Summer Sale",
    "description": "Get ready to grind this summer with a 30% off on all plugins!",
    "discount": 30,
    "startTime": "2025-05-22T10:00:00Z",
    "endTime": "2025-06-01T22:00:00Z",
    "active": true,
    "pluginNames": ["Alchemical-Hydra", "Cerberus", "Zulrah"]
}'
```

## Testing with Stripe

Stripe checkout is used to direct users to the stripe checkout page. Afterwards, a series of webhooks will be fired to let the Kraken API know if the purchase
was successful or not. These webhooks are enqueued into a rabbitmq queue and processed sequentially to update our database with additional tokens.

To test this process locally run Note: only local events sent via stripe cli will work with this: 

```shell
# Start RabbitMQ docker container locally
docker run --env=RABBITMQ_DEFAULT_USER=kraken --env=RABBITMQ_DEFAULT_PASS=<PASSWORD> --env=RABBITMQ_VERSION=4.0.7 --env=RABBITMQ_HOME=/opt/rabbitmq --env=HOME=/var/lib/rabbitmq --volume=/var/lib/rabbitmq --network=bridge -p 15672:15672 -p 5672:5672 --restart=no --runtime=runc -d rabbitmq:4.0.7-management

# Login to stripe (tokens expire every 90 days)
stripe login
stripe listen --forward-to localhost:8080/api/v1/stripe/webhook

# In the .env file update the stripe webhook signing secret:
STRIPE_ENDPOINT_SECRET=<secret from stripe listen command>

# Start frontend and go through checkout process
npm run dev

# tail server logs to see enqueued results
go ./main
```

To test the actual workflow you will need to re-deploy the API with `stripe-secrets-test` env vars being injected into the helm manifests. Note:
You may also have to update the customer id in the database.

## Running the tests

`go test -v ./...`

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
