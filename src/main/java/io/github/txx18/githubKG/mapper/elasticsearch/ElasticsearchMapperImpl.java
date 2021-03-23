package io.github.txx18.githubKG.mapper.elasticsearch;

import io.github.txx18.githubKG.mapper.ElasticsearchMapper;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ShaneTang
 * @create 2021-03-17 10:12
 */
@Component
public class ElasticsearchMapperImpl implements ElasticsearchMapper {

    private final RestClient restClient;

    private ElasticsearchMapperImpl() {
        restClientBuilder.setFailureListener(new RestClient.FailureListener() {
            @Override
            public void onFailure(Node node) {
                System.out.println("出错了 -> " + node);
            }
        });
        this.restClient = restClientBuilder.build();
    }

    RestClientBuilder restClientBuilder = RestClient.builder(
            new HttpHost("localhost", 9200, "http")
//            new HttpHost("localhost", 9201, "http"),
//            new HttpHost("localhost", 9202, "http"),
//            new HttpHost("localhost", 9203, "http")
    );

    public void close() throws IOException {
        restClient.close();
    }

    public void getInfo() throws IOException {
        Request request = new Request("GET", "/_cluster/state");
        request.addParameter("pretty", "true");
        Response response = restClient.performRequest(request);
        System.out.println("-----------------------------------------------------------------------");
        System.out.println(response.getStatusLine());
        System.out.println("-----------------------------------------------------------------------");
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

    @Override
    public String importRepo(String jsonStr) throws IOException {
        Request request = new Request("POST", "/user_repo/_doc");
        request.setJsonEntity(jsonStr);
        Response response = restClient.performRequest(request);
        System.out.println("-----------------------------------------------------------------------");
        System.out.println(response.getStatusLine());
        System.out.println("-----------------------------------------------------------------------");
        System.out.println(EntityUtils.toString(response.getEntity()));
        return "ok";
    }
}
