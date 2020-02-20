package org.railwaystations.api.db;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;

public interface PhotoDao {

    @SqlUpdate("insert into photos (countryCode, id, url, license, photographerId, createdAt) values (:stationKey.country, :stationKey.id, :url, :license, :photographer.id, :createdAt)")
    void insert(@BindBean final Photo photo);

    @SqlUpdate("update photos set url = :url, license = :license, photographerId = :photographer.id, createdAt = :createdAt where countryCode = :stationKey.country and id = :stationKey.id")
    void update(@BindBean final Photo photo);

    @SqlUpdate("delete from photos where countryCode = :country and id = :id")
    void delete(@BindBean final Station.Key key);

}
