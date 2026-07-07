package com.zx.student.archive.domain.bo.paper;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

/**
 * 分片上传BO
 * 
 * @author zx
 */
@Data
public class ChunkUploadBO {
    
    /**
     * 上传ID（由初始化接口返回）
     */
    @NotNull(message = "上传ID不能为空")
    private String uploadId;
    
    /**
     * 分片索引（从0开始）
     */
    @NotNull(message = "分片索引不能为空")
    @PositiveOrZero(message = "分片索引必须大于等于0")
    private Integer chunkIndex;
    
    /**
     * 分片文件
     */
    @NotNull(message = "分片文件不能为空")
    private MultipartFile chunkFile;
}









