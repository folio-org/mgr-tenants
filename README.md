# mgr-tenants

Copyright (C) 2022-2022 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of contents

* [Introduction](#introduction)
* [Interaction with keycloak](#interaction-with-keycloak)
* [Environment Variables](#environment-variables)
* [Keycloak Integration](#keycloak-integration)


## Introduction
For now, `mgr-tenants` proxies requests to OKAPI's tenant API
When any operation will happen on tenant, it will take place on realm in keycloak,
also it will send a request to keycloak to retrieve a token and persist in cache for 60s for doing all the stuff related to realm

## Environment Variables

| Name                         | Default value        | Required | Description                                                                                                                                       |
|:-----------------------------|:---------------------|:--------:|:--------------------------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST                      | localhost            |  false   | Postgres hostname                                                                                                                                 |
| DB_PORT                      | 5432                 |  false   | Postgres port                                                                                                                                     |
| DB_USERNAME                  | postgres             |  false   | Postgres username                                                                                                                                 |
| DB_PASSWORD                  | postgres             |  false   | Postgres username password                                                                                                                        |
| DB_DATABASE                  | tenant_manager       |  false   | Postgres database name                                                                                                                            |
| OKAPI_INTEGRATION_ENABLED    | false                |  false   | Defines if Okapi integration is enabled or disabled                                                                                               |
| okapi.url                    | -                    |  false   | Okapi URL used to perform HTTP requests by `OkapiClient`.                                                                                         |
| CACHE_EXPIRATION_TTL         | 60s                  |  false   | ttl value for token to persist in cache                                                                                                           |
| SECURITY_ENABLED             | false                |  false   | Allows to enable/disable security. <br/>If true and KEYCLOAK_INTEGRATION_ENABLED is also true - the Keycloak will be used as a security provider. |
| KC_IMPERSONATION_CLIENT      | impersonation-client |  false   | Defined client in Keycloak, that has permissions to impersonate users.                                                                            |
| MOD_AUTHTOKEN_URL            | -                    |   true   | Mod-authtoken URL. Required if OKAPI_INTEGRATION_ENABLED is true and SECURITY_ENABLED is true and KC_INTEGRATION_ENABLED is false.                |
| SECRET_STORE_TYPE            | -                    |   true   | Secure storage type. Supported values: `Ephemeral`, `Aws_ssm`, `Vault`                                                                            |
| MAX_HTTP_REQUEST_HEADER_SIZE | 200KB                |   true   | Maximum size of the HTTP request header.                                                                                                          |

### Secure storage environment variables

#### AWS-SSM

Required when `SECRET_STORE_TYPE=Aws_ssm`

| Name                                          | Default value | Description                                                                                                                                                    |
|:----------------------------------------------|:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SECRET_STORE_AWS_SSM_REGION                   | -             | The AWS region to pass to the AWS SSM Client Builder. If not set, the AWS Default Region Provider Chain is used to determine which region to use.              |
| SECRET_STORE_AWS_SSM_USE_IAM                  | true          | If true, will rely on the current IAM role for authorization instead of explicitly providing AWS credentials (access_key/secret_key)                           |
| SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_ENDPOINT | -             | The HTTP endpoint to use for retrieving AWS credentials. This is ignored if useIAM is true                                                                     |
| SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_PATH     | -             | The path component of the credentials endpoint URI. This value is appended to the credentials endpoint to form the URI from which credentials can be obtained. |

#### Vault

Required when `SECRET_STORE_TYPE=Vault`

| Name                                    | Default value | Description                                                                         |
|:----------------------------------------|:--------------|:------------------------------------------------------------------------------------|
| SECRET_STORE_VAULT_TOKEN                | -             | token for accessing vault, may be a root token                                      |
| SECRET_STORE_VAULT_ADDRESS              | -             | the address of your vault                                                           |
| SECRET_STORE_VAULT_ENABLE_SSL           | false         | whether or not to use SSL                                                           |
| SECRET_STORE_VAULT_PEM_FILE_PATH        | -             | the path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding    |
| SECRET_STORE_VAULT_KEYSTORE_PASSWORD    | -             | the password used to access the JKS keystore (optional)                             |
| SECRET_STORE_VAULT_KEYSTORE_FILE_PATH   | -             | the path to a JKS keystore file containing a client cert and private key            |
| SECRET_STORE_VAULT_TRUSTSTORE_FILE_PATH | -             | the path to a JKS truststore file containing Vault server certs that can be trusted |

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
The feature is controlled by two env variables `SECURITY_ENABLED` and `KEYCLOAK_INTEGRATION_ENABLED`.

### Keycloak specific environment variables

| Name                        | Default value                |  Required   | Description                                                                                                                                             |
|:----------------------------|:-----------------------------|:-----------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------|
| KC_URL                      | http://keycloak:8080         |    false    | Keycloak URL used to perform HTTP requests.                                                                                                             |
| KC_INTEGRATION_ENABLED      | false                        |    false    | Defines if Keycloak integration is enabled or <br/>disabled.<br/>If it set to `false` - it will exclude all keycloak-related beans from spring context. |
| KC_IMPORT_ENABLED           | false                        |    false    | If true - at startup, register/create necessary records in keycloak from the internal module descriptor.                                                |
| KC_ADMIN_CLIENT_ID          | folio-backend-admin-client   |    false    | Keycloak admin client id. Used for register/create necessary records in keycloak from the internal module descriptor.                                   |
| KC_ADMIN_CLIENT_SECRET      | -                            | conditional | Keycloak admin secret. Required only if admin username/password are not set.                                                                            |
| KC_ADMIN_USERNAME           | -                            | conditional | Keycloak admin username. Required only if admin secret is not set.                                                                                      |
| KC_ADMIN_PASSWORD           | -                            | conditional | Keycloak admin password. Required only if admin secret is not set.                                                                                      |
| KC_ADMIN_GRANT_TYPE         | client_credentials           |    false    | Keycloak admin grant type. Should be set to `password` if username/password are used instead of client secret.                                          |
| KC_CLIENT_ID                | mgr-tenants                  |    false    | client id to be imported to Keycloak.                                                                                                                   |
| KC_CLIENT_SECRET            | -                            |    true     | client secret to be imported to Keycloak.                                                                                                               |
| KC_SERVICE_CLIENT_ID        | sidecar-module-access-client |    false    | Tenant specific client id for authenticating module-to-module requests.                                                                                 |
| KC_SERVICE_CLIENT_SECRET    | -                            |    true     | Tenant specific client secret for authenticating module-to-module requests.                                                                             |
| KC_LOGIN_CLIENT_SUFFIX      | -login-application           |    false    | Tenant specific client id suffix for login operations.                                                                                                  |
| KC_LOGIN_CLIENT_SECRET      | -                            |    true     | Tenant specific client secret for login operations.                                                                                                     |
| KC_CLIENT_SECRET_LENGTH     | 32                           |    false    | Configure a length to generate a client secret.                                                                                                         |
| KC_PASSWORD_RESET_CLIENT_ID | password-reset-client        |    false    | Tenant specific client id for password reset operations.                                                                                                |
| KC_PASSWORD_RESET_TOKEN_TTL | 86400                        |    false    | Password reset token Lifespan in seconds. Default value is 1 day, max value is 4 weeks.                                                                 |

### Interaction with Keycloak
The module before performing operations on Keycloak, sends auth request with grant type client_credential or password flow

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
