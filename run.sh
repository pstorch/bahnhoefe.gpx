#!/bin/sh

#--show-module-resolution -Xlog:module=debug

java --module-path 'app/target/lib/'  --add-modules java.xml.bind -m rsapi/org.railwaystations.api.RsApiApp server app/config.yml
