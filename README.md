[![Build Status](https://travis-ci.org/RailwayStations/RSAPI.svg?branch=master)](https://travis-ci.org/RailwayStations/RSAPI) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/b9882fcf1221409680f36afe2c85fcba)](https://www.codacy.com/gh/RailwayStations/RSAPI?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=RailwayStations/RSAPI&amp;utm_campaign=Badge_Grade) [![Coverage Status](https://coveralls.io/repos/github/RailwayStations/RSAPI/badge.svg?branch=master)](https://coveralls.io/github/RailwayStations/RSAPI?branch=master) 

# Railway Stations API
Backend Service for the https://map.railway-stations.org and the Mobile Apps for [Android](https://github.com/RailwayStations/RSAndroidApp) and [iOS](https://github.com/RailwayStations/Bahnhofsfotos) of the [Bahnhofsfotos opendata Project](https://github.com/RailwayStations).
It started as a simple GPX Exporter to use the Waypoints in your favorite Map App or GPS Device.
Later it was enhanced to export a plaintext list of the Waypoints for the online Map at: http://www.alsgefahn.de/bfmap/bhfueb.html.
Then it returned the Waypoints as json for the Android and iOS Apps as well as the Website.
Over time more and more countries have been added, see [Use](#use).

This API is hosted at https://api.railway-stations.org or at the Deutsche Bahn developer site: https://developer.deutschebahn.com/store/apis/list where you can also find an online and executable version of the swagger documentation.

## build
To build the project, you need Java 11.

Run on Unix like systems:
```./mvnw clean install```

Run on Windows:

```./mvnw.cmd clean install```

## Working Directory

The API uses `/var/rsapi` as working directory. This can be changed in the `config.yml` or via Docker volume, see below.

The following subdirectories are being used:

- `photos`
  - `<countryCode>`: photos for the country identified by the country code
- `inbox`: all uploads are collected here
  - `toprocess`: uploaded photos are sent to VsionAI for image processing
  - `processed`: processed photos from VsionAI
  - `done`: imported (unprocessed) photos
  - `<countryCode>/import`: old import directories for batch imports

## Docker
This project can be run as a Docker container. The docker image is automatically built via the above maven build command.

- build:
  ```docker build . -t railwaystations/rsapi:latest```

- Configure the ```config.yml``` file from current directory and put it into the rsapi work directory.
    
- Run interactively:
  ```docker run -it --net=host --rm -p 8080:8080 -v <work-dir>:/var/rsapi -e RSAPI_LB_CONTEXT=test --name rsapi railwaystations/rsapi```

- Run as background service:
  ```docker run -d -p 8080:8080 --net=host --restart always --name rsapi -v <work-dir>:/var/rsapi -e RSAPI_LB_CONTEXT=test railwaystations/rsapi:latest```

- Remove the (running) container:
  ```docker rm -f rsapi```
  
- Check if it is running:
  ```docker ps```
  
- Read the logs:
  ```docker logs -f rsapi```
  
- Attach to container:
  ```docker attach --sig-proxy=false rsapi```

- Restart (e.g. after config change):
  ```docker restart rsapi```
  
Ready to use images are published at https://hub.docker.com/repository/docker/railwaystations/rsapi

## Maria DB

For local testing purpose you can start a MariaDB in docker with `mariadb-start.sh`.

Enter mysql CLI:
`docker exec -it mariadb mysql -ursapi -prsapi rsapi --default-character-set=utf8mb4`

### Migrations

Before using the DB it needs to be populated with schema and data, also updates might be necessary from time to time.

When the project is build, you can start the DB migrations with: `java -jar target/rsapi-1.0.0-SNAPSHOT.jar db migrate -i test config.yml
`.

The context (`-i` parameter) can take the following values:
- prod: for production
- test: for local testing
- junit: for automated unit testing during maven build

## Use
Point your browser to `http://localhost:8080/{country}/stations`, where `country` can be "de", "ch", "fi", "uk", ...

With the following query parameter:
- `hasPhoto`: boolean, indicates if only trainstations with or without a photo should be selected
- `photographer`: (nick)name of a photographer, select only trainstations with photos from her
- `maxDistance`, `lat`, `lon`: select trainstations within a max distance of km of the given reference point
- `country`: select trainstations from a given country, this parameter is an alternative to the `{country}` path

A more detailed API documentation can be found in the [swagger](swagger.yaml) file or online at [developer.deutschebahn.com](https://developer.deutschebahn.com/store/apis/list).

### Examples
- all supported countries: https://api.railway-stations.org/countries
- all german trainstations: https://api.railway-stations.org/de/stations
- german trainstations without photo: https://api.railway-stations.org/de/stations?hasPhoto=false
- austrian trainsations from photographer @android_oma: https://api.railway-stations.org/ch/stations?photographer=@android_oma
- german trainsations within 20km from FFM mainstation: https://api.railway-stations.org/de/stations?maxDistance=20&lat=50.1060866&lon=8.6615762
- all photographers with count of photos: https://api.railway-stations.org/photographers.txt
- german photographers: https://api.railway-stations.org/de/photographers.txt
- statistic per country (de): https://api.railway-stations.org/de/stats.txt

The default output format is json. But can easily be changed to GPX or TXT. Either set the `Accept` header to `text/plain` or `application/gpx+xml` or simply add the extension `.txt` or `.gpx` to the end of the URL.

Download the .gpx file and import it to your favourite Map application (e.g. Locus on Android).

### Nextcloud and Docker

On our server we use two directories `toprocess` and `processed` from a Nextcloud shared directory. To use these two directories in the API running in Docker they need to be created as Docker volumes and mounted into the Docker instance.

- Install [fentas/davfs](https://github.com/fentas/docker-volume-davfs) plugin: `docker plugin install fentas/davfs`
- Create volumes:
  - `docker volume create -d fentas/davfs -o url=https://<user>:<password>@cloud.railway-stations.org/remote.php/webdav/VsionAI/roh -o uid=1000 -o gid=1000 toprocess`
  - `docker volume create -d fentas/davfs -o url=https://<user>:<password>@cloud.railway-stations.org/remote.php/webdav/VsionAI/verpixelt -o uid=1000 -o gid=1000 processed`
- Run Container with these volumes: `docker run -d --name rsapi -v /var/rsapi:/var/rsapi -v toprocess:/var/rsapi/inbox/toprocess -v processed:/var/rsapi/inbox/processed railwaystations/rsapi:latest`
