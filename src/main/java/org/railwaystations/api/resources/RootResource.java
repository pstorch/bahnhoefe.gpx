package org.railwaystations.api.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class RootResource {

    @GET
    @Produces({MediaType.TEXT_HTML})
    public String  get() {
        return "<html><head><title>RSAPI</title></head><body><h1>RSAPI</h1><p>Documentation: <a href=\"https://github.com/RailwayStations/RSAPI/blob/master/swagger.yaml\">swagger.yaml</a></body></html>";
    }

}
