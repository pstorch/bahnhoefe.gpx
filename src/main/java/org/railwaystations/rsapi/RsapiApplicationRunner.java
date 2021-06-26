package org.railwaystations.rsapi;

import org.railwaystations.rsapi.monitoring.Monitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RsapiApplicationRunner implements CommandLineRunner {

    @Autowired
    private Monitor monitor;

    @Autowired
    private StationsRepository repository;

    @Override
    public void run(final String... args) {
        monitor.sendMessage(repository.getCountryStatisticMessage());
    }

}
