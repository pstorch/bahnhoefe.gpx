[![Build Status](https://travis-ci.org/RailwayStations/RSAPI.svg?branch=master)](https://travis-ci.org/RailwayStations/RSAPI) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/f28660e9e90e4549871fe8c35fe7bd2d)](https://www.codacy.com/app/peter-storch/RSAPI?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=RailwayStations/RSAPI&amp;utm_campaign=Badge_Grade) [![Coverage Status](https://coveralls.io/repos/github/RailwayStations/RSAPI/badge.svg?branch=master)](https://coveralls.io/github/RailwayStations/RSAPI?branch=master) 

# Railway Stations API
Backend Service for the https://play.google.com/store/apps/details?id=de.bahnhoefe.deutschlands.bahnhofsfotos App of the [Bahnhofsfotos opendata Project](http://www.railway-stations.org/).
It started as a simple GPX Exporter to use the Waypoints in your favorite Map App or GPS Device.
Later it was enhanced to export a txt list of the Waypoints for the online Map at: http://www.alsgefahn.de/bfmap/bhfueb.html.
Then it returned the Waypoints as json for the Bahnhofsfotos App.
The latest addition are the trainstations from switzerland, via country code `ch`, see [Use](#use).

This API is hosted at https://api.railway-stations.org or at the Deutsche Bahn developer site: https://developer.deutschebahn.com/store/apis/list where you can also find an online and executable version of the swagger documentation.

## build
To build the project, you need Maven and Java 8.

Run:
```mvn clean install```

Release:
- `mvn release:prepare`
- `mvn release:perform -Darguments=-DaltDeploymentRepository=local::default::file:///tmp/maven`

## Docker
This project can be run as a Docker container. The docker image is automatically built via the above maven build command.

- run locally:
  ```docker run -it --net=host --rm -p 8080:8080 pstorch/rsapi```

- run on server:
  - Download the image from docker hub:
  ```docker pull pstorch/rsapi:<version>```
  
  - Configure environment variables. Place ```rsapi.env``` file in current directory:
  ```
    SLACK_MONITOR_URL=https://hooks.slack.com/services/...
    SLACK_VERIFICATION_TOKEN=...
    API_KEY=...
    SALT=...
    SMTP_HOST=...
    SMTP_USER=...
    SMTP_PASSWD=...
    SMTP_FROM=...
    SMTP_PORT=587
  ```

  - Run as background service:
  ```docker run -d -p 8080:8080 --net=host --restart always --name rsapi --env-file rsapi.env -v <photo-upload-dir>:/tmp/rsapi -v <photo-main-dir>:/tmp/railway-station-photos pstorch/rsapi:<version>```

  - Remove the (running) container:
  ```docker rm -f rsapi```
  
  - Check if it is running:
  ```docker ps```
  
  - Read the logs:
  ```docker logs rsapi```
  
  - Attach to container:
  ```docker attach --sig-proxy=false rsapi```
  
Ready to use images are published at https://hub.docker.com/r/pstorch/rsapi/

## Use
Point your browser to http://localhost:8080/{country}/stations, where `country` can be "de", "ch", "fi" or "uk"

With the following query parameter:
- `hasPhoto`: boolean, indicates if only trainstations with or without a photo should be selected
- `photographer`: (nick)name of a photographer, select only trainstations with photos from her
- `maxDistance`, `lat`, `lon`: select trainstations within a max distance of km of the given reference point
- `country`: select trainstations from a given country, this parameter is an alternative to the `{country}` path

A more detailed API documentation can be found in the [swagger](swagger.yaml) file or online at [developer.deutschebahn.com](https://developer.deutschebahn.com/store/apis/list).

### Examples
- all german trainstations: http://localhost:8080/de/stations
- german trainstations without photo: http://localhost:8080/de/stations?hasPhoto=false
- austrian trainsations from photographer @android_oma: http://localhost:8080/ch/stations?photographer=@android_oma
- german trainsations within 20km from FFM mainstation: http://localhost:8080/de/stations?maxDistance=20&lat=50.1060866&lon=8.6615762
- photographers: http://localhost:8080/photographers.txt
- german photographers: http://localhost:8080/de/photographers.txt
- statistic: http://localhost:8080/de/stats.txt

The default output format is json. But can easily be changed to GPX or TXT. Either set the `Accept` header to `text/plain` or `application/gpx+xml` or simply add the extension `.txt` or `.gpx` to the end of the URL.

Download the .gpx file and import it to your favourite Map application (e.g. Locus on Android).
