package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderCh;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderDe;
import io.dropwizard.Configuration;

public class BahnhoefeServiceConfiguration extends Configuration {

    private BahnhoefeLoaderDe loaderDe = new BahnhoefeLoaderDe();
    private BahnhoefeLoaderCh loaderCh = new BahnhoefeLoaderCh();

    public BahnhoefeLoaderDe getLoaderDe() {
        return loaderDe;
    }

    public BahnhoefeLoaderCh getLoaderCh() {
        return loaderCh;
    }

    public BahnhoefeRepository getRepository() {
        return new BahnhoefeRepository(loaderDe, loaderCh);
    }

}
