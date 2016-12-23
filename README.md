[![Build Status](https://travis-ci.org/RailwayStations/RSAPI.svg?branch=master)](https://travis-ci.org/RailwayStations/RSAPI.gpx) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/e9be06b4e9944de1a24a37e3b26d3051)](https://www.codacy.com/app/peter-storch/RSAPI?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=RailwayStations/RSAPI&amp;utm_campaign=Badge_Grade) [![Coverage Status](https://coveralls.io/repos/github/RailwayStations/RSAPI/badge.svg?branch=master)](https://coveralls.io/github/RailwayStations/RSAPI?branch=master)

# Railway Stations API
Backend Service for the https://play.google.com/store/apps/details?id=de.bahnhoefe.deutschlands.bahnhofsfotos App of the [Bahnhofsfotos opendata Project](http://www.deutschlands-bahnhoefe.de/).
It started as a simple GPX Exporter to use the Waypoints in your favorite Map App or GPS Device.
Later it was enhanced to export a txt list of the Waypoints for the online Map at: http://www.alsgefahn.de/bfmap/bhfueb.html.
Then it returned the Waypoints as json for the Bahnhofsfotos App.
The latest addition are the trainstations from switzerland, via country code `ch`, see [Use](#use).

## build
To build the project, you need Maven and Java 8.

Run:
```mvn clean install```

Release:
- `mvn release:prepare`
- `mvn release:perform`

## Docker
This project can be run as a Docker container. The docker image is automatically built via the above maven build command.

- run locally: 
  ```docker run -it --rm -p 8080:8080 pstorch/bahnhoefe-gpx```
  
- run on server: 
  ```docker run -d --restart=always -p 8080:8080 pstorch/bahnhoefe-gpx:<version>```

Ready to use images are published at https://hub.docker.com/r/pstorch/bahnhoefe-gpx/

## Use
Point your browser to http://localhost:8080/{country}/stations, where `country` can be "de" or "ch"

With the following query parameter:
- `hasPhoto`: boolean, indicates if only trainstations with or without a photo should be selected
- `photographer`: (nick)name of a photographer, select only trainstations with photos from her
- `maxDistance`, `lat`, `lon`: select trainstations within a max distance of km of the given reference point

### Examples
- all german trainstations: http://localhost:8080/de/stations
- german trainstations without photo: http://localhost:8080/de/stations&hasPhoto=false
- austrian trainsations from photographer @android_oma: http://localhost:8080/ch/stations&photographer=@android_oma
- german trainsations within 20km from FFM mainstation: http://localhost:8080/de/stations&maxDistance=20&lat=50.1060866&lon=8.6615762

### Deprecated endpoints
- Export trainstations
  http://localhost:8080/{country}/bahnhoefe -> use: http://localhost:8080/{country}/stations&hasPhoto=true
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
