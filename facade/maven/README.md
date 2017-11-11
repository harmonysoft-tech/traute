## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Usage](#3-usage)
* [4. Sample](#4-sample)

## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

It does not make sense to create a *Maven* plugin for simplifying the [*Javac* plugin](../../core/javac/README.md) usage like is done for [*Gradle*](../gradle/README.md) due to rather restrictive *Maven* facilities (more details on that [here](http://blog.harmonysoft.tech/2017/11/maven-plugins-does-it-worth-it.html)). So, we just provide information and examples on how to configure your *Maven* build to be able to use the target *Javac* plugin.

## 3. Usage

**Mandatory**

It's necessary to do two things:
* define a dependency to the [*Javac* plugin](../../core/javac/README.md) 
* configure *maven-compiler* to use the plugin

```xml
<project>

  <!-- ... -->

  <dependencies>
    <dependency>
      <groupId>tech.harmonysoft</groupId>
      <artifactId>traute-javac</artifactId>
      <version>1.0.4</version> <!-- use the latest available version here -->
      <scope>compile</scope>
    </dependency>
  </dependencies>
  
  <!-- ... -->
  
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.7.0</version>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
          <compilerArgs>
            <arg>-Xplugin:Traute</arg>
          </compilerArgs>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

**Optional**

It's possible to specify additional options [available in the *Javac* plugin](../../core/javac/README.md#7-settings):

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.7.0</version>
      <configuration>
        <source>1.8</source>
        <target>1.8</target>
        <compilerArgs>
          <arg>-Xplugin:Traute</arg>
          <arg>-Atraute.annotations.not.null=mycompany.util.NotNul</arg>
          <arg>-Atraute.instrumentations=parameter</arg>
          <arg>-Atraute.log.verbose=true</arg>
        </compilerArgs>
      </configuration>
    </plugin>
  </plugins>
</build>
```

## 4. Sample

A sample *Traute*-aware *Maven* project can be found [here](https://github.com/denis-zhdanov/traute/tree/master/facade/maven/sample).