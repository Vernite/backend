# Building Vernite backend

## Prerequisites:
- JDK >= 17
- Maven 3
- MySQL Server >= 8.0.26
- `application.properties` in the working directory
- `vernite-2022.private-key.der` in the working directory

### Java 17

To install Java 17 on Windows download it from [Oracle](https://www.oracle.com/java/technologies/downloads/#jdk17-windows).

To install on Debian-based distributions:
```console
$ sudo apt install openjdk-17-jdk-headless
```

### Maven 3

To install Maven 3 follow this [guide](https://maven.apache.org/install.html).

On Debian-based distros you can just:
```console
$ sudo apt install maven
```

### MySQL

MySQL can be downloaded from [dev.mysql.com](https://dev.mysql.com/downloads/mysql/). Create an empty databases named `vernite` and `vernite_test`.

### `application.properties`

Secrets are held in `application.properties` in the working directory. Create a file with the content:
```
spring.mail.password=...
spring.datasource.password=...
githubKey=...
maxmindPassword=...
staticFolder=...
slack.signingSecret=...
slack.clientId=...
slack.clientSecret=...
slack.app.level.token=...
github.client.secret=...
recaptcha.secret=...
```
- `spring.mail.password` - it's password to SMTP for sending emails. Other SMTP settings can be configured in `./src/main/resources/application.properties`.
- `spring.datasource.password` - password to connect to the database. The default username when connecting is `vernite` (host `localhost:3306`)
- `githubKey` - you need to create [GitHub App](https://docs.github.com/en/developers/apps/building-github-apps) and copy key
- `maxmindPassword` - it is used to check the user location on the session list screen. You can generate a license key on the official [MaxMind](https://www.maxmind.com/en/accounts/current/license-key) website.
- `staticFolder` - path to where static files will be stored
- `slack.signingSecret` - slack application signing secret obtained from: [Slack apps](https://api.slack.com/apps)
- `slack.clientId` - slack application client id obtained from: [Slack apps](https://api.slack.com/apps)
- `slack.clientSecret` - slack application client secret obtained from: [Slack apps](https://api.slack.com/apps)
- `slack.app.level.token` - slack application app level token obtained from: [Slack apps](https://api.slack.com/apps)
- `github.client.secret` - github app client secret obtained from [Github apps](https://github.com/settings/apps)
- `recaptcha.secret` - TODO: add description

### GitHub Application

To create a GitHub application you need a GitHub account.

Go to your GitHub account `setting` -> `developer settings` -> `new GitHub app`. \
In the setup and callback URL fields enter `YOUR_URL/pl-PL/github/` and turn on redirect on update in the `post installation` section. \
In the webhook URL field enter `YOUR_URL/api/webhook/github` and generate and enter the webhook secret to the field and `application.properties` file as `githubkey`. \
In the permissions section change:
- `Contents` - read and write
- `Discussions` - read and write
- `Issues` - read and write
- `Pull requests` - read and write

After the app has been created go to `edit` -> `permissions and events` and subscribe to: `Issues`, `Pull request` and `Push`.
From the `general` section go to `private keys` and `generate private key`. Convert the downloaded pem file to der format:
```bash
openssl x509 -in cert.pem -out cert.der -outform DER
```
Rename der file to `vernite-2022.private-key.der` and copy it to the working directory.

### Slack Application

To create a Slack application you need a Slack account.

Go to [link](https://api.slack.com/apps) and create new app.
In basic information tab create App-Level token and add it to `application.properties`.
In `Add features and functionality` configure `Event subscriptions`:
- Turn them on
- Set request url to `YOUR_URL/api/integration/slack/events`
- In section `Subscribe to events on behalf of users` select: `message.channels`, `message.groups`, `message.im`

In `OAuth & Permission` section add redirect url: `YOUR_URL/api/integration/slack/oauth_redirect`.
In `User Token Scopes` choose: `message.channels`, `message.groups`, `message.im`.

### ReCaptcha setup

TODO: add description

## Building

To build the backend and run all unit tests during building, execute:
```console
$ mvn clean package
```
If you want to skip unit tests, e.g. when you don't want to connect to MySQL during building:
```console
$ mvn clean package -DskipTests
```

## Generating Javadoc & JaCoCo

After build you can generate jacoco report and javadocs:
```console
$ mvn jacoco:report javadoc:javadoc
```
- JaCoCo report is available online at https://vernite.github.io/backend/jacoco/
- Javadocs: https://vernite.github.io/backend/apidocs/ \
  Currently, project does not support 3rd plugins, so javadocs are not needed, but this may change in the future.

## Running

```console
$ java -jar target/vernite-VERSION.jar
```

Replace `VERSION` with the current version, e.g. `0.0.1-SNAPSHOT`. After the first run, schema database will be automatically created.
