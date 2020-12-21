package io.datasearch.epidatafuse.core.fusionpipeline.fuseengine;

import io.datasearch.epidatafuse.core.fusionpipeline.datastore.PipelineDataStore;
import io.datasearch.epidatafuse.core.fusionpipeline.datastore.query.QueryManager;
import io.datasearch.epidatafuse.core.fusionpipeline.model.configuration.GranularityRelationConfig;
import io.datasearch.epidatafuse.core.fusionpipeline.model.granularitymappingmethod.ContainMapper;
import io.datasearch.epidatafuse.core.fusionpipeline.model.granularitymappingmethod.DefaultMapper;
import io.datasearch.epidatafuse.core.fusionpipeline.model.granularitymappingmethod.IntersectMapper;
import io.datasearch.epidatafuse.core.fusionpipeline.model.granularitymappingmethod.NearestMapper;
import io.datasearch.epidatafuse.core.fusionpipeline.model.granularitymappingmethod.WithinRadiusMapper;
import io.datasearch.epidatafuse.core.fusionpipeline.model.granularityrelationmap.SpatialGranularityRelationMap;
import io.datasearch.epidatafuse.core.fusionpipeline.model.granularityrelationmap.TemporalGranularityMap;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/**
 * For granularity Mapping.
 */
public class GranularityRelationMapper {

    private PipelineDataStore dataStore;
    private QueryManager queryManager;
    private static final Logger logger = LoggerFactory.getLogger(GranularityRelationMapper.class);

    private ArrayList<Map<String, Object>> spatioTemporalGranularityConfigs;

    //private SimpleFeatureCollection targetSpatialGranules;

    public GranularityRelationMapper(PipelineDataStore dataStore) {
        this.dataStore = dataStore;
        this.queryManager = new QueryManager();
    }

    public SpatialGranularityRelationMap buildSpatialGranularityMap(GranularityRelationConfig config) {
        logger.info("Building spatial granularity map for " + config.getFeatureTypeName() + " feature..");
        SpatialGranularityRelationMap spatialMap;
        String spatialGranularity = config.getSpatialGranularity();
        String relationMappingMethod = config.getSpatialRelationMappingMethod();
        if (relationMappingMethod == null) {
            spatialMap = new SpatialGranularityRelationMap();
        } else {
            String targetSpatialGranularity = config.getTargetSpatialGranularity();
            String targetUUID = this.dataStore.getGranularitySchema(targetSpatialGranularity).getUuidAttributeName();
            String baseUUID = this.dataStore.getGranularitySchema(spatialGranularity).getUuidAttributeName();
            SimpleFeatureCollection targetSpatialGranules = this.getGranuleSet(targetSpatialGranularity);
            SimpleFeatureCollection baseSpatialGranuleSet = this.getGranuleSet(spatialGranularity);

            switch (relationMappingMethod) {
                case NearestMapper.MAPPER_NAME:
                    spatialMap = NearestMapper.buildNearestMap(targetSpatialGranules,
                            baseSpatialGranuleSet, config.getSpatialMappingArguments(), baseUUID, targetUUID);
                    break;
                case ContainMapper.MAPPER_NAME:
                    spatialMap = ContainMapper
                            .buildContainMap(targetSpatialGranules, baseSpatialGranuleSet, baseUUID, targetUUID);
                    break;
                case WithinRadiusMapper.MAPPER_NAME:
                    spatialMap = WithinRadiusMapper
                            .buildWithinRadiusMap(targetSpatialGranules, baseSpatialGranuleSet, baseUUID, targetUUID);
                    break;
                case IntersectMapper.MAPPER_NAME:
                    spatialMap = IntersectMapper
                            .buildIntersectMap(targetSpatialGranules, baseSpatialGranuleSet, baseUUID, targetUUID);
                    break;
                case DefaultMapper.MAPPER_NAME:
                    spatialMap = DefaultMapper
                            .buildDefaultMap(targetSpatialGranules, baseSpatialGranuleSet, baseUUID, targetUUID);
                    break;
                default:
                    spatialMap = new SpatialGranularityRelationMap();
            }
        }
        return spatialMap;
    }

    public SimpleFeatureCollection getGranuleSet(String granularityName) {
        try {
            Query query = new Query(granularityName);
            FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                    dataStore.getDataStore().getFeatureReader(query, Transaction.AUTO_COMMIT);
            ArrayList<SimpleFeature> featureList = new ArrayList<>();
            while (reader.hasNext()) {
                SimpleFeature feature = reader.next();
                featureList.add(feature);
            }
            reader.close();
            return DataUtilities.collection(featureList);
        } catch (Exception e) {
            logger.info(e.getMessage());
            return null;
        }
    }

    // todo:to implement
    public TemporalGranularityMap buildTemporalMap(GranularityRelationConfig granularityRelationConfig) {

        try {
            logger.info("temporal " + granularityRelationConfig.getFeatureTypeName());

            String baseTemporalGranularity = granularityRelationConfig.getTemporalGranularity();
            String targetTemporalGranularity = granularityRelationConfig.getTargetTemporalGranularity();
            String featureTypeName = granularityRelationConfig.getFeatureTypeName();
            String temporalRelationMappingMethod = granularityRelationConfig.getTemporalRelationMappingMethod();

            return new TemporalGranularityMap(
                    baseTemporalGranularity, targetTemporalGranularity, featureTypeName,
                    temporalRelationMappingMethod);
        } catch (Exception e) {
            return new TemporalGranularityMap(
                    "day", "week", "test",
                    "test");
        }
    }
}
