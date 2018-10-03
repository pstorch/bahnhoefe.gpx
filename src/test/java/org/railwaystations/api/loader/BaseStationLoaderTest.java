package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.railwaystations.api.model.Coordinates;

import java.io.IOException;


public class BaseStationLoaderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void getMandatoryAttribute() throws IOException {
        final JsonNode node = MAPPER.readTree("{ \"test\": \"value\" }");
        Assertions.assertEquals(StationLoader.getMandatoryAttribute(node, "test").asText(), "value");
    }

    @Test
    public void getMandatoryAttributeMissing() throws IOException {
        final JsonNode node = MAPPER.readTree("{ \"something\": \"value\" }");
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            StationLoader.getMandatoryAttribute(node, "test");
        });
    }

    @Test
    public void readCoordinates() throws IOException {
        final JsonNode node = MAPPER.readTree("{ \"geometry\":{\"type\":\"Point\",\"coordinates\":[-3.523202,50.731094]} }");
        final Coordinates coordinates = StationLoader.readCoordinates(node);
        Assertions.assertEquals(coordinates.getLat(), 50.731094, 0.000001);
        Assertions.assertEquals(coordinates.getLon(), -3.523202, 0.000001);
    }

    @Test
    public void readCoordinatesMissingGeometry() throws IOException {
        final JsonNode node = MAPPER.readTree("{ \"type\":\"Point\",\"coordinates\":[-3.523202,50.731094]} ");
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            StationLoader.readCoordinates(node);
        });
    }

    @Test
    public void readCoordinatesMissingCoordinates() throws IOException {
        final JsonNode node = MAPPER.readTree("{ \"geometry\":{\"type\":\"Point\" } }");
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            StationLoader.readCoordinates(node);
        });
    }

}
