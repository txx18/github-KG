package io.github.txx18.githubKG;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author ShaneTang
 * @create 2020-12-18 20:14
 */
public class DataTest {

    @Test
    public void testPaperTitle() {
        String filePath1 = "C:\\Disk_Dev\\Repository\\github-KG\\github-KG-python\\tx_data\\resource\\paperswithcode" +
                "\\methods.json";
        String filePath2 = "C:\\Disk_Dev\\Repository\\github-KG\\github-KG-python\\tx_data\\resource\\paperswithcode" +
                "\\papers-with-abstracts.json";
        JSONArray jsonArray1 = (JSONArray) JSONUtil.readJSON(new File(filePath1), StandardCharsets.UTF_8);
        JSONArray jsonArray2 = (JSONArray) JSONUtil.readJSON(new File(filePath2), StandardCharsets.UTF_8);
        HashMap<String, Object> compare = new HashMap<>();
//        compare.put("methods", JsonPath.read(jsonArray1.toString(), "$..paper"));
//        compare.put("pwa", JsonPath.read(jsonArray2.toString(), "$..title"));
        ArrayList<String[]> list = new ArrayList<>();
        for (Object o : jsonArray1) {
            String title = (String) ((JSONObject) o).getOrDefault("paper", "");
            String[] tokens = title.split("\\W+");
            list.add(tokens);
        }
        return;
    }

    @Test void testPWCData() {
        String filePath1 = "C:\\Disk_Dev\\Repository\\github-KG\\github-KG-python\\tx_data\\resource\\paperswithcode" +
                "\\evaluation-tables.json";
        String filePath2 = "C:\\Disk_Dev\\Repository\\github-KG\\github-KG-python\\tx_data\\resource\\paperswithcode" +
                "\\links-between-papers-and-code.json";
        String filePath3 = "C:\\Disk_Dev\\Repository\\github-KG\\github-KG-python\\tx_data\\resource\\paperswithcode" +
                "\\methods.json";
        String filePath4 = "C:\\Disk_Dev\\Repository\\github-KG\\github-KG-python\\tx_data\\resource\\paperswithcode" +
                "\\papers-with-abstracts.json";
        HashMap<String, Object> compare = new HashMap<>();
        compare.put("evaluation-tables", (JSONArray) JSONUtil.readJSON(new File(filePath1), StandardCharsets.UTF_8));
        compare.put("links-between-papers-and-code", (JSONArray) JSONUtil.readJSON(new File(filePath2), StandardCharsets.UTF_8));
        compare.put("methods", (JSONArray) JSONUtil.readJSONArray(new File(filePath3), StandardCharsets.UTF_8));
        compare.put("papers-with-abstracts", (JSONArray) JSONUtil.readJSON(new File(filePath4),
                StandardCharsets.UTF_8));
        return;
    }
}
