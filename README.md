# mgr-tenants

Copyright (C) 2022-2022 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of contents

* [Introduction](#introduction)
* [Environment Variables](#environment-variables)
  * [Deprecated environment variables](#deprecated-environment-variables)
  * [SSL Configuration environment variables](#ssl-configuration-environment-variables)
  * [Secure storage environment variables](#secure-storage-environment-variables)
    * [AWS-SSM](#aws-ssm)
    * [Vault](#vault)
    * [Folio Secure Store Proxy (FSSP)](#folio-secure-store-proxy-fssp)
    * [Kafka](#kafka)
* [Keycloak Integration](#keycloak-integration)
* [Integration Testing](#integration-testing)

## Introduction

`mgr-tenants` owns the tenant lifecycle.
When any operation will happen on tenant, it will take place on realm in keycloak,
also it will send a request to keycloak to retrieve a token and persist in cache for 60s for doing all the stuff related
to realm

## Environment Variables

| Name                         | Default value                        | Required | Description                                                                                                                                                                                               |
|:-----------------------------|:-------------------------------------|:--------:|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST                      | localhost                            |  false   | Postgres hostname                                                                                                                                                                                         |
| DB_PORT                      | 5432                                 |  false   | Postgres port                                                                                                                                                                                             |
| DB_USERNAME                  | postgres                             |  false   | Postgres username                                                                                                                                                                                         |
| DB_PASSWORD                  | postgres                             |  false   | Postgres username password                                                                                                                                                                                |
| DB_DATABASE                  | tenant_manager                       |  false   | Postgres database name                                                                                                                                                                                    |
| MTE_URL                      | http://mgr-tenant-entitlements:8081  |  false   | Base URL for mgr-tenant-entitlements service. Used to check for active entitlements before tenant deletion.                                                                                               |
| MTE_TLS_ENABLED              | false                                |  false   | Enable TLS for communication with mgr-tenant-entitlements.                                                                                                                                                |
| MTE_TLS_TRUSTSTORE_PATH      | -                                    |  false   | Path to truststore for TLS communication with mgr-tenant-entitlements.                                                                                                                                    |
| MTE_TLS_TRUSTSTORE_PASSWORD  | -                                    |  false   | Password for the TLS truststore.                                                                                                                                                                          |
| MTE_TLS_TRUSTSTORE_TYPE      | -                                    |  false   | Type of the TLS truststore (e.g., JKS, PKCS12).                                                                                                                                                           |
| APIGW_ENABLED                | true                                 |  false   | Defines if API gateway integration is enabled or disabled.<br/>If it set to `false` - it will exclude all gateway-related beans from spring context.                                                      |
| APIGW_URL                    | -                                    |  false   | API gateway admin URL. Falls back to the `kong.url` system property (set by integration tests) when unset.                                                                                                |
| MODULE_URL                   | http://mgr-tenants:8081              |  false   | Module URL used for self-registration with the API gateway.                                                                                                                                               |
| APIGW_REGISTER_MODULE        | true                                 |  false   | Defines if this module must self-register with the API gateway.                                                                                                                                           |
| APIGW_CONNECT_TIMEOUT        | -                                    |  false   | Defines the timeout in milliseconds for establishing a connection from the API gateway to this module. If the value is not provided then gateway defaults are applied.                                    |
| APIGW_READ_TIMEOUT           | -                                    |  false   | Defines the timeout in milliseconds between two successive read operations for transmitting a request from the API gateway to this module. If the value is not provided then gateway defaults are applied.|
| APIGW_WRITE_TIMEOUT          | -                                    |  false   | Defines the timeout in milliseconds between two successive write operations for transmitting a request from the API gateway to this module. If the value is not provided then gateway defaults are applied.|
| APIGW_RETRIES                | -                                    |  false   | Defines the number of retries to execute upon failure to proxy. If the value is not provided then gateway defaults are applied.                                                                           |
| APIGW_TLS_ENABLED            | false                                |  false   | Enables TLS for communication with the API gateway admin API.                                                                                                                                             |
| APIGW_TLS_TRUSTSTORE_PATH    | -                                    |  false   | Path to the truststore for TLS communication with the API gateway.                                                                                                                                        |
| APIGW_TLS_TRUSTSTORE_PASSWORD| -                                    |  false   | Password for the TLS truststore.                                                                                                                                                                          |
| APIGW_TLS_TRUSTSTORE_TYPE    | -                                    |  false   | Type of the TLS truststore (e.g., JKS, PKCS12).                                                                                                                                                           |
| CACHE_EXPIRATION_TTL         | 60s                                  |  false   | ttl value for token to persist in cache                                                                                                                                                                   |
| SECURITY_ENABLED             | true                                 |  false   | Allows to enable/disable security. <br/>If true and KC_INTEGRATION_ENABLED is also true - the Keycloak will be used as a security provider.                                                               |
| KC_IMPERSONATION_CLIENT      | impersonation-client                 |  false   | Defined client in Keycloak, that has permissions to impersonate users.                                                                                                                                    |
| SECURE\_STORE\_ENV           | folio                                |  false   | First segment of the secure store key, for example `prod` or `test`. Defaults to `folio`. In Ramsons and Sunflower defaults to ENV with fall-back `folio`.                                                |
| SECRET_STORE_TYPE            | -                                    |   true   | Secure storage type. Supported values: `EPHEMERAL`, `AWS_SSM`, `VAULT`, `FSSP`                                                                                                                            |
| MAX_HTTP_REQUEST_HEADER_SIZE | 200KB                                |   true   | Maximum size of the HTTP request header.                                                                                                                                                                  |
| ROUTER_PATH_PREFIX           |                                      |  false   | Defines routes prefix to be added to the generated endpoints by OpenAPI generator (`/foo/entites` -> `{{prefix}}/foo/entities`). Required if load balancing group has format like `{{host}}/{{moduleId}}` |

### Deprecated environment variables

The following legacy Kong-specific variables, and the equivalent `application.kong.*` configuration property
keys, are still functional but log a deprecation warning at startup. They are planned for removal in the
Vetch release - migrate to the `APIGW_*` variables (`application.apigw.*` properties) listed above.

| Deprecated                           | Replacement                     |
|:--------------------------------------|:---------------------------------|
| KONG_INTEGRATION_ENABLED              | APIGW_ENABLED                    |
| KONG_ADMIN_URL                        | APIGW_URL                        |
| REGISTER_MODULE_IN_KONG               | APIGW_REGISTER_MODULE            |
| KONG_CONNECT_TIMEOUT                  | APIGW_CONNECT_TIMEOUT            |
| KONG_READ_TIMEOUT                     | APIGW_READ_TIMEOUT               |
| KONG_WRITE_TIMEOUT                    | APIGW_WRITE_TIMEOUT              |
| KONG_RETRIES                          | APIGW_RETRIES                    |
| KONG_TLS_ENABLED                      | APIGW_TLS_ENABLED                |
| KONG_TLS_TRUSTSTORE_PATH              | APIGW_TLS_TRUSTSTORE_PATH        |
| KONG_TLS_TRUSTSTORE_PASSWORD          | APIGW_TLS_TRUSTSTORE_PASSWORD    |
| KONG_TLS_TRUSTSTORE_TYPE              | APIGW_TLS_TRUSTSTORE_TYPE        |
| `application.kong.*` property keys    | `application.apigw.*`            |

### SSL Configuration environment variables

| Name                          | Default value | Required | Description                                                            |
|:------------------------------|:--------------|:--------:|:-----------------------------------------------------------------------|
| SERVER_PORT                   | 8081          |  false   | Server HTTP port. Should be specified manually in case of SSL enabled. |
| SERVER_SSL_ENABLED            | false         |  false   | Manage server's mode. If `true` then SSL will be enabled.              |
| SERVER_SSL_KEY_STORE          |               |  false   | Path to the keystore.  Mandatory if `SERVER_SSL_ENABLED` is `true`.    |
| SERVER_SSL_KEY_STORE_TYPE     | BCFKS         |  false   | Type of the keystore. By default `BCFKS` value is used.                |
| SERVER_SSL_KEY_STORE_PROVIDER | BCFIPS        |  false   | Provider of the keystore.                                              |
| SERVER_SSL_KEY_STORE_PASSWORD |               |  false   | Password for keystore.                                                 |
| SERVER_SSL_KEY_PASSWORD       |               |  false   | Password for key in keystore.                                          |

### Secure storage environment variables

#### AWS-SSM

Required when `SECRET_STORE_TYPE=AWS_SSM`

| Name                                          | Default value | Description                                                                                                                                                    |
|:----------------------------------------------|:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SECRET_STORE_AWS_SSM_REGION                   | -             | The AWS region to pass to the AWS SSM Client Builder. If not set, the AWS Default Region Provider Chain is used to determine which region to use.              |
| SECRET_STORE_AWS_SSM_USE_IAM                  | true          | If true, will rely on the current IAM role for authorization instead of explicitly providing AWS credentials (access_key/secret_key)                           |
| SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_ENDPOINT | -             | The HTTP endpoint to use for retrieving AWS credentials. This is ignored if useIAM is true                                                                     |
| SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_PATH     | -             | The path component of the credentials endpoint URI. This value is appended to the credentials endpoint to form the URI from which credentials can be obtained. |

#### Vault

Required when `SECRET_STORE_TYPE=VAULT`

| Name                                    | Default value | Description                                                                         |
|:----------------------------------------|:--------------|:------------------------------------------------------------------------------------|
| SECRET_STORE_VAULT_TOKEN                | -             | token for accessing vault, may be a root token                                      |
| SECRET_STORE_VAULT_ADDRESS              | -             | the address of your vault                                                           |
| SECRET_STORE_VAULT_ENABLE_SSL           | false         | whether or not to use SSL                                                           |
| SECRET_STORE_VAULT_PEM_FILE_PATH        | -             | the path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding    |
| SECRET_STORE_VAULT_KEYSTORE_PASSWORD    | -             | the password used to access the JKS keystore (optional)                             |
| SECRET_STORE_VAULT_KEYSTORE_FILE_PATH   | -             | the path to a JKS keystore file containing a client cert and private key            |
| SECRET_STORE_VAULT_TRUSTSTORE_FILE_PATH | -             | the path to a JKS truststore file containing Vault server certs that can be trusted |

#### Folio Secure Store Proxy (FSSP)

Required when `SECRET_STORE_TYPE=FSSP`

| Name                                   | Default value         | Description                                          |
|:---------------------------------------|:----------------------|:-----------------------------------------------------|
| SECRET_STORE_FSSP_ADDRESS              | -                     | The address (URL) of the FSSP service.               |
| SECRET_STORE_FSSP_SECRET_PATH          | secure-store/entries  | The path in FSSP where secrets are stored/retrieved. |
| SECRET_STORE_FSSP_ENABLE_SSL           | false                 | Whether to use SSL when connecting to FSSP.          |
| SECRET_STORE_FSSP_TRUSTSTORE_PATH      | -                     | Path to the truststore file for SSL connections.     |
| SECRET_STORE_FSSP_TRUSTSTORE_FILE_TYPE | -                     | The type of the truststore file (e.g., JKS, PKCS12). |
| SECRET_STORE_FSSP_TRUSTSTORE_PASSWORD  | -                     | The password for the truststore file.                |

#### Kafka

| Name                                         | Default value | Required | Description                                                                                                                                                                            |
|:---------------------------------------------|:--------------|:--------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ENV                                          | folio         |  false   | The logical name of the deployment (kafka topic prefix), must be unique across all environments using the same shared Kafka clusters, a-z (any case), 0-9, -, _ symbols only allowed   |
| KAFKA_HOST                                   | kafka         |  false   | Kafka broker hostname                                                                                                                                                                  |
| KAFKA_PORT                                   | 9092          |  false   | Kafka broker port                                                                                                                                                                      |
| KAFKA_SECURITY_PROTOCOL                      | PLAINTEXT     |  false   | Kafka security protocol used to communicate with brokers (SSL or PLAINTEXT)                                                                                                            |
| KAFKA_SSL_KEYSTORE_LOCATION                  | -             |  false   | The location of the Kafka key store file. This is optional for client and can be used for two-way authentication for client.                                                           |
| KAFKA_SSL_KEYSTORE_PASSWORD                  | -             |  false   | The store password for the Kafka key store file. This is optional for client and only needed if 'ssl.keystore.location' is configured.                                                 |
| KAFKA_SSL_TRUSTSTORE_LOCATION                | -             |  false   | The location of the Kafka trust store file.                                                                                                                                            |
| KAFKA_SSL_TRUSTSTORE_PASSWORD                | -             |  false   | The password for the Kafka trust store file. If a password is not set, trust store file configured will still be used, but integrity checking is disabled.                             |


## Keycloak Integration

### Import Keycloak data on startup

As startup, the application creates/updates necessary records in Keycloak from the internal module descriptor:

- Resource server
- Client - with credentials of `KC_CLIENT_ID`/`KC_CLIENT_SECRET`.
- Resources - mapped from descriptor routing entries.
- Permissions - mapped from `requiredPermissions` of routing entries.
- Roles - mapped from permission sets of descriptor.
- Policies - role policies as well as aggregate policies (specific for each resource).

### Keycloak Security

Keycloak can be used as a security provider. If enabled - application will delegate endpoint permissions evaluation to
Keycloak.
A valid Keycloak JWT token must be passed for accessing secured resources.
The feature is controlled by two env variables `SECURITY_ENABLED` and `KC_INTEGRATION_ENABLED`.

### Keycloak specific environment variables

| Name                              | Default value                |  Required   | Description                                                                                                                                             |
|:----------------------------------|:-----------------------------|:-----------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------|
| KC_URL                            | http://keycloak:8080         |    false    | Keycloak URL used to perform HTTP requests.                                                                                                             |
| KC_INTEGRATION_ENABLED            | true                         |    false    | Defines if Keycloak integration is enabled or <br/>disabled.<br/>If it set to `false` - it will exclude all keycloak-related beans from spring context. |
| KC_IMPORT_ENABLED                 | false                        |    false    | If true - at startup, register/create necessary records in keycloak from the internal module descriptor.                                                |
| KC_ADMIN_CLIENT_ID                | folio-backend-admin-client   |    false    | Keycloak admin client id. Used for register/create necessary records in keycloak from the internal module descriptor.                                   |
| KC_ADMIN_CLIENT_SECRET            | -                            | conditional | Keycloak admin secret. Required only if admin username/password are not set.                                                                            |
| KC_ADMIN_USERNAME                 | -                            | conditional | Keycloak admin username. Required only if admin secret is not set.                                                                                      |
| KC_ADMIN_PASSWORD                 | -                            | conditional | Keycloak admin password. Required only if admin secret is not set.                                                                                      |
| KC_ADMIN_GRANT_TYPE               | client_credentials           |    false    | Keycloak admin grant type. Should be set to `password` if username/password are used instead of client secret.                                          |
| KC_ADMIN_CONNECT_TIMEOUT          | 10s                           |    false    | Keycloak admin client connect timeout (Spring `Duration` syntax, e.g. `10s`, `500ms`).                                                                   |
| KC_ADMIN_READ_TIMEOUT             | 60s                           |    false    | Keycloak admin client read/socket timeout (Spring `Duration` syntax, e.g. `60s`, `2m`).                                                                  |
| KC_CLIENT_ID                      | mgr-tenants                  |    false    | client id to be imported to Keycloak.                                                                                                                   |
| KC_CLIENT_SECRET                  | -                            |    true     | client secret to be imported to Keycloak.                                                                                                               |
| KC_SERVICE_CLIENT_ID              | sidecar-module-access-client |    false    | Tenant specific client id for authenticating module-to-module requests.                                                                                 |
| KC_SERVICE_CLIENT_SECRET          | -                            |    true     | Tenant specific client secret for authenticating module-to-module requests.                                                                             |
| KC_LOGIN_CLIENT_SUFFIX            | -login-application           |    false    | Tenant specific client id suffix for login operations.                                                                                                  |
| KC_LOGIN_CLIENT_SECRET            | -                            |    true     | Tenant specific client secret for login operations.                                                                                                     |
| KC_CLIENT_SECRET_LENGTH           | 32                           |    false    | Configure a length to generate a client secret.                                                                                                         |
| KC_PASSWORD_RESET_CLIENT_ID       | password-reset-client        |    false    | Tenant specific client id for password reset operations.                                                                                                |
| KC_PASSWORD_RESET_TOKEN_TTL       | 86400                        |    false    | Password reset token Lifespan in seconds. Default value is 1 day, max value is 4 weeks.                                                                 |
| KC_CLIENT_TLS_ENABLED             | -                            |    false    | Enables TLS for keycloak clients.                                                                                                                       |
| KC_CLIENT_TLS_TRUSTSTORE_PATH     | -                            |    false    | Truststore file path for keycloak clients.                                                                                                              |
| KC_CLIENT_TLS_TRUSTSTORE_PASSWORD | -                            |    false    | Truststore password for keycloak clients.                                                                                                               |
| KC_CLIENT_TLS_TRUSTSTORE_TYPE     | -                            |    false    | Truststore file type for keycloak clients.                                                                                                              |
| KC_JWKS_BASE_URL                  |                              |    false    | Custom base URL for JWKS endpoint. If specified, will be used instead of issuer URL from token's iss claim (e.g., http://keycloak:8080).                |

When an admin client call exceeds `KC_ADMIN_CONNECT_TIMEOUT` or `KC_ADMIN_READ_TIMEOUT`, it fails with a
`jakarta.ws.rs.ProcessingException` (wrapping a `ConnectTimeoutException` / `SocketTimeoutException`),
releasing the caller thread so existing retry logic can observe it.


### Interaction with Keycloak

The module before performing operations on Keycloak, sends auth request with grant type client_credential or password
flow

### Authenticate with Keycloak (using client's credentials) and get back an access token

```shell
curl -XPOST \
-H "Content-Type: application/x-www-form-urlencoded" \
--data-urlencode "client_id=$clientId" \
--data-urlencode "client_secret=$clientSecret" \
--data-urlencode "grant_type=client_credentials" \
"$keycloakUrl/realms/$tenantId/protocol/openid-connect/token"
```

### Create a realm

```shell
curl -XPOST \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $token" \
-d "{"id":"05a2a258-462d-11ed-b878-0242ac120002","realm":"tenant2","enabled":"true"}" \
"$keycloakUrl/admin/realms"
```

### Delete a realm

```shell
curl -XDELETE \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $token" \
"$keycloakUrl/admin/realms/$tenantId"
```
## Integration Testing

Integration tests use Testcontainers for PostgreSQL and Kong. The following environment variables
let you redirect containers to a private registry or adjust startup behaviour without changing
source code.

| Environment variable                     | Default                         | Description                          |
|:-----------------------------------------|:--------------------------------|:-------------------------------------|
| `TESTCONTAINERS_POSTGRES_IMAGE`          | `postgres:16-alpine`            | PostgreSQL container image           |
| `TESTCONTAINERS_KONG_IMAGE`              | `folioci/folio-kong:latest`     | Kong container image                 |
| `TESTCONTAINERS_KONG_READINESS_TIMEOUT`  | `120`                           | Seconds to wait for Kong startup     |

## AI Documentation
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/folio-org/mgr-tenants)
