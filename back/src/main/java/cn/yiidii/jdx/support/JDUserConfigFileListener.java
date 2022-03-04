package cn.yiidii.jdx.support;

import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.yiidii.jdx.config.prop.JDUserConfigProperties;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

/**
 * JDUserConfigFileListener配置文件监听
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JDUserConfigFileListener implements InitializingBean {

    private final JDUserConfigProperties jdUserConfigProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 启动文件监听
        SimpleWatcher simpleWatcher = new SimpleWatcher() {
            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                jdUserConfigProperties.update(false);
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                log.error("JDUser配置文件删除, 系统停止");
                AbstractApplicationContext ctx = (AbstractApplicationContext) SpringUtil.getApplicationContext();
                ctx.registerShutdownHook();
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                log.error("JDUser配置文件删除, 系统停止");
                AbstractApplicationContext ctx = (AbstractApplicationContext) SpringUtil.getApplicationContext();
                ctx.registerShutdownHook();
            }
        };
        // 延迟处理监听事件
        WatchMonitor.createAll(JDUserConfigProperties.JD_USER_CONFIG_FILE_PAH, new DelayWatcher(simpleWatcher, 1000)).start();
    }

}
