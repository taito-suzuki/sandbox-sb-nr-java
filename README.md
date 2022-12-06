Install NewRelic jar

```shell
make newrelic
```

Set environment variables

```shell
# License Key
export NEW_RELIC_LICENSE_KEY=<key>

# Application name
export NEW_RELIC_APP_NAME=<app name>
```

Run application

```shell
./gradlew bootRun | tee app.log

while [ 1 ]; do for l in /p001 /p002 /p003 /p004 /p005 /p006 /p007; do curl http://localhost:8080${l}; sleep 1; done; done
```