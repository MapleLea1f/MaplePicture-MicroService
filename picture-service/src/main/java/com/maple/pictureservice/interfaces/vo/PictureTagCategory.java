package com.maple.pictureservice.interfaces.vo;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {

    /**
     * 图片标签列表
     */
    List<String> tagList;

    /**
     * 图片分类列表
     */
    List<String> categoryList;
}
