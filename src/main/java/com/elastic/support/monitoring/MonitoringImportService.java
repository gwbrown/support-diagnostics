package com.elastic.support.monitoring;

import com.elastic.support.ElasticClientService;
import com.elastic.support.config.Constants;
import com.elastic.support.config.ElasticClientInputs;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringImportService extends ElasticClientService {

    private Logger logger = LogManager.getLogger(MonitoringImportService.class);
    private static final String SCROLL_ID = "{ \"scroll_id\" : \"{{scrollId}}\" }";
    private RestClient client;
    private MonitoringExtractConfig monitoringExtractConfig;
    private MonitoringImportInputs monitoringImportInputs;
    private String tempDir = SystemProperties.userDir + SystemProperties.fileSeparator + Constants.MONITORING_DIR;

    public MonitoringImportService(MonitoringImportInputs inputs){
        this.monitoringImportInputs = inputs;
        Map configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
        monitoringExtractConfig = new MonitoringExtractConfig(configMap);
        client = createEsRestClient(monitoringExtractConfig, inputs);
    }

    void execImport(){

        try {
            // Create the temp directory - delete if first if it exists from a previous run
            logger.info("Creating temp directory: {}", tempDir);

            FileUtils.deleteDirectory(new File(tempDir));
            Files.createDirectories(Paths.get(tempDir));

            logger.error("Sucessfully created temp directory.");

            // Set up the log file manually since we're going to package it with the diagnostic.
            // It will go to wherever we have the temp dir set up.
            logger.info("Configuring log file.");
            createFileAppender(tempDir, "import.log");
            ArchiveUtils archiveUtils = new ArchiveUtils(new MonitoringImportProcessor(monitoringExtractConfig, monitoringImportInputs, client));
            archiveUtils.extractDiagnosticArchive(monitoringImportInputs.input);


        }catch (Throwable t){
            logger.log(SystemProperties.DIAG, "Error extracting archive or indexing results", t);
            logger.error("Cannot contiue processing. Exiting {}", Constants.CHECK_LOG);
        }
        finally {
            closeLogs();
            client.close();
        }
    }


}
