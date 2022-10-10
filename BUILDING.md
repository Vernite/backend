# Building Workflow backend

## Prerequisites:
- JDK >= 17
- Maven 3
- MySQL Server >= 8.0.26
- `application.properties` in the working directory
- `workflow-2022.private-key.der` in the working directory

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

MySQL can be downloaded from [dev.mysql.com](https://dev.mysql.com/downloads/mysql/). Create an empty databases named `workflow` and `workflow_test`.

### `application.properties`

Secrets are held in `application.properties` in the working directory. Create a file with the content:
```
spring.mail.password=...
spring.datasource.password=...
githubKey=...
maxmindPassword=...
```
- `spring.mail.password` - it's password to SMTP for sending emails. Other SMTP settings can be configured in `./src/main/resources/application.properties`.
- `spring.datasource.password` - password to connect to the database. The default username when connecting is `workflow` (host `localhost:3306`)
- `githubKey` - you need to create [GitHub App](https://docs.github.com/en/developers/apps/building-github-apps) and copy key
- `maxmindPassword` - it is used to check the user location on the session list screen. You can generate a license key on the official [MaxMind](https://www.maxmind.com/en/accounts/current/license-key) website.


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
- JaCoCo report is available online at https://sampandonte.github.io/workflow/jacoco/
- Javadocs: https://sampandonte.github.io/workflow/apidocs/ \
  Currently, project does not support 3rd plugins, so javadocs are not needed, but this may change in the future.

## Running

```console
$ java -jar target/workflow-VERSION.jar
```

Replace `VERSION` with the current version, e.g. `0.0.1-SNAPSHOT`. After the first run, schema database will be automatically created.