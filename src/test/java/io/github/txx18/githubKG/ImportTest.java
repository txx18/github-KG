package io.github.txx18.githubKG;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author ShaneTang
 * @create 2020-12-17 20:44
 */
public class ImportTest {

    @Test
    public void testReadJson() {
        // read的是JSONArray的话，会自动忽略嵌套的null
        Object jsonArray = JSONUtil.readJSON(new File("C:\\Disk_Data\\Small_Data\\Neo4j\\混合repo_list.json"),
                StandardCharsets.UTF_8);
        //        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
//        Object source_link = jsonObject.get("source_link");
    }

    @Test
    public void testReadJsonObject() {
        // 检验嵌套的null
        // Object接收，第一层有null，嵌套多层有null
        Object raw = JSONUtil.readJSON(new File("C:\\Disk_Data\\Small_Data\\Neo4j\\test.json"),
                StandardCharsets.UTF_8);
        JSONObject jsonObject = JSONUtil.parseObj(JSONUtil.toJsonStr(raw), true);
        JSONObject repository = ((JSONObject) jsonObject.getByPath("data.repository"));
        Object nameWithOwner = repository.getOrDefault("nameWithOwner", "");
        Object issues = repository.getOrDefault("issues", JSONUtil.createObj());
        return;
    }
}
