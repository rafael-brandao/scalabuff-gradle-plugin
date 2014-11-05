# Scalabuff Gradle Plugin

|                      |
|----------------------|
|  [ ![Download](https://api.bintray.com/packages/rafael-brandao/maven/scalabuff-gradle-plugin/images/download.svg) ](https://bintray.com/rafael-brandao/maven/scalabuff-gradle-plugin/_latestVersion) | 

This is a Gradle plugin that acts as a wrapper of the [ScalaBuff](https://github.com/SandroGrzicic/ScalaBuff) tool.

It automatically applies the `scala` plugin and adds the tasks `scalabuffCompile` and `generateProtoDescriptors` to Gradle. Normally there is no need to call or configure these tasks, they are integrated in Gradle build lifecycle.

Scala library version is inferred after project evaluation, so the user must declare it as a `compile` dependency.


## Examples

A simple example script:

```groovy
// Using the new, incubating, plugin mechanism introduced in Gradle 2.1
plugins {
    id 'com.github.rafael-brandao.scalabuff' version '0.1.0'
}

apply plugin: 'com.github.rafael-brandao.scalabuff'

scalabuff {
    // Plugin configuration goes here, but it is optional
}

repositories {
    jcenter()
}

dependencies {
    compile 'org.scala-lang:scala-library:2.10.4'
}
```

or the traditional way:

```groovy
// Build script snippet for use in all Gradle versions
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.github.rafael-brandao.gradle:scalabuff-gradle-plugin:0.1.0'
    }
}
apply plugin: 'com.github.rafael-brandao.scalabuff'

scalabuff {
    // Plugin configuration goes here, but it is optional
}

repositories {
    jcenter()
}

dependencies {
    compile 'org.scala-lang:scala-library:2.10.4'
}
```


----------


Full plugin configuration example (showing the default settings):

```groovy
plugins {
    id 'com.github.rafael-brandao.scalabuff' version '0.1.0'
}

apply plugin: 'com.github.rafael-brandao.scalabuff'

repositories {
    jcenter()
}

dependencies {
    compile 'org.scala-lang:scala-library:2.10.4'
}

scalabuff {
	sourceSets {
        proto {
            srcDir = 'src/proto'
        }
    }

    outputDir             = file("${project.buildDir}/scalabuff") // read-only                             
    generatedSourcesDir   = file("${scalabuff.outputDir}/generated-sources") // read-only
    generatedResourcesDir = file("${scalabuff.outputDir}/generated-resources") // read-only
	
    failIfProtocNotDetected = false
    generateDescriptor      = true
    protocPath              = 'protoc'
}
```


## Workflow

 1.  Scan `${scalabuff.sourceSets}` directories for any `.proto` files;
 2. Convert any `.proto` file found to a valid Scala class using the [ScalaBuff](https://github.com/SandroGrzicic/ScalaBuff) tool;
 3. Store the generated sources in `${scalabuff.generated-sources}`  directory;
 4. Store the generated descriptors in `${scalabuff.generated-resources}`. For example, a file named `myFile.proto` will have it's descriptor file named as `myFile.proto.descriptor`.
 5. Automatically applies the `scala` plugin;
 6. Add compile time dependency to `scalabuff-runtime` library;
 7. Add `${scalabuff.generated-sources}`  directory to `${sourceSets.scala.main.srcDirs}` for compilation;
 8. Optional: If property  `failIfProtocNotDetected` is set to true, the plugin fails the build if it can't detect `protoc` command in the path;
 9. Optional: Set property `protocPath` to provide a configurable way to find `protoc` command. It defaults to `'protoc'`

## Notes

 - Current `scalabuff` version is  `1.3.8` ;
 - Until [scalabuff issue 80](https://github.com/SandroGrzicic/ScalaBuff/issues/80) gets fixed, scala 2.11 generated code will not compile.


## License
Released under the [Mozilla Public License, Version 2.0](https://www.mozilla.org/MPL/2.0/)
