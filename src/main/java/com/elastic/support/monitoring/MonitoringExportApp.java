package com.elastic.support.monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonitoringExportApp {

    private static final Logger logger = LogManager.getLogger(MonitoringExportApp.class);

    public static void main(String[] args) {

        MonitoringExportInputs monitoringExportInputs = new MonitoringExportInputs();
        monitoringExportInputs.parseInputs(args);

        if (!monitoringExportInputs.validate()) {
            logger.info("Exiting...");
            System.exit(0);
        }

        MonitoringExportService service = new MonitoringExportService(monitoringExportInputs);
        service.execExtract();
    }
}
