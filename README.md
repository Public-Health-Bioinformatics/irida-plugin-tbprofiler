[![Build Status](https://travis-ci.org/public-health-bioinformatics/irida-plugin-tbprofiler.svg?branch=master)](https://travis-ci.org/public-health-bioinformatics/irida-plugin-tbprofiler)
[![codecov](https://codecov.io/gh/public-health-bioinformatics/irida-plugin-tbprofiler/branch/master/graph/badge.svg)](https://codecov.io/gh/public-health-bioinformatics/irida-plugin-tbprofiler)
[![Current Release Version](https://img.shields.io/github/release/public-health-bioinformatics/irida-plugin-tbprofiler.svg)](https://github.com/public-health-bioinformatics/irida-plugin-tbprofiler/releases)

# IRIDA TBProfiler Pipeline Plugin

![galaxy-workflow-diagram.png][]

This project contains a pipeline implemented as a plugin for the [IRIDA][] bioinformatics analysis system. 
This can be used to determine the sequence type of specific transposable elements that may be present in bacterial isolates.

# Table of Contents

   * [IRIDA TBProfiler Pipeline Plugin](#irida-tbprofiler-pipeline-plugin)
   * [Installation](#installation)
      * [Installing Galaxy Dependencies](#installing-galaxy-dependencies)
      * [Installing to IRIDA](#installing-to-irida)
   * [Usage](#usage)
      * [Analysis Results](#analysis-results)
      * [Metadata Table](#metadata-table)
   * [Building](#building)
      * [Installing IRIDA to local Maven repository](#installing-irida-to-local-maven-repository)
      * [Building the plugin](#building-the-plugin)
   * [Dependencies](#dependencies)

# Installation

## Installing Galaxy Dependencies

In order to use this pipeline, you will also have to install the following Galaxy tools and data 
managers within your Galaxy instance. These can be found at:

| Name                       | Version          | Owner                          | Metadata Revision | Galaxy Toolshed Link                                                                          |
|----------------------------|------------------|------------------------------- |-------------------|-----------------------------------------------------------------------------------------------|
| fastp                      | `0.23.2+galaxy0` | `iuc`                          | 10 (2022-02-03)   | [fastp-10:65b93b623c77](https://toolshed.g2.bx.psu.edu/view/iuc/fastp/65b93b623c77) |
| fastp_json_to_tabular      | `0.1.0`          | `public-health-bioinformatics` |  0 (2022-03-10)   | [fastp_json_to_tabular-0:091a2fb2e7ad](https://toolshed.g2.bx.psu.edu/view/public-health-bioinformatics/fastp_json_to_tabular/091a2fb2e7ad) |
| tbprofiler                 | `4.1.1+galaxy0`  | `iuc`                          | 14 (2022-03-16)   | [tbprofiler-14:ac8250086ac3](https://toolshed.g2.bx.psu.edu/view/iuc/tbprofiler/ac8250086ac3) |
| tbprofiler_json_to_tabular | `0.1.0+galaxy2`  | `dfornika`                     |  5 (2022-10-24)   | [tbprofiler_json_to_tabular-5:90d6f1633abc](https://testtoolshed.g2.bx.psu.edu/view/dfornika/tbprofiler_json_to_tabular/90d6f1633abc) |

## Installing to IRIDA

Please download the provided `irida-plugin-tbprofiler-[version].jar` from the [releases][] page and copy to your 
`/etc/irida/plugins` directory.  Now you may start IRIDA and you should see the pipeline appear in your list of pipelines.

*Note:* This plugin requires you to be running IRIDA version >= `21.01`. Please see the [IRIDA][] documentation for more details.

# Usage

The plugin should now show up in the **Analyses > Pipelines** section of IRIDA.

![plugin-pipeline.png][]
![pipeline-parameters.png][]

## Analysis Results

You should be able to run a pipeline with this plugin and get analysis results. The results include a TBProfiler report 
listing detailed strain typing and drug resistance information, a JSON-format TBProfiler result file, a vcf file containing
variant information and a bam format alignment.

![plugin-results-1.png][]
![plugin-results-2.png][]

## Metadata Table

And, you should be able to save and view these results in the IRIDA metadata table. The following fields are written to
the IRIDA 'Line List':

| Field Name                       | Description                                                                                                                                                        |
|----------------------------------|-------------------------------------------------------------------|
| tbprofiler/percent_reads_mapped  | Percentage of reads mapped against the reference sequence (H37Rv) |
| tbprofiler/resistance_type       | Resistance type (MDR/XDR)                                         |
| tbprofiler/main_lineage          | Main lineage                                                      |
| tbprofiler/sub_lineage           | Sub-lineage                                                       |

![plugin-metadata.png][]

# Building

Building and packaging this code is accomplished using [Apache Maven][maven]. However, you will first need to install [IRIDA][] to your local Maven repository. The version of IRIDA you install will have to correspond to the version found in the `irida.version.compiletime` property in the [pom.xml][] file of this project. Right now, this is IRIDA version `19.01.3`.

## Installing IRIDA to local Maven repository

To install IRIDA to your local Maven repository please do the following:

1. Clone the IRIDA project

```bash
git clone https://github.com/phac-nml/irida.git
cd irida
```

2. Checkout appropriate version of IRIDA

```bash
git checkout 21.01
```

3. Install IRIDA to local repository

```bash
mvn clean install -DskipTests
```

## Building the plugin

Once you've installed IRIDA as a dependency, you can proceed to building this plugin. Please run the following commands:

```bash
cd irida-plugin-tbprofiler

mvn clean package
```

Once complete, you should end up with a file `target/irida-plugin-tbprofiler-<VERSION>.jar` which can be installed as a plugin to IRIDA.

# Dependencies

The following dependencies are required in order to make use of this plugin.

* [IRIDA][] >= 21.01
* [Java][] >= 11 and [Maven][maven] (for building)

[maven]: https://maven.apache.org/
[IRIDA]: http://irida.ca/
[Galaxy]: https://galaxyproject.org/
[Java]: https://www.java.com/
[irida-pipeline]: https://irida.corefacility.ca/documentation/developer/tools/pipelines/
[irida-pipeline-galaxy]: https://irida.corefacility.ca/documentation/developer/tools/pipelines/#galaxy-workflow-development
[irida-wf-ga2xml]: https://github.com/phac-nml/irida-wf-ga2xml
[pom.xml]: pom.xml
[workflows-dir]: src/main/resources/workflows
[workflow-structure]: src/main/resources/workflows/0.2.0/irida_workflow_structure.ga
[irida-plugin-java]: https://github.com/phac-nml/irida/tree/development/src/main/java/ca/corefacility/bioinformatics/irida/plugins/IridaPlugin.java
[irida-updater]: src/main/java/ca/corefacility/bioinformatics/irida/plugins/ExamplePluginUpdater.java
[irida-setup]: https://irida.corefacility.ca/documentation/administrator/index.html
[messages]: src/main/resources/workflows/0.2.0/messages_en.properties
[maven-min-pom]: https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#Minimal_POM
[pf4j-start]: https://pf4j.org/doc/getting-started.html
[plugin-results-1.png]: doc/images/plugin-results-1.png
[plugin-results-2.png]: doc/images/plugin-results-2.png
[plugin-pipeline.png]: doc/images/plugin-pipeline.png
[plugin-metadata.png]: doc/images/plugin-metadata.png
[pipeline-parameters.png]: doc/images/pipeline-parameters.png
[galaxy-workflow-diagram.png]: doc/images/galaxy-workflow-diagram.png
