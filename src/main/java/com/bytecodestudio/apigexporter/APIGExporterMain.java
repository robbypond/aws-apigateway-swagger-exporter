package com.bytecodestudio.apigexporter;

import java.io.FileWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.beust.jcommander.JCommander;

public class APIGExporterMain {

    private static final String CMD_NAME = "aws-api-export";
    private static final Log LOG = LogFactory.getLog(APIGExporterMain.class);
    
    @com.beust.jcommander.Parameter(names = {"--api", "-a"}, description = "API ID to export")
    private String apiId;

    @com.beust.jcommander.Parameter(names = {"--format", "-f"}, description = "Swagger file format: yaml or json")
    private String format = "yaml";

    @com.beust.jcommander.Parameter(names = {"--profile", "-p"}, description = "AWS CLI profile to use")
    private String profile = "default";

    @com.beust.jcommander.Parameter(names = {"--output", "-o"}, description = "Output file, prints to console if no file specified")
    private String file;

    @com.beust.jcommander.Parameter(names = "--help", help = true)
    private boolean help;

    public static void main(String[] args) {
        APIGExporterMain main = new APIGExporterMain();
        JCommander jCommander = new JCommander(main, args);
        jCommander.setProgramName(CMD_NAME);
        main.execute(jCommander);
    }

    public void execute(JCommander jCommander) {
        if (help) {
            jCommander.usage();
            return;
        }

        if (!validateArgs()) {
            jCommander.usage();
            System.exit(1);
        }

        AwsConfig config = new AwsConfig(profile);
        try {
            config.load();
        } catch (Throwable t) {
            LOG.error("Could not load AWS configuration. Please run 'aws configure'");
            System.exit(1);
        }
        
        try {
    		APIGExporter exporter = new APIGExporter(
    				getCredentialsProvider(config.getProfile()), config.getRegion());
    		String result = exporter.export(apiId, format);
    		if (file != null) {
    			FileWriter w = new FileWriter(file);
    			w.write(result);
    			w.close();
    		} else {
    			LOG.info(result);
    		}
        } catch (Throwable t) {
            LOG.error("Error exporting API in Swagger format", t);
            System.exit(1);
        }
    }

    private boolean validateArgs() {
        if (apiId == null) {
            return false;
        }
        if (!"yaml".equals(format) && !"json".equals(format)) {
            LOG.error("Unsupported Swagger file format " + format);
            return false;
        }

        return true;
    }

    private static AWSCredentialsProvider getCredentialsProvider(String profile) throws Throwable {
        ProfileCredentialsProvider provider = new ProfileCredentialsProvider(profile);

        try {
            provider.getCredentials();
        } catch (Throwable t) {
            LOG.error("Could not load AWS configuration. Please run 'aws configure'");
            throw t;
        }

        return provider;
    }
}
