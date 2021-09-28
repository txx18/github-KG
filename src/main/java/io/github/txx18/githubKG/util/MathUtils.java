package io.github.txx18.githubKG.util;

/**
 * @author ShaneTang
 * @create 2021-05-18 17:22
 */
public class MathUtils {

    public static double[] sumNormalization(double[] arr) {
        double sum = 0;
        for (double v : arr) {
            sum += v;
        }
        double[] res = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            res[i] = arr[i] / sum;
        }
        return res;
    }
}
