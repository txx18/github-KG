package io.github.txx18.githubKG;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;

/**
 * @author ShaneTang
 * @create 2020-12-18 19:38
 */
public class UtilTest {

    @Test
    public void testSplit() {
        String str1 = "ts-dfd-dfdsaf-dfa-dfaf";
        String str2 = "ts";
        String str3 = "";
        String str4 = "ts/dfd/dfdsaf/dfa/dfaf";
        String[] split1 = StrUtil.split(str1, "-");
        String[] split2 = StrUtil.split(str2, "-");
        String[] split3 = StrUtil.split(str3, "-");
        String[] split4 = StrUtil.split(str4, "-");
        return;
    }

    @Test
    public void testRemoveSymbol() {
        String stri = "ss             &*(,.~1如果@&(^-自己!!知道`什`么#是$苦%……Z，&那*()么一-=定——+告诉::;\"'/?.,><[]{}\\||别人什么是甜。";
        String stri4 = stri.replaceAll("[\\pP\\p{Punct}]", "");//清除所有符号,只留下字母 数字  汉字  共3类.
        String method1 = "motion-encoded-particle-swarm-optimization";

        return;
    }

}
