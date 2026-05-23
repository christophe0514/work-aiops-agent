package com.example.agent.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_prompt_config")
public class AiPromptConfig {

    private Long id;

    private String promptCode;

    private String promptName;

    private String promptType;

    private String promptContent;

    private String modelName;

    private String agentName;

    private Integer versionNum;

    private Integer isEnabled;

    private String remark;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}

