package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.dto.Fmy.aifaq.response.AiFaqQuestionResponse;
import com.example.studentarchives.repository.AiFaqQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 常见问题查询服务（学生端）
 * <p>
 * 返回启用中的常见问题列表，供 AI 助手聊天页"常见问题"区渲染。
 * 前端点击问题后，将问题文本作为消息内容调用现有发送接口即可。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFaqQueryService {

    private final AiFaqQuestionRepository aiFaqQuestionRepository;

    /**
     * 获取启用中的常见问题列表，按 sort 正序
     */
    @Transactional(readOnly = true)
    public List<AiFaqQuestionResponse> listActive() {
        return aiFaqQuestionRepository.findAllActive().stream()
                .map(faq -> AiFaqQuestionResponse.builder()
                        .id(faq.getId())
                        .question(faq.getQuestion())
                        .sort(faq.getSort())
                        .build())
                .collect(Collectors.toList());
    }
}
