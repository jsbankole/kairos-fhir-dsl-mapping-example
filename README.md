# kairos-fhir-dsl-mapping-example

This project contains Groovy example scripts for the use of the FHIR custom export interface.

More infos to CentraXX can be found on the [Kairos Website](https://www.kairos.de/en/)

* To write or modify custom export scripts, it is necessary to have a very good understanding of the source and target data models to transform into
  each other. Therefore, it is very helpful to use the kairos-fhir-dsl library as a dependency, which contains a CentraXX JPA meta model as a source,
  and the FHIR R4 model as a target.
* This project uses [Maven](https://maven.apache.org/) for build management to download all necessary dependencies
  from [Maven Central](https://mvnrepository.com/repos/central) or the kairos-fhir-dsl library
  from [GitHub Packages](https://github.com/kairosmike/kairos-fhir-dsl-mapping-example/packages).
* Because GitHub does not allow to download packages without access token, use maven with the local settings.xml in this project. e.g.
  ```
  mvn install -s settings.xml
  ```   
* The kairos-fhir-dsl binaries before v.1.5.0 have not been published on a public maven repository yet, but can be downloaded in
  the [assets section of the corresponding tag](https://github.com/kairosmike/kairos-fhir-dsl-mapping-example/releases).
* The versioning of this example projects will be parallel to the kairos-fhir-dsl library, which
  follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
* All Groovy example scripts will contain a @since annotation that describes the first CentraXX version, that can interpret the respective script. The
  specified CentraXX version contains the necessary minimal version of the kairos-fhir-dsl library, CXX entity exporter, initializer and support for
  the ExportResourceMappingConfig.json.
* Further instructions can be found in the [German how-to](/CXX_FHIR_Custom_Export.pdf).
