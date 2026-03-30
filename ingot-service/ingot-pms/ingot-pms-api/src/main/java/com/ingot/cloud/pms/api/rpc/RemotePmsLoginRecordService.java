package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.dto.auth.LoginRecordDTO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * PMS 登录记录 Feign 接口
 * <p>
 * Auth 服务通过此接口通知 PMS 异步处理登录成功/失败事件：
 * 更新 last_login_at、last_login_ip、failed_login_count，以及触发自动锁定逻辑。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@FeignClient(contextId = "pmsLoginRecordService", value = ServiceNameConstants.PMS_SERVICE)
public interface RemotePmsLoginRecordService {

    /**
     * 记录用户登录事件（成功或失败）
     *
     * @param dto 登录记录 DTO
     * @return 处理结果
     */
    @PostMapping("/inner/user/login/record")
    R<Void> record(@RequestBody LoginRecordDTO dto);
}
