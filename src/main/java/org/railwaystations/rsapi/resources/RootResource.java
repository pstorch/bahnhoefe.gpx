package org.railwaystations.rsapi.resources;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootResource {

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE, value = "/")
    public String  get() {
        return "<html><head><title>RSAPI</title></head><body><h1>RSAPI</h1><p>Documentation: <a href=\"https://github.com/RailwayStations/RSAPI/blob/master/swagger.yaml\">swagger.yaml</a></body></html>";
    }

}
