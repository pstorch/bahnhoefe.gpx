package org.railwaystations.rsapi.model;

public class ProviderApp {

    private final String type;
    private final String name;
    private final String url;

    public ProviderApp(final String type, final String name, final String url) {
        this.type = type;
        this.name = name;
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
