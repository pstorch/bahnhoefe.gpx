package github.pstorch.bahnhoefe.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Statistic {

    @JsonProperty
    private final int total;

    @JsonProperty
    private final int withPhoto;

    @JsonProperty
    private final int withoutPhoto;

    @JsonProperty
    private final int photographers;

    public Statistic(final int total, final int withPhoto, final int withoutPhoto, final int photographers) {
        this.total = total;
        this.withPhoto = withPhoto;
        this.withoutPhoto = withoutPhoto;
        this.photographers = photographers;
    }

    public int getTotal() {
        return total;
    }

    public int getWithPhoto() {
        return withPhoto;
    }

    public int getWithoutPhoto() {
        return withoutPhoto;
    }

    public int getPhotographers() {
        return photographers;
    }

}
