package com.zx.student.archive.factory;

import com.zx.common.utils.spring.SpringUtils;
import org.springframework.stereotype.Component;


@Component
public class CommonBusinessStrategyFactory {

    public ICommonBusinessStrategyHandler getStrategy(String handeler) {
        return  SpringUtils.getBean(handeler);
    }
}
