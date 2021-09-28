package io.github.txx18.githubKG.model;

/**
 * @author ShaneTang
 * @create 2021-05-17 16:13
 */
public class RecommendRecord {

    private String key;

    private Double score;

    private Double repoDegree;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getRepoDegree() {
        return repoDegree;
    }

    public void setRepoDegree(Double repoDegree) {
        this.repoDegree = repoDegree;
    }
}
