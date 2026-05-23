package com.example.agent.core.service;

import com.example.agent.core.domain.vo.KbIngestResultVO;
import com.example.agent.core.domain.vo.KbSearchResultVO;

import java.util.List;

public interface KnowledgeBaseService {

    KbIngestResultVO ingestThemeBusinessKnowledge();

    List<KbSearchResultVO> search(String query);
}
