package com.maple.pictureservice.interfaces.dto;

import lombok.Data;

@Data
public class SearchPictureByColorRequest {
    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
