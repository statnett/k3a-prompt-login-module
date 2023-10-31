# prompt-login-module

A JAAS `LoginModule` intended for use with Kafka clients. When using
this module, it is not necessary to embed/store the password in the
config. Instead, the user will be prompted for the password (and
optionally, the username, if not present in the config).

## How to use it

Make the jar file available to the Kafka tools by installing it in the
shared jar directory of the tools. This will typically be a
`share/java/kafka` sub-directory under the base directory of the tools
installation. If tools are installed in `/opt/kafka`, this command
will install the jar:

```shell
KAFKA_BASEDIR=/opt/kafka
PROMPT_LOGIN_MODULE_VERSION=1.1.0
curl -sSfo ${KAFKA_BASEDIR}/share/java/kafka/prompt-login-module.jar \
      https://github.com/statnett/k3a-prompt-login-module/releases/download/v${PROMPT_LOGIN_MODULE_VERSION}/k3a-prompt-login-module-${PROMPT_LOGIN_MODULE_VERSION}.jar
```

The latest version may be found [on GitHub](https://github.com/statnett/k3a-prompt-login-module).

Reference the `io.statnett.k3a.authz.PromptLoginModule` class in
the JAAS section of your Kafka properties file:

```properties
sasl.mechanism=PLAIN
security.protocol=SASL_SSL
sasl.jaas.config=io.statnett.k3a.authz.PromptLoginModule required \
  username="my.user.name";
```

Run a Kafka command, referencing the Kafka properties, and note how it
now asks for a password from the console (If the JAAS config lacks a
username, it will be prompted for too):

```shell
$ kafka-topics --bootstrap-server broker:9092 --command-config kafka.properties --list
Password:
__consumer_offsets
__transaction_state
app_strm_demo_topic
  :
```

If input or output is redirected, the password will not be read from
the console, but instead a dialog window will open asking for the
credentials.
