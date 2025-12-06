package com.ruoyi.student.archive.factory;

import com.ruoyi.common.utils.spring.SpringUtils;
import org.springframework.stereotype.Component;


@Component
public class CommonBusinessStrategyFactory {

    public ICommonBusinessStrategyHandler getStrategy(String handeler) {
        return  SpringUtils.getBean(handeler);
    }
}
