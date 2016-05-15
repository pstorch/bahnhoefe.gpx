# bahnhoefe.gpx
GPX Exporter for the [Bahnhofsfotos opendata Project](http://www.deutschlands-bahnhoefe.de/)

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
Point your browser to 
- Export all trainstations:
  http://localhost:8080/bahnhoefe
- Export all trainstations with Photo
  http://localhost:8080/bahnhoefe-withPhoto
- Export all trainstations without Photo
  http://localhost:8080/bahnhoefe-withoutPhoto

The default output format is json. But you can get GPX and TXT as well. Either set the `Accept` header to `text/plain` or `application/gpx+xml` or simply add the extension `.txt` or `.gpx` to the end of the URL.

Download the .gpx file and import it to your favourite Map application (e.g. Locus on Android).
