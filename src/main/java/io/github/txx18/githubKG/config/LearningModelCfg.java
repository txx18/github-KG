package io.github.txx18.githubKG.config;

import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ShaneTang
 * @create 2021-06-09 10:59
 */
@Configuration
public class LearningModelCfg {

    Map<String, Double> modelSetMeanMap;

    Map<String, Double> modelSetStdMap;

    Map<String, Double> recommenderWeightMap;

    Double NOT_IN_RECO_LIST_SCORE = 0.0;

    @Value("${model.filePath}")
    String recommenderWeightModelFile;

    public LearningModelCfg() {
        modelSetMeanMap = new HashMap<>();
        //        1.173501	0.279589	0.157405	0.280117	117.251887
        modelSetMeanMap.put("language", 1.173501);
        modelSetMeanMap.put("task", 0.279589);
        modelSetMeanMap.put("method", 0.157405);
        modelSetMeanMap.put("dataset", 0.280117);
        modelSetMeanMap.put("repoDegree", 117.251887);
        //        1.298134	0.634492	0.541739	0.625491	197.926418
        modelSetStdMap = new HashMap<>();
        modelSetStdMap.put("language", 1.298134);
        modelSetStdMap.put("task", 0.634492);
        modelSetStdMap.put("method", 0.541739);
        modelSetStdMap.put("dataset", 0.625491);
        modelSetStdMap.put("repoDegree", 197.926418);
    }

    public Evaluator getEvaluator() throws JAXBException, IOException, SAXException {
        return new LoadingModelEvaluatorBuilder().load(new File(recommenderWeightModelFile)).build();
    }
}
