# minecraft-stress-test

Stress test your Minecraft server with bots. Supports versions 1.20.6 through 1.21.11.

Bots log into the server in offline mode and fly around exploring the world. They automatically respawn when killed.

## Building

```shell
git clone https://github.com/PureGero/minecraft-stress-test.git
cd minecraft-stress-test
mvn package
```

## Running

Ensure the following values are set in your server.properties:

```properties
online-mode=false
allow-flight=true
```

Run the bot with:

```shell
java -jar target/minecraft-stress-test-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

## Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `bot.version` | `1.21` | Minecraft version (1.20.6-1.21.11) or protocol number |
| `bot.count` | `1` | Number of bots to connect |
| `bot.ip` | `127.0.0.1` | Server address |
| `bot.port` | `25565` | Server port |
| `bot.name` | `Bot` | Bot name prefix (becomes Bot1, Bot2, etc.) |
| `bot.login.delay.ms` | `100` | Delay between bot logins in milliseconds |
| `bot.max.execution.time` | `0` | Max runtime in seconds (0 = unlimited) |
| `bot.radius` | `1000` | Movement radius in blocks |
| `bot.speed` | `0.1` | Movement speed in blocks/tick |
| `bot.x` | `0` | Center X coordinate |
| `bot.z` | `0` | Center Z coordinate |
| `bot.viewdistance` | `2` | Client view distance |
| `bot.yaxis` | `true` | Enable vertical movement |
| `bot.logs` | `true` | Enable logging |
| `bot.resource.pack.response` | `3` | Resource pack response (3 = accepted) |

Example with parameters:

```shell
java -Dbot.version=1.21.4 -Dbot.count=100 -Dbot.ip=play.example.com -Dbot.max.execution.time=300 -jar target/minecraft-stress-test-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

## Commands

Type commands into the console to control bots at runtime:

| Command | Description |
|---------|-------------|
| `count <n>` | Set bot count (connects/disconnects as needed) |
| `speed <n>` | Set movement speed (default: 0.1) |
| `radius <n>` | Set movement radius (default: 1000) |

## Supported Versions

| Version | Protocol |
|---------|----------|
| 1.20.6 | 766 |
| 1.21, 1.21.1 | 767 |
| 1.21.2, 1.21.3 | 768 |
| 1.21.4 | 769 |
| 1.21.5 | 770 |
| 1.21.6 | 771 |
| 1.21.7, 1.21.8 | 772 |
| 1.21.9, 1.21.10 | 773 |
| 1.21.11 | 774 |
