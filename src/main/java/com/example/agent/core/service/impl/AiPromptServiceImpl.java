package com.example.agent.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.agent.core.domain.entity.AiPromptConfig;
import com.example.agent.core.mapper.AiPromptConfigMapper;
import com.example.agent.core.service.AiPromptService;
import org.springframework.stereotype.Service;

@Service
public class AiPromptServiceImpl extends ServiceImpl<AiPromptConfigMapper, AiPromptConfig> implements AiPromptService {
}



