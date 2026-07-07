package com.zx.student.archive.factory;

import com.zx.common.constant.AppErrorCode;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.strategy.question.IQuestionBusinessStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class QuestionBusinessProcessStrategyFactory {

    private final Map<Integer, IQuestionBusinessStrategy> strategyMap;

    // 使用构造函数注入
    public QuestionBusinessProcessStrategyFactory(List<IQuestionBusinessStrategy> strategies) {
        this.strategyMap = strategies.stream().collect(Collectors.toUnmodifiableMap(
            IQuestionBusinessStrategy::getBusinessType,
            Function.identity(),
            (s1, s2) -> { throw new IllegalStateException("Duplicate strategy for type: " + s1.getBusinessType()); }
        ));
    }

    public IQuestionBusinessStrategy getStrategy(Integer businessType) throws ServiceException {
        if (businessType == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_NOT_NULL_MSG);
        }
        IQuestionBusinessStrategy strategy = strategyMap.get(businessType);
        if (strategy == null) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_BUSINESS_STRATEGY_NOT_EXIST_MSG);
        }
        return strategy;
    }
}