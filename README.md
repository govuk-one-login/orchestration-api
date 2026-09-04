# orchestration-api

This repo contains the backend code and infrastructure for the Orchestration component for [GOV.UK One Login](https://sign-in.service.gov.uk/).
The Orchestration component is responsible for maintaining the RP facing **Open ID Connect** API for [GOV.UK One Login](https://sign-in.service.gov.uk/) as well as directing users between different GOV.UK One Login components.

# Getting started

## Pre-commit hook

The repo has config set up for a custom pre-commit hook in `.pre-commit-config.yaml`.
Pre-commit checks include applying formatting, so after the script has run you may see files updated with formatting changes.

To implement the pre-commit hook, you will need to install pre-commit:

```shell script
brew install pre-commit
```

and then set up the hook by running

```shell script
pre-commit install
```

## Formatting:

This repo uses Spotless for its formatting. You run this by running the following command:

```shell
./gradlew spotlessApply
```

## Diagrams

High level sequence diagrams for the Orchestration component are located [here](../../docs/diagrams/orchestration) and outline the different flows supported by Orchestration.

## Infrastructure:

Our application infrastructure is defined in the [main cloudformation template](./template.yaml). We have some infrastructure which is deployed outside the application template, using the dev-platform [Stack Orchestration tool](https://github.com/govuk-one-login/devplatform-deploy/tree/main/stack-orchestration-tool). This exists in the ci/ directory, alongside its configuration.
See the [README](ci/stack-orchestration/README.md) for more information.

## Code

The Orchestration codebase is gradually being separated from the Authentication codebase. The Orchestration code is located in the following locations:

- `ipv-api`
- `oidc-api`
- `client-registry-api`
- `doc-checking-app-api`
- `sis-api`
- Any directory starting with `orchestration-*`

Each of these directories corresponds to separate modules with some shared code. A brief description of each provided below:

- IPV API: Manages the redirect and callback from the IPV component.
- OIDC API: Manages the OIDC interactions with Relying Parties and the callback from the Authentication component.
- Client Registry API: Exposes some API endpoints to allow interactions with the [SSE Admin Tool](https://github.com/govuk-one-login/onboarding-self-service-experience).
- Doc Checking App API: Manages the interactions between Orchestration and the Document Checking App service.
- SIS API: Manages interactions with the SIS component
- Identity Shared: Shared code for interacting with the Identity components of GOV.UK One Login.

## Testing:

### Unit tests:

To run all the unit tests in the project you can run the following command:

```shell
./gradlew --parallel test -x integration-tests:test -x delivery-receipts-integration-tests:test
```

However, it may be useful to run the tests in a specific module in the codebase. For example the following command runs the tests in `oidc-api`:

```shell
./gradlew oidc-api:test
```

Alternatively you can run a single test file:

```shell
./gradlew oidc-api:test --tests uk.gov.di.authentication.oidc.lambda.AuthCodeHandlerTest
```

### Integration tests:

To run all the integration tests in the project you can run the following command:

```shell
./gradlew integration-tests:test
```

### Acceptance Tests:

Acceptance tests for Orchestration are stored in the [orchestration-acceptance-tests repository.](https://github.com/govuk-one-login/orchestration-acceptance-tests)

## Deploying to dev:

We currently use a [workflow in GitHub Actions](../../.github/workflows/deploy-orch-dev.yml) to deploy Orchestration to the development environment for manual testing.

## Documentation:

If a lambda has specific documentation, it will live under `docs/<lambdaName>` for example the [Authorisation Handler](../../oidc-api/docs/AuthorisationHandler.md).
Most of our documentation can be found in confluence.

### Contract (Pact):

Contract tests verify that two services agree on the format of their API communication without needing to run them together. They use [Pact](https://docs.pact.io/) to define a shared contract between a **consumer** (the service making requests) and a **provider** (the service handling them).

Most of the contract tests in this repo are **consumer-side** Pact tests. They live in `contract` packages within each module (e.g. `frontend-api/src/test/java/.../contract/`).
For example, the frontend-api is the consumer — it makes outbound requests to external services (TICF CRI, Account Interventions, IPV, etc.).

#### How they work

1. Each test class defines the expected request/response interactions using `@Pact` methods
2. A Pact mock server starts locally and acts as the provider
3. The real handler code runs against the mock server
4. If the handler sends a request matching the pact definition and handles the response correctly, the test passes
5. A **pact file** (JSON) is generated in `<module>/build/pacts/`
6. On merge, the pact file is published to a Pact Broker
7. The provider team runs the pact file against their real service to verify they satisfy the contract

This means both sides can test independently. If either side drifts from the contract, their tests fail.

#### Running contract tests

Run all contract tests for a module:

```shell
./gradlew client-registry-api:pactConsumerTests
```

#### Running in IntelliJ

Contract tests are **excluded** from the standard `test` task, so clicking the play button won't work with the default Gradle test runner. Two options:

1. **Change IntelliJ's test runner** — Go to Settings → Build, Execution, Deployment → Build Tools → Gradle → change "Run tests using" to **IntelliJ IDEA**. The play button will then work directly.

2. **Create a Gradle run configuration** — Run → Edit Configurations → + → Gradle. Set the task to `client-registry-api:pactConsumerTests` (but this can only run all the tests unless you specify a configuration per test)

#### When to update contract tests

- **Adding a new field to an outbound request** — Add the field to the pact definition if it will be present in the JSON payload
- **Changing the shape of a request/response** — Update the relevant `@Pact` method
- **Adding a new external service interaction** — Create a new test class

#### Important notes

- Null fields stripped during serialization don't need to be in the pact definition.
- The provider team must verify against your updated pact before the change is safe to deploy to production. Ensure you communicate with the team that have the provider tests.
- Pact files are published automatically on merge via CI (requires `PACT_URL`, `PACT_USER`, `PACT_PASSWORD` env vars - see [contract-tests.yaml](.github/workflows/contract-tests.yml)).

# Useful links:

- OIDC specification: https://openid.net/specs/openid-connect-core-1_0.html
- Technical documentation for GOV.UK One Login: https://docs.sign-in.service.gov.uk/
