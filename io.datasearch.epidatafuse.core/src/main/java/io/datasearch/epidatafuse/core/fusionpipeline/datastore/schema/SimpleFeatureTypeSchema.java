package io.datasearch.epidatafuse.core.fusionpipeline.datastore.schema;

import io.datasearch.epidatafuse.core.fusionpipeline.util.PipelineUtil;
import io.datasearch.epidatafuse.core.util.FeatureConfig;
import org.locationtech.geomesa.utils.interop.SimpleFeatureTypes;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.List;
import java.util.Map;

/**
 * Schema for attributes.
 */
public class SimpleFeatureTypeSchema implements DiseaseDataSchema {
    private SimpleFeatureType simpleFeatureType;
    private String simpleFeatureTypeName;
    private List<Map<String, String>> attributes;
    private String variableType;
    private String uuidAttributeName;
    private String shapeFileName;
    private String externalSourceAPIURL;

    //todo: Use string constants
    public SimpleFeatureTypeSchema(FeatureConfig featureConfig) {
        this.simpleFeatureTypeName = featureConfig.getFeatureName();
        this.variableType = featureConfig.getFeatureType();
        this.attributes = featureConfig.getAttributes();
        if (FeatureConfig.GRANULARITY_TYPE_IDENTIFIER.equals(featureConfig.getFeatureType())) {
            this.uuidAttributeName = featureConfig.getUuidAttributeName();
            this.shapeFileName = featureConfig.getFileName();
        }
        buildSchema(this.attributes, featureConfig.getFeatureType());
    }

    @Override
    public String getSimpleFeatureTypeName() {
        return this.simpleFeatureTypeName;
    }

    public SimpleFeatureType getSimpleFeatureType() {
        return simpleFeatureType;
    }

    public List<Map<String, String>> getAttributes() {
        return this.attributes;
    }

    public String getVariableType() {
        return variableType;
    }

    public String getUuidAttributeName() {
        return uuidAttributeName;
    }

    public String getShapeFileName() {
        return shapeFileName;
    }

    public void setExternalSourceAPIURL(String externalSourceAPIURL) {
        this.externalSourceAPIURL = externalSourceAPIURL;
    }

    public String getExternalSourceAPIURL() {
        return externalSourceAPIURL;
    }

    /**
     * Create a SimpleFeatureType in hbase datastore building a schema from given attributes.
     *
     * @param attributes
     */
    public void buildSchema(List<Map<String, String>> attributes, String featureType) {
        StringBuilder featureAttributes = new StringBuilder();
        for (int i = 0; i < attributes.size(); i++) {
            String attributeName = attributes.get(i).get(PipelineUtil.ATTRIBUTE_NAME_KEY);
            String attributeType = attributes.get(i).get(PipelineUtil.ATTRIBUTE_TYPE_KEY);
            Boolean indexed = Boolean.parseBoolean(attributes.get(i).get(PipelineUtil.INDEXED_KEY));
            //todo: throw error if attribute type is not a defined one
            if (FeatureConfig.GRANULARITY_TYPE_IDENTIFIER.equals(featureType) &&
                    attributeName.equals(uuidAttributeName)) {
                featureAttributes.append(AttributeUtil.getAttribute(attributeName, true, attributeType));
            } else {
                featureAttributes.append(AttributeUtil.getAttribute(attributeName, indexed, attributeType));
            }
            if (i != attributes.size() - 1) {
                featureAttributes.append(",");
            }
        }
        if (FeatureConfig.VARIABLE_TYPE_IDENTIFIER.equals(featureType)) {
            featureAttributes.append(AttributeUtil.getGranularityAttributes());
            attributes.add(AttributeUtil.getSpatialGranuleAttribute());
            attributes.add(AttributeUtil.getTemporalGranuleAttribute());
        }
        this.simpleFeatureType = SimpleFeatureTypes.createType(this.simpleFeatureTypeName,
                featureAttributes.toString());
    }
}
