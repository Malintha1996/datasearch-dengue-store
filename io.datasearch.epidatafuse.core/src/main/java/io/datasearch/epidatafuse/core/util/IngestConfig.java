package io.datasearch.epidatafuse.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for ingestion.
 */
public class IngestConfig {
    private static final String FEATURE_NAME_KEY = "feature_name";
    private static final String SOURCE_TYPE_KEY = "source_type";
    private static final String SOURCE_FORMAT_KEY = "source_format";
    private static final String DATA_SOURCES_KEY = "data_sources";
    private static final String TRANSFORMATIONS_KEY = "transformations";
    private static final String ATTRIBUTE_NAME_KEY = "attribute_name";
    private static final String TRANSFORMATION_KEY = "transformation";
    private String featureName;
    private String sourceType;
    private String sourceFormat;
    private Map<String, String> transformations;
    private List<String> dataSources;

    public IngestConfig(Map<String, Object> configurations) {
        this.featureName = (String) configurations.get(FEATURE_NAME_KEY);
        this.sourceType = (String) configurations.get(SOURCE_TYPE_KEY);
        this.sourceFormat = (String) configurations.get(SOURCE_FORMAT_KEY);
        this.transformations = new HashMap<>();
        for (Map<String, String> transformation : (List<Map<String, String>>) configurations.get(TRANSFORMATIONS_KEY)) {
            this.transformations.put(transformation.get(ATTRIBUTE_NAME_KEY),
                    transformation.get(TRANSFORMATION_KEY));
        }
        this.dataSources = (List<String>) configurations.get(DATA_SOURCES_KEY);
    }

    public String getFeatureName() {
        return featureName;
    }

    public Map<String, String> getTransformations() {
        return transformations;
    }

    public List<String> getDataSources() {
        return dataSources;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public String getSourceType() {
        return sourceType;
    }
}
