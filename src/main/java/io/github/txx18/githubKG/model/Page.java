package io.github.txx18.githubKG.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 页面类
 *
 * @author Shane Tang
 * @create 2019-12-23 14:21
 */
@Data // 使用了lombok
@AllArgsConstructor
@NoArgsConstructor
public class Page<T> {

    private long total;

    private List<T> rows;

}
