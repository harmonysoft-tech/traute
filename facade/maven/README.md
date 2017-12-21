## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Usage](#3-usage)
* [4. Sample](#4-sample)
* [5. Options](#5-options)
  * [5.1. NotNull Annotations](#51-notnull-annotations)
  * [5.2. NotNullByDefault Annotations](#52-notnullbydefault-annotations)
  * [5.3. Nullable Annotations](#53-nullable-annotations)
  * [5.4. Instrumentations Types](#54-instrumentation-types)
  * [5.5. Exception to Throw](#55-exception-to-throw)
  * [5.6. Exception Text](#56-exception-text)
  * [5.7. Logging](#57-logging)
  * [5.8. Log Location](#58-log-location)

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
      <version>1.1.1</version> <!-- use the latest available version here -->
      <scope>provided</scope><!-- make the jar eligible for compilation only -->
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

## 5. Options

### 5.1. NotNull Annotations  

*NotNull* annotations to use are defined through the *traute.annotations.not.null* option (multiple annotations might be specified separated by the colon (*:*)):  

```xml
<compilerArgs>
  <arg>-Xplugin:Traute</arg>
  <!-- Add null-checks only for method parameters/return values marked by @my.company.NotNull -->
  <arg>-Atraute.annotations.not.null=my.company.NotNull</arg>
</compilerArgs>
```  

More details on that can be found [here](../../core/javac/README.md#71-notnull-annotations).  

### 5.2. NotNullByDefault Annotations

*NotNullByDefault* annotations to use are defined through the *traute.annotations.not.null.by.default.* option prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69):  

```xml
<compilerArgs>
  <arg>-Xplugin:Traute</arg>
  <!-- Use my.custom.NotNullByDefault for method parameters -->
  <arg>-Atraute.annotations.not.null.by.default.parameter=my.custom.NotNullByDefault</arg>
</compilerArgs>
```  

More details on that can be found [here](../../core/javac/README.md#72-notnullbydefault-annotations).  

### 5.3. Nullable Annotations  

*Nullable* annotations to use are defined through the *traute.annotations.nullable* option (multiple annotations might be specified separated by *:*):  

```xml
<compilerArgs>
  <arg>-Xplugin:Traute</arg>
  <!-- Do not generate null-checks for method parameters/return values marked by @my.company.Nullable -->
  <arg>-Atraute.annotations.not.null=my.company.Nullable</arg>
</compilerArgs>
```

More details on that can be found [here](../../core/javac/README.md#73-nullable-annotations).  

### 5.4. Instrumentation Types  

Instrumentations types to use are defined through the *traute.instrumentations* option:  

```xml
<compilerArgs>
  <arg>-Xplugin:Traute</arg>
  <!-- Add checks only for method parameters (do not add check for return values) -->
  <arg>-Atraute.instrumentations=parameter</arg>
</compilerArgs>
```  

More details on that can be found [here](../../core/javac/README.md#74-instrumentation-types).  

### 5.5. Exception to Throw  

Custom exception class to throw from failed *null*-checks is defined through the *traute.exception.* option prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69):  

```xml
<compilerArgs>
  <arg>-Xplugin:Traute</arg>
  <!-- Throw a IllegalArgumentException when a null is given to a method parameter marked by @NotNull -->
  <arg>-Atraute.exception.parameter=IllegalArgumentException</arg>
  <!-- Throw a IllegalStateException when a null is given from a method marked by @NotNull -->
  <arg>-Atraute.exception.return=IllegalStateException</arg>
</compilerArgs>
```  

More details on that can be found [here](../../core/javac/README.md#75-exception-to-throw).  

### 5.6. Exception Text

Custom exception text to use in exceptions thrown from failed *null*-checks is defined through the *traute.failure.text.* option prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69):  

```xml
<compilerArgs>
  <arg>-Xplugin:Traute</arg>
  <!-- Use exception message like 'MyArg must not be null' for a method parameter names 'myArg' -->
  <arg>-Atraute.failure.text.parameter=$${capitalize(PARAMETER_NAME)} must not be null</arg>
</compilerArgs>
```  

More details on that can be found [here](../../core/javac/README.md#76-exception-text).  

### 5.7. Logging  

Logging verbosity is defined through the *traute.log.verbose* option:  

```xml
<compilerArgs>
  <arg>-Xplugin:Traute</arg>
  <!-- Use verbose logging -->
  <arg>-Atraute.log.verbose=true</arg>
</compilerArgs>
```  

More details on that can be found [here](../../core/javac/README.md#77-logging).  

### 5.8. Log Location  

Plugin's log file is defined through the *traute.log.file* option:  

```xml
<compilerArgs>
  <arg>-Xplugin:Traute</arg>
  <!-- Instruct the plugin to write its logs to the /home/me/traute.log -->
  <arg>-Atraute.log.file=/home/me/traute.log</arg>
</compilerArgs>
```  

More details on that can be found [here](../../core/javac/README.md#78-log-location).