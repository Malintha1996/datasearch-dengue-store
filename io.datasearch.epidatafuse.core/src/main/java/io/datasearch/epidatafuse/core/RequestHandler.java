package io.datasearch.epidatafuse.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.datasearch.epidatafuse.core.fusionpipeline.FusionPipeLineController;
import io.datasearch.epidatafuse.core.fusionpipeline.FusionPipeline;
import io.datasearch.epidatafuse.core.fusionpipeline.datastore.query.QueryManager;
import io.datasearch.epidatafuse.core.fusionpipeline.datastore.schema.AttributeUtil;
import io.datasearch.epidatafuse.core.fusionpipeline.model.aggregationmethod.AggregationUtil;
import io.datasearch.epidatafuse.core.fusionpipeline.model.granularitymappingmethod.MapperUtil;
import io.datasearch.epidatafuse.core.fusionpipeline.util.PipelineUtil;
import io.datasearch.epidatafuse.core.util.ConfigurationLoader;
import io.datasearch.epidatafuse.core.util.FeatureConfig;
import io.datasearch.epidatafuse.core.util.IngestConfig;
import io.datasearch.epidatafuse.core.util.IngestionConfig;
import io.datasearch.epidatafuse.core.util.OutputFrame;
import io.datasearch.epidatafuse.core.util.PipelineInfo;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Http request controller.
 */
@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class RequestHandler {

    private static final String CONFIGURATION_ERROR_MESSAGE = "Configuration Error";
    private static final String PIPELINE_NAME_EMPTY_ERROR_MESSAGE = "Pipeline name can not be empty!";
    private static final String SERVER_ERROR_MESSAGE = "Server Error!";
    private static final String PIPELINE_CREATION_SUCCESS_MESSAGE = "Pipeline created Successfully";
    private static final String PIPELINE_CREATION_ERROR_MESSAGE = "Error creating pipeline";
    private static final String ADD_NEW_FEATURE_ERROR_MESSAGE = "Could not add new feature";
    private static final String ADD_NEW_FEATURE_SUCCESS_MESSAGE = "New feature added successfully!";
    private static final String INGESTION_ERROR_MESSAGE = "Ingestion unsuccessful!";
    private static final String INGESTION_SUCCESS_MESSAGE = "Ingestion successful!";
    private static final String VARIABLE_TYPE_IDENTIFIER = "variable";
    private static final String GRANULARITY_TYPE_IDENTIFIER = "granularity";
    private static final String PIPELINE_NAMES_KEY = "pipeline_names";
    private static final String ATTRIBUTE_TYPES_KEY = "attribute_types";
    private static final String SPATIAL_GRANULARITIES_KEY = "spatialGranularities";
    private static final String TEMPORAL_GRANULARITIES_KEY = "temporalGranularities";
    private static final String SPATIAL_CONVERSION_METHODS = "spatialConversionMethods";
    private static final String SPATIAL_AGGREGATION_METHODS = "spatialAggregationMethods";
    private static final String TEMPORAL_CONVERSION_METHODS = "temporalConversionMethods";
    private static final String TEMPORAL_AGGREGATION_METHODS = "temporalAggregationMethods";
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private static ObjectMapper mapper = new ObjectMapper();

    @RequestMapping(value = "/createPipeline", method = RequestMethod.POST)
    public String createPipeline(@RequestBody Map<String, Object> payload) {
        Response response;
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                FusionPipeLineController.createFusionPipeLine(pipelineName);
                response =
                        new Response(true, false, PIPELINE_CREATION_SUCCESS_MESSAGE, new HashMap<>());
            } else {
                response =
                        new Response(false, true, PIPELINE_NAME_EMPTY_ERROR_MESSAGE, new HashMap<>());
            }
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                return SERVER_ERROR_MESSAGE;
            } else {
                return PIPELINE_CREATION_ERROR_MESSAGE;
            }
        }
    }

    @RequestMapping(value = "/addFeatureSchema", method = RequestMethod.POST)
    public String addFeatureSchema(@RequestBody Map<String, Object> payload) {
        Response response;
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                FeatureConfig featureConfig = new FeatureConfig(payload, VARIABLE_TYPE_IDENTIFIER);
                Boolean status = FusionPipeLineController.addNewFeature(pipelineName, featureConfig);
                if (status) {
                    response =
                            new Response(true, false, ADD_NEW_FEATURE_SUCCESS_MESSAGE, new HashMap<>());
                } else {
                    response =
                            new Response(false, true, ADD_NEW_FEATURE_ERROR_MESSAGE, new HashMap<>());
                }
            } else {
                response =
                        new Response(false, true, PIPELINE_NAME_EMPTY_ERROR_MESSAGE, new HashMap<>());
            }
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                return SERVER_ERROR_MESSAGE;
            } else {
                return ADD_NEW_FEATURE_ERROR_MESSAGE;
            }
        }
    }

    @RequestMapping(value = "/putFile", method = RequestMethod.PUT)
    public String putFile(@RequestParam("file") MultipartFile file) {
        return "success!";
    }

    @RequestMapping(value = "/addGranularity", method = RequestMethod.POST)
    public String addGranularity(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                FeatureConfig featureConfig = new FeatureConfig(payload, GRANULARITY_TYPE_IDENTIFIER);
                Boolean status = FusionPipeLineController.addNewFeature(pipelineName, featureConfig);
                if (status) {
                    IngestConfig ingestConfig = new IngestConfig((Map<String, Object>) payload.get("ingestion_config"));
                    Boolean statusIngest = FusionPipeLineController.ingestToGranularity(pipelineName, ingestConfig);
                    if (statusIngest) {
                        return "success!";
                    } else {
                        return "Unable to add new granularity";
                    }
                } else {
                    return "Unable to add new granularity";
                }
            } else {
                return "Pipeline name can not be empty!";
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    @RequestMapping("/ingestToFeature")
    public String ingestToFeature(@RequestBody Map<String, Object> payload) {
        Response response;
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                IngestConfig ingestConfig = new IngestConfig(payload);
                Boolean status = FusionPipeLineController.ingestToFeature(pipelineName, ingestConfig);
                if (status) {
                    response =
                            new Response(true, false, INGESTION_SUCCESS_MESSAGE, new HashMap<>());
                } else {
                    response =
                            new Response(false, true, INGESTION_ERROR_MESSAGE, new HashMap<>());
                }
            } else {
                response =
                        new Response(false, true, PIPELINE_NAME_EMPTY_ERROR_MESSAGE, new HashMap<>());
            }
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                return SERVER_ERROR_MESSAGE;
            } else {
                return ADD_NEW_FEATURE_ERROR_MESSAGE;
            }
        }
    }

    @RequestMapping("/ingestToGranularity")
    public String ingestToGranularity(@RequestBody Map<String, Object> payload) {
        Response response;
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                IngestConfig ingestConfig = new IngestConfig(payload);
                Boolean status = FusionPipeLineController.ingestToGranularity(pipelineName, ingestConfig);
                if (status) {
                    response =
                            new Response(true, false, INGESTION_SUCCESS_MESSAGE, new HashMap<>());
                } else {
                    response =
                            new Response(false, true, INGESTION_ERROR_MESSAGE, new HashMap<>());
                }
            } else {
                response =
                        new Response(false, true, PIPELINE_NAME_EMPTY_ERROR_MESSAGE, new HashMap<>());
            }
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                return SERVER_ERROR_MESSAGE;
            } else {
                return ADD_NEW_FEATURE_ERROR_MESSAGE;
            }
        }
    }


    @RequestMapping("/initPipeline")
    public String initPipeline(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                Boolean status = FusionPipeLineController.initPipeline(pipelineName);
                if (status) {
                    return "Success!";
                } else {
                    return "Pipeline" + pipelineName + "Not found";
                }
            } else {
                return "Pipeline name can not be empty!";
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    //todo: format properly
    @RequestMapping("/ingest")
    public String ingest() {
        try {
            String pipelineName = "dengue";
            IngestionConfig ingestionConfig =
                    new IngestionConfig(ConfigurationLoader.getIngestConfigurations());
            ServerContext.getPipeline(pipelineName).bulkIngest(ingestionConfig);
            return "Success";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    @RequestMapping("/granularityMap")
    public String granularityMap() {
        try {
            String pipelineName = "dengue";
            ServerContext.getPipeline(pipelineName).mapGranularityRelations();
            return "Success mapping";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    @RequestMapping("/convert")
    public String granularityConvert() {
        try {
            String pipelineName = "dengue";
            String featureType = "precipitation";
            //dengDIPipeLineMap.get(pipelineName).convertIntoRequiredGranule(featureType);

//            ServerContext.getPipeline(pipelineName).aggregate();
            ServerContext.getPipeline(pipelineName).invokeAggregate();
            return "Success conversion";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    @RequestMapping("/query")
    public String queryDengDIpipeline(String[] params) {
        try {
            String pipelineName = "dengue";
            QueryManager queryManager = new QueryManager();
            queryManager.runQueries(ServerContext.getPipeline(pipelineName));
            return "Success!";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

//    @RequestMapping("/removePipeline")
//    public String removePipeline(String[] params) {
//        try {
//            queryManager.runQueries(ServerContext.getPipeline(pipelineName).dispose());
//            return "Success!";
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//            return e.getMessage();
//        }
//    }

    @RequestMapping("/getPipelineInfo")
    public String getPipelineInfo(@RequestBody Map<String, Object> payload) {
        Response response;
        try {
            if (payload.get(PIPELINE_NAMES_KEY) != null) {
                List<PipelineInfo> pipelines = new ArrayList<>();
                List<String> pipelineNames = (ArrayList<String>) payload.get(PIPELINE_NAMES_KEY);
                for (String pipelineName : pipelineNames) {
                    pipelines.add(ServerContext.getPipeline(pipelineName).getInfo());
                }

                if (pipelines.size() > 0) {
                    Map<String, Object> responseData = new HashMap<>();
                    pipelines.forEach(pipelineInfo -> responseData.put(pipelineInfo.getPipelineName(), pipelineInfo));
                    response =
                            new Response(true, false, INGESTION_SUCCESS_MESSAGE, responseData);
                } else {
                    response =
                            new Response(false, true, INGESTION_ERROR_MESSAGE, new HashMap<>());
                }
            } else {
                response =
                        new Response(false, true, PIPELINE_NAME_EMPTY_ERROR_MESSAGE, new HashMap<>());
            }
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                return SERVER_ERROR_MESSAGE;
            } else {
                return ADD_NEW_FEATURE_ERROR_MESSAGE;
            }
        }
    }

    @RequestMapping("/getAttributeInfo")
    public String getAttributeInfo(@RequestBody Map<String, Object> payload) {
        Response response;
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                if (ServerContext.getPipeline(pipelineName) != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put(ATTRIBUTE_TYPES_KEY, AttributeUtil.getAttributeTypeList());
                    response =
                            new Response(true, false, INGESTION_SUCCESS_MESSAGE, responseData);
                } else {
                    response = new Response(true, false, INGESTION_SUCCESS_MESSAGE, new HashMap<>());
                }

            } else {
                response =
                        new Response(false, true, PIPELINE_NAME_EMPTY_ERROR_MESSAGE, new HashMap<>());
            }
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                return SERVER_ERROR_MESSAGE;
            } else {
                return SERVER_ERROR_MESSAGE;
            }
        }
    }

    @RequestMapping("/getGranularityInfo")
    public String getGranularityInfo(@RequestBody Map<String, Object> payload) {
        Response response;
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                FusionPipeline pipeline = ServerContext.getPipeline(pipelineName);
                if (pipeline != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    List<String> keys = new ArrayList<>();
                    keys.addAll(pipeline.getInfo().getGranularities().keySet());
                    responseData.put(SPATIAL_GRANULARITIES_KEY, keys);
                    responseData.put(TEMPORAL_GRANULARITIES_KEY, keys);
                    response =
                            new Response(true, false, INGESTION_SUCCESS_MESSAGE, responseData);
                } else {
                    response = new Response(true, false, INGESTION_SUCCESS_MESSAGE, new HashMap<>());
                }

            } else {
                response =
                        new Response(false, true, PIPELINE_NAME_EMPTY_ERROR_MESSAGE, new HashMap<>());
            }
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                return SERVER_ERROR_MESSAGE;
            } else {
                return SERVER_ERROR_MESSAGE;
            }
        }
    }


    @RequestMapping("/getConversionMethodInfo")
    public String getConversionMethodInfo(@RequestBody Map<String, Object> payload) {
        Response response;
        try {
            if (payload.get(PipelineUtil.PIPELINE_NAME_KEY) != null) {
                String pipelineName = (String) payload.get(PipelineUtil.PIPELINE_NAME_KEY);
                FusionPipeline pipeline = ServerContext.getPipeline(pipelineName);
                if (pipeline != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put(SPATIAL_CONVERSION_METHODS, MapperUtil.getMAPPERS());
                    responseData.put(TEMPORAL_CONVERSION_METHODS, MapperUtil.getMAPPERS());
                    responseData.put(SPATIAL_AGGREGATION_METHODS, AggregationUtil.getSpatialAggregators());
                    responseData.put(TEMPORAL_AGGREGATION_METHODS, AggregationUtil.getTemporalAggregators());
                    response =
                            new Response(true, false, INGESTION_SUCCESS_MESSAGE, responseData);
                } else {
                    response = new Response(true, false, INGESTION_SUCCESS_MESSAGE, new HashMap<>());
                }

            } else {
                response =
                        new Response(false, true, PIPELINE_NAME_EMPTY_ERROR_MESSAGE, new HashMap<>());
            }
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                return SERVER_ERROR_MESSAGE;
            } else {
                return SERVER_ERROR_MESSAGE;
            }
        }
    }

    @RequestMapping("/getdataframes")
    public String getdataframes() {
        String dir = "public/output/";
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();

        ArrayList<OutputFrame> outputData = new ArrayList<OutputFrame>();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                File each = listOfFiles[i];

                String[] headers = null;
                ArrayList<String[]> content = new ArrayList<String[]>();

                try {
                    FileReader reader = new FileReader(each);
                    BufferedReader bufferedReader = new BufferedReader(reader);

                    String line = null;
                    headers = bufferedReader.readLine().split(",");


                    while ((line = bufferedReader.readLine()) != null) {
                        String[] temp = line.split(",");
                        content.add(temp);
                    }
                    bufferedReader.close();
                    reader.close();

                } catch (Exception e) {
                    logger.info(e.getMessage());
                }
                String fileName = each.toString().substring(14);
                OutputFrame frame = new OutputFrame(fileName, headers, content);
                outputData.add(frame);
            }
        }

        String message = "Successfully responded";
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("data", outputData);

        Response response = new Response(true, false, message, data);

        ObjectMapper mapper = new ObjectMapper();
        String jsonString;

        try {
            jsonString = mapper.writeValueAsString(response);
        } catch (Exception e) {
            jsonString = "Server error!";
            logger.error("Could not write response", e.getMessage());
        }
        return jsonString;
    }

    @RequestMapping("/testinit")
    public String testInit() {
        String message = "Successfully responded";
        Map<String, Object> data = new HashMap<>();
        Response response = new Response(true, false, message, data);
        ObjectMapper mapper = new ObjectMapper();
        String jsonString;
        try {
            jsonString = mapper.writeValueAsString(response);
        } catch (Exception e) {
            jsonString = "Server error!";
            logger.error("Could not write response", e.getMessage());
        }
        return jsonString;
    }


//    @Deprecated
//    @RequestMapping(value = "/createPipelineFromConfigFiles")
//    public String createPipelineFromConfigFiles() {
//        try {
//            SchemaConfig schemaConfig = new SchemaConfig(ConfigurationLoader.getSchemaConfigurations());
//            if (schemaConfig.getPipelineName() != null) {
//                FusionPipeline pipeLine = FusionPipeLineController.createFusionPipeLine(schemaConfig);
//                ServerContext.addPipeline(schemaConfig.getPipelineName(), pipeLine);
//                return "Success!";
//            } else {
//                return "Invalid configuration!";
//            }
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//            return e.getMessage();
//        }
//    }
}
