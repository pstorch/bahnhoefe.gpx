package org.railwaystations.api.db;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.api.model.Photo;

public interface PhotoDao {

    @SqlUpdate("insert into photos (countryCode, id, url, license, photographerId, createdAt) values (:countryCode, :id, :url, :license, :photographerId, :createdAt)")
    void insert(@BindBean final Photo photo);

}
