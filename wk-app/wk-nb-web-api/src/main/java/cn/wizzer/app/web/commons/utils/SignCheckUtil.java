package cn.wizzer.app.web.commons.utils;

import cn.wizzer.app.sys.modules.services.SysApiService;
import cn.wizzer.framework.base.Result;
import com.alibaba.dubbo.config.annotation.Reference;
import org.nutz.integration.jedis.RedisService;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.lang.Times;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import java.util.Map;

/**
 * Created by wizzer on 2017/7/21.
 */
@IocBean
public class SignCheckUtil {
    private static final Log log = Logs.get();
    @Inject
    private PropertiesProxy conf;
    @Inject
    @Reference
    private SysApiService sysApiService;
    @Inject
    private RedisService redisService;

    public Result checkSign(Map<String, Object> paramMap) {
        try {
            String appid = Strings.sNull(paramMap.get("appid"));
            String sign = Strings.sNull(paramMap.get("sign"));
            String timestamp = Strings.sNull(paramMap.get("timestamp"));
            String nonce = Strings.sNull(paramMap.get("nonce"));
            String appkey = sysApiService.getAppkey(appid);
            if (Strings.isBlank(appid) || Strings.isBlank(appkey)) {
                return Result.error(1, "appid不正确");
            }
            if (Times.getTS() - Long.valueOf(timestamp) > 60 * 1000) {//时间戳相差大于1分钟则为无效的
                return Result.error(2, "timestamp不正确");
            }
            String nonceCache = redisService.get("api_sign_nonce:" + appid + "_" + nonce);
            if (Strings.isNotBlank(nonceCache)) {//如果一分钟内nonce是重复的则为无效,让nonce只能使用一次
                return Result.error(3, "nonce不正确");

            }
            if (!SignUtil.createSign(appkey, paramMap).equalsIgnoreCase(sign)) {
                return Result.error(4, "sign签名不正确");
            }
            //nonce保存到缓存
            redisService.set("api_sign_nonce:" + appid + "_" + nonce, nonce);
            redisService.expire("api_sign_nonce:" + appid + "_" + nonce, 60);
            return Result.success("验证成功");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(-1, "系统异常");
        }
    }
}
