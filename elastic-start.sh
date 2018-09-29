#!/bin/bash
# creates and runs a local elastic with some testdata

docker rm -f elastic

docker run -d --net host -e "discovery.type=single-node" -e "xpack.security.transport.filter.allow=localhost" -e "xpack.security.transport.filter.deny=_all" -e "xpack.security.enabled=false" --name elastic docker.elastic.co/elasticsearch/elasticsearch:5.5.2

until $(curl --output /dev/null --silent --head --fail http://localhost:9200); do
    printf '.'
    sleep 5
done

function importDump {
	echo Import index $1
	elasticdump --input="elasticdata/$1-mapping.json" --output="http://localhost:9200/$1" --type=mapping
	elasticdump --input="elasticdata/$1.json" --output="http://localhost:9200/$1" --type=data
}

importDump bahnhoefe
importDump bahnhofsfotosde
importDump bahnhoefeat
importDump bahnhofsfotosat
importDump bahnhoefec2
importDump bahnhofsfotosch
importDump bahnhoefees
importDump bahnhofsfotoses
importDump bahnhoefefi
importDump bahnhofsfotosfi
importDump bahnhoefefr
importDump bahnhofsfotosfr
importDump bahnhoefeie
importDump bahnhofsfotosie
importDump bahnhoefein
importDump bahnhofsfotosin
importDump bahnhoefenl
importDump bahnhofsfotosnl
importDump bahnhoefeno
importDump bahnhofsfotosno
importDump bahnhoefepl
importDump bahnhofsfotospl
importDump bahnhoefeuk
importDump bahnhofsfotosuk
importDump bahnhoefebe
importDump bahnhofsfotosbe
importDump bahnhoefecz
importDump bahnhofsfotoscz
importDump bahnhoefemd
importDump bahnhofsfotosmd
importDump bahnhoefept
importDump bahnhofsfotospt


importDump fotografen

