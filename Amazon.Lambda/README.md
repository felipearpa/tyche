# Amazon Lambda

Hosts the Tyche backend services (HTTP API, score and position computation, account management).

## Prerequisites

Install the .NET SDK: https://dotnet.microsoft.com/download

Install the Amazon.Lambda.Tools global tool:

```bash
dotnet tool install -g Amazon.Lambda.Tools
```

## Login

Install the AWS CLI: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html

The scripts need two profiles, one per AWS account, both in region `us-east-2`. Configure the `default` profile with an access key for the prod account (`567776770335`), and the `dev` profile with one for the dev account (`041269245665`):

```bash
aws configure
aws configure --profile dev
```

Each command asks for the access key id, secret access key, and default region; answer `us-east-2` for the region. Run the same command again to replace an expired or rotated key.

## Stages

The backend runs in two AWS accounts with identical stacks: `dev` (`041269245665`) and `prod` (`567776770335`). Resource names inside the stack (`TycheApi`, the tables, queues, topic, and SSM parameter) are the same in both; the account is the boundary.

Each stage has a settings file in `stages/`:

| File | `AWS_PROFILE` | `DEPLOY_BUCKET` | `POOL_MAINTENANCE_MAXIMUM_CONCURRENCY` |
| --- | --- | --- | --- |
| `stages/dev.sh` | `dev` | `tyche-api-deploy-dev` | `2` |
| `stages/prod.sh` | `default` | `tyche-api-deploy` | `16` |

The profiles are the ones set up in [Login](#login).

## Deploy

From this directory (`Amazon.Lambda/`):

```bash
./deploy-lambda.sh dev
./deploy-lambda.sh prod
```

The stage argument is required; without it the script prints `usage: ./deploy-lambda.sh dev|prod` and exits. The script loads `stages/<stage>.sh`, reads the `LatestStreamArn` of `Pool`, `PoolLayout`, and `MatchScoreIngestion` in that stage's account, runs the test suite, packages with `dotnet lambda package-ci` to the stage's deploy bucket, and deploys with `aws cloudformation deploy`, passing the stream ARNs and the maintenance concurrency as parameters.

- `dev` executes the deployment directly.
- `prod` creates the changeset without executing it, prints every resource change with its action and replacement flag, and asks `Execute this changeset on TycheApi in production? [y/N]`. Only `y` executes it; any other answer, or no terminal input, deletes the changeset and exits 1.

`Felipearpa.Tyche.AmazonLambda/src/aws-lambda-tools-defaults.json` still names the prod profile and the `tyche-api-deploy` bucket, so a bare `dotnet lambda deploy-serverless` targets production without the stream parameters or the confirmation. Deploy with `deploy-lambda.sh`.

### First prod deploy: avatar bucket rename

The avatar bucket is named `tyche-avatars-<accountId>` in both stages, so the first prod deploy of this template renames `tyche-avatars` to `tyche-avatars-567776770335`. CloudFormation can only delete the old bucket when it is empty, so empty it first (its contents are dropped):

```bash
aws s3 rm s3://tyche-avatars --recursive --profile default
./deploy-lambda.sh prod
```

The changeset must show exactly two replacements: `AvatarsBucket` and `AvatarsBucketPolicy` (the policy follows the bucket name). Any other replacement is a stop: answer `n` and investigate before deploying.

## Bootstrap a stage

`operations/bootstrap-stage.sh` creates everything a non-production stage needs, from the CLI. It refuses `prod` and refuses any profile that resolves to the production account.

```bash
./operations/bootstrap-stage.sh dev
```

In order, it:

1. Creates the stage's deploy bucket.
2. Creates the `Pool`, `PoolLayout`, `Account`, and `MatchScoreIngestion` tables with production's schema (`operations/pool-table.sh create-all`).
3. Copies the football-data token `/tyche/football-data/token` from prod SSM to the stage as a `SecureString`, without writing it to disk.
4. Deploys the stack with `deploy-lambda.sh <stage>`.
5. Requests the ACM certificate for `tyche-api-<stage>.felipearpa.com` in the stage account, writes its DNS validation CNAME in the `felipearpa.com` zone, and waits until it is issued.
6. Creates the API Gateway custom domain with that certificate and maps it to the stack's `AuthGatewayHttpApi` (`$default` stage).
7. Writes the alias A record for `tyche-api-<stage>.felipearpa.com` in the `felipearpa.com` zone, pointing at the custom domain.

Every step checks before it creates, so a failed run can be repeated and a complete run changes nothing but the stack deploy, which uploads a fresh package.

The `felipearpa.com` hosted zone (`Z09565921NSDP1O1BZLH3`) stays in the prod account, so the Route 53 writes in steps 5 and 7 use the prod profile from `stages/prod.sh`; every other step uses the stage's profile. The prod profile therefore needs `route53:ListResourceRecordSets`, `route53:ChangeResourceRecordSets`, and `route53:GetChange` on that zone, and `ssm:GetParameter` with decryption on the token. The script writes only the stage's two records and never touches `tyche-api.felipearpa.com`.

Dev answers at `https://tyche-api-dev.felipearpa.com`; prod keeps `https://tyche-api.felipearpa.com`, whose domain and certificate are managed by hand.

### Shared football-data quota

Both stages read the same football-data.org token, so match-score ingestion in dev and prod shares its limit of 10 requests per minute. Polling matches in dev takes requests away from prod.

## Run the API locally

`Felipearpa.Tyche.HttpApi` hosts the HTTP API on `http://localhost:5098` against a stage's real tables and avatar bucket. Choose the stage with a launch profile in `Felipearpa.Tyche.HttpApi/src/Properties/launchSettings.json`:

| Profile | `AWS__Profile` | `AVATAR_BUCKET_NAME` |
| --- | --- | --- |
| `dev` | `dev` | `tyche-avatars-041269245665` |
| `prod` | `default` | `tyche-avatars-567776770335` |

From `Felipearpa.Tyche.HttpApi/src/`:

```bash
dotnet run --launch-profile dev
dotnet run --launch-profile prod
```

Both profiles run with `ASPNETCORE_ENVIRONMENT=Development` and validate the caller's Firebase ID token from `tyche-588ce`. Plain `dotnet run` uses the first profile, `dev`. Without a launch profile (`dotnet run --no-launch-profile`, or running the built assembly) the host runs as Production on port 5000 against the `dev` profile from `appsettings.json`, with no avatar bucket, so the avatar upload-URL endpoint fails. Always pass `--launch-profile`. The `prod` profile reads and writes production data.

The `local` variants of the iOS and Android apps call the ngrok address in `iOS/Tyche/Tyche/AppConfigLocal.xcconfig` (`URL_BASE_PATH`) and `Android/gradle.properties` (`urlBasePathLocal`). Point that tunnel at port 5098 and those apps see whichever stage the running profile targets.

## Operations

Settlement, projection, migration, and recovery procedures are in [`operations/README.md`](operations/README.md); how the flow works is in [`docs/pool-settlement.md`](../docs/pool-settlement.md).

## Release notes

See [`CHANGELOG.md`](CHANGELOG.md).
