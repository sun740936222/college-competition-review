package com.zhk.contest.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MaterialResponse {
    private Long id;
    private String materialType;
    private String originalFilename;
    private String storedFilename;
    private Long fileSize;

}
