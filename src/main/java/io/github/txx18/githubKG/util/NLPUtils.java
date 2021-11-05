package io.github.txx18.githubKG.util;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;
import java.util.StringJoiner;

public class NLPUtils {

    private static final String annotators;

    private static final StanfordCoreNLP pipeline;

    static {
        annotators = "tokenize, ssplit, pos, lemma";
        Properties props = new Properties();
        props.setProperty("annotators", annotators);
        pipeline = new StanfordCoreNLP(props);
    }


    public static String getLemmaString(String label) {
        // 把特殊符号换成空格
        String tmp1 = label.toLowerCase().replaceAll("[\\pP\\pS\\pZ]", " ");
        CoreDocument document = pipeline.processToCoreDocument(tmp1);
        StringJoiner stringJoiner = new StringJoiner("-");
        for (CoreLabel tok : document.tokens()) {
            stringJoiner.add(tok.lemma());
        }
        return stringJoiner.toString();
    }

    public static void main(String[] args) {
        // set up pipeline properties
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        // build pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // create a document object
        CoreDocument document = pipeline.processToCoreDocument("sentence-embeddings");
        // display tokens
        for (CoreLabel tok : document.tokens()) {
            System.out.printf("%s\t%s%n", tok.word(), tok.lemma());
        }
    }

}
