# Botcity Framework Core

Core module of the BotCity RPA framework. Basically, this module provides features to recognize UI elemements and
interact with it using mouse and keyboard actions.

## Usage

### Local file without install into a local maven repository

```xml
<properties>
    <botcity.core.version>1.1.0</botcity.core.version>
</properties>
```

```xml
<dependencies>
    <dependency>
        <groupId>dev.botcity</groupId>
        <artifactId>botcity-framework-core</artifactId>
        <version>${botcity.core.version}</version>
        <type>jar</type>
        <scope>system</scope>
        <systemPath>${project.basedir}/lib/botcity-framework-core-${botcity.core.version}.jar</systemPath>
    </dependency>
</dependencies>
```

### Local file with install in a local Maven repository

```shell
mvn install:install-file  -Dfile=./lib/botcity-framework-core-1.1.0.jar -DgroupId=dev.botcity -DartifactId=botcity-framework-core -Dversion=1.1.0 -Dpackaging=jar
```

```xml
<properties>
    <botcity.core.version>1.1.0</botcity.core.version>
</properties>
```

```xml
<dependencies>
    <dependency>
        <groupId>dev.botcity</groupId>
        <artifactId>botcity-framework-core</artifactId>
        <version>${botcity.core.version}</version>
    </dependency>
</dependencies>
```
