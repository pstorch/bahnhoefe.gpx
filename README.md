# bahnhoefe.gpx
GPX Exporter for the [Bahnhofsfotos opendata Project](http://www.deutschlands-bahnhoefe.de/)

[![imagelayers](https://imagelayers.io/badge/pstorch/bahnhoefe-gpx:latest.svg)](https://imagelayers.io/?images=pstorch/bahnhoefe-gpx:latest)<br>
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/e9be06b4e9944de1a24a37e3b26d3051)](https://www.codacy.com/app/peter-storch/bahnhoefe-gpx?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pstorch/bahnhoefe.gpx&amp;utm_campaign=Badge_Grade)

## build
To build the project, you need Maven and Java 8.

Run:
```mvn clean install```

## Start
Run server:
```java -jar target/bahnhoefe.gpx-0.0.1-SNAPSHOT.jar server config.yml```

## Docker
This project can also be run as a Docker container.

- build the docker image: 
  ```docker build .```
- run image: 
  ```docker run -d -p 8080:8080 <image-id>```
  
A ready to use image is available at https://hub.docker.com/r/pstorch/bahnhoefe-gpx/

## Use
Point your browser to, where country can be "de" or "ch"
- Export all trainstations:
  http://localhost:8080/{country}/bahnhoefe
- Export all trainstations with Photo
  http://localhost:8080/{country}/bahnhoefe-withPhoto
- Export all trainstations without Photo
  http://localhost:8080/{country}/bahnhoefe-withoutPhoto

Default country is germany ("de"):
- Export all trainstations:
  http://localhost:8080/bahnhoefe
- Export all trainstations with Photo
  http://localhost:8080/bahnhoefe-withPhoto
- Export all trainstations without Photo
  http://localhost:8080/bahnhoefe-withoutPhoto

The default output format is json. But you can get GPX and TXT as well. Either set the `Accept` header to `text/plain` or `application/gpx+xml` or simply add the extension `.txt` or `.gpx` to the end of the URL.

Download the .gpx file and import it to your favourite Map application (e.g. Locus on Android).
