package com.grameenphone.wipro.fmfs.mfs_communicator.interceptor;

import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.utility.common.EnumUtil;
import com.grameenphone.wipro.utility.common.StringUtil;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class RequestInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String msisdn;
        String wallet_type;
        String channel;
        var pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if(pathVariables != null && !pathVariables.isEmpty()) {
            msisdn = pathVariables.get("msisdn");
            wallet_type = pathVariables.get("wallet_type");
            channel = pathVariables.get("channel");
            if(pathVariables.containsKey("msisdn")) {
                if (msisdn == null || (msisdn = StringUtil.sanitizeMsisdn(msisdn)) == null) {
                    throw new ValidationException("Invalid Wallet MSISDN");
                } else {
                    pathVariables.put("msisdn", "0" + msisdn);
                }
            }
            if (pathVariables.containsKey("wallet_type") && (wallet_type == null || !EnumUtil.hasValue(WalletType.class, wallet_type))) {
                throw new ValidationException("Invalid Wallet Type");
            }
            if (pathVariables.containsKey("channel") && (channel == null || !EnumUtil.hasValue(Channel.class, channel))) {
                throw new ValidationException("Invalid Channel");
            }
        }
        return true;
    }
}