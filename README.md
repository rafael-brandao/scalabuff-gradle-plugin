# Scalabuff Gradle Plugin

|      Scala 2.10      |      Scala 2.11      |
|----------------------|----------------------|
| [![Download for Scala 2.10](https://api.bintray.com/packages/rafael-brandao/maven/scalabuff-gradle-plugin_2.10/images/download.svg) ](https://bintray.com/rafael-brandao/maven/scalabuff-gradle-plugin_2.10/_latestVersion) | [![Download for Scala 2.11](https://api.bintray.com/packages/rafael-brandao/maven/scalabuff-gradle-plugin_2.11/images/download.svg) ](https://bintray.com/rafael-brandao/maven/scalabuff-gradle-plugin_2.11/_latestVersion) |

This is a Gradle plugin that acts as a wrapper of the [ScalaBuff](https://github.com/SandroGrzicic/ScalaBuff) tool.

It applies the `scala` plugin automatically and adds the tasks `compileProto` and `cleanProto` to Gradle. Normally there is no need to call these tasks, they are integrated in Gradle build lifecycle.


## Examples

A simple example script:

```groovy
// Using the new, incubating, plugin mechanism introduced in Gradle 2.1
plugins {
    id 'com.github.rafael-brandao.scalabuff_2.10' version '0.0.1'
}

scalabuff {
	// Plugin configuration goes here
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
        classpath 'com.github.rafael-brandao.gradle:scalabuff-gradle-plugin_2.10:0.0.1'
    }
}
apply plugin: 'com.github.rafael-brandao.scalabuff_2.10'

scalabuff {
	// Plugin configuration goes here
}
```

Full plugin configuration example (showing the default settings):

```groovy
plugins {
    id 'com.github.rafael-brandao.scalabuff_2.10' version '0.0.1'
}

scalabuff {
	sourceSets = ['src/protobuff']
	
	outputDir = file("${project.buildDir}/scalabuff")
	generatedSourcesDir = file("${scalabuff.outputDir}/generated-sources")
	generatedResourcesDir = file("${scalabuff.outputDir}/generated-resources")
	
	generateDescriptor = true
}
```


## Workflow

 1.  Scan the `${scalabuff.sourceSets}` directory for any `.proto` files;
 2. Convert any `.proto` file found to a valid Scala class using the [ScalaBuff](https://github.com/SandroGrzicic/ScalaBuff) tool;
 3. Store the generated sources in `${scalabuff.generated-sources}`  directory;
 4. Store the generated descriptors in `${scalabuff.generated-resources}`. For example, a file named `myFile.proto` will have it's descriptor file named as `myFile.proto.descriptor`.
 5. Automatically applies the `scala` plugin;
 6. Add compile time dependency to `protobuf-java` and `scalabuff-runtime` libraries;
 7. Add `${scalabuff.generated-sources}`  directory to the `${sourceSets.scala.main.srcDirs}` for compilation.


## Notes

 - Current `scalabuff` version is  `1.3.8` and `protobuf-java` version is `2.5.0`;
 - There is also a scala '2.11' version, but until [scalabuff issue 80](https://github.com/SandroGrzicic/ScalaBuff/issues/80) gets fixed, it generates code that will not compile. Scala `2.11` example:

```groovy
plugins {
	id 'com.github.rafael-brandao.scalabuff_2.11' version '0.0.1'
}

scalabuff {
	// config goes here...
}
```


# License
released under the [Mozilla Public License, Version 2.0](https://www.mozilla.org/MPL/2.0/)
