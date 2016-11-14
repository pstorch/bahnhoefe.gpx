[![Build Status](https://travis-ci.org/pstorch/bahnhoefe.gpx.svg?branch=master)](https://travis-ci.org/pstorch/bahnhoefe.gpx) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/e9be06b4e9944de1a24a37e3b26d3051)](https://www.codacy.com/app/peter-storch/bahnhoefe-gpx?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pstorch/bahnhoefe.gpx&amp;utm_campaign=Badge_Grade)

# bahnhoefe.gpx
Backend Service for the https://play.google.com/store/apps/details?id=de.bahnhoefe.deutschlands.bahnhofsfotos App of the [Bahnhofsfotos opendata Project](http://www.deutschlands-bahnhoefe.de/).
It started as a simple GPX Exporter to use the Waypoints in your favorite Map App or GPS Device.
Later it was enhanced to export a txt list of the Waypoints for the online Map at: http://www.alsgefahn.de/bfmap/bhfueb.html.
Then it returned the Waypoints as json for the Bahnhofsfotos App.
The latest addition are the trainstations from Austria, via country code `ch`, see [Use](#use).

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
Point your browser to http://localhost:8080/{country}/bahnhoefe, where `country` can be "de" or "ch"

With the following query parameter:
- `hasPhoto`: boolean, indicates if only trainstations with or without a photo should be selected
- `photographer`: (nick)name of a photographer, select only trainstations with photos from her
- `maxDistance`, `lat`, `lon`: select trainstations within a max distance of km of the given reference point

### Examples
- all german trainstations: http://localhost:8080/de/bahnhoefe
- german trainstations without photo: http://localhost:8080/de/bahnhoefe&hasPhoto=false
- austrian trainsations from photographer @android_oma: http://localhost:8080/ch/bahnhoefe&photographer=@android_oma
- german trainsations within 20km from FFM mainstation: http://localhost:8080/de/bahnhoefe&maxDistance=20&lat=50.1060866&lon=8.6615762

### Deprecated endpoints
- Export trainstations with Photo
  http://localhost:8080/{country}/bahnhoefe-withPhoto -> use: http://localhost:8080/{country}/bahnhoefe&hasPhoto=true
- Export trainstations without Photo
  http://localhost:8080/{country}/bahnhoefe-withoutPhoto -> use: http://localhost:8080/{country}/bahnhoefe&hasPhoto=false
- Export trainstations in `country` = `de`:
  http://localhost:8080/bahnhoefe -> use: http://localhost:8080/de/bahnhoefe
- Export trainstations in `country` = `de` with Photo
  http://localhost:8080/bahnhoefe-withPhoto -> use: http://localhost:8080/de/bahnhoefe&hasPhoto=true
- Export trainstations in `country` = `de` without Photo
  http://localhost:8080/bahnhoefe-withoutPhoto -> use: http://localhost:8080/de/bahnhoefe&hasPhoto=false

The default output format is json. But can easily be changed to GPX or TXT. Either set the `Accept` header to `text/plain` or `application/gpx+xml` or simply add the extension `.txt` or `.gpx` to the end of the URL.

Download the .gpx file and import it to your favourite Map application (e.g. Locus on Android).
