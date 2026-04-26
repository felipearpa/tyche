# Amazon Lambda

Hosts the Tyche backend services (HTTP API, score and position computation, account management).

## Prerequisites

Install the .NET SDK: https://dotnet.microsoft.com/download

Install the Amazon.Lambda.Tools global tool:

```bash
dotnet tool install -g Amazon.Lambda.Tools
```

## Login

Configure AWS credentials with the AWS CLI: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html

```bash
aws configure
```

To refresh credentials if your session has expired:

```bash
aws configure
```

## Deploy

From this directory (`Amazon.Lambda/`):

```bash
./deploy-lambda.sh
```

The script runs the test suite and then deploys via `dotnet lambda deploy-serverless`.

## Release notes

See [`CHANGELOG.md`](CHANGELOG.md).
