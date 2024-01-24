package com.grameenphone.wipro.fmfs.mfs_communicator.validator;

import com.grameenphone.wipro.exception.TaggedCheckedException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.BaseRequest;
import com.grameenphone.wipro.utility.common.StringUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

@ControllerAdvice
public class RequestBodyValidator extends RequestBodyAdviceAdapter {
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return BaseRequest.class.isAssignableFrom((Class)targetType);
    }

    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if(body instanceof BaseRequest) {
            BaseRequest _body = (BaseRequest) body;
            String _10DigitMsisdn;
            if (_body.msisdn == null || (_10DigitMsisdn = StringUtil.sanitizeMsisdn(_body.msisdn)) == null) {
                throw new TaggedCheckedException(new ValidationException("Invalid Wallet MSISDN"));
            }
            _body.msisdn = "0" + _10DigitMsisdn;
        }
        return body;
    }
}