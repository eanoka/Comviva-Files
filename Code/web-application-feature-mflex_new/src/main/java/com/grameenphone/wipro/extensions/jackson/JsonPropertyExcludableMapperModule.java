package com.grameenphone.wipro.extensions.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.IOException;
import java.util.Arrays;

import static com.grameenphone.wipro.fmfs.cbp.consts.RequestAttribute.HANDLER_METHOD;
import static com.grameenphone.wipro.fmfs.cbp.consts.RequestAttribute.REQUEST_MODEL_VIEW_CONTAINER;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

public class JsonPropertyExcludableMapperModule extends SimpleModule {
    private final static Logger logger = LoggerFactory.getLogger(JsonPropertyExcludableMapperModule.class);

    protected final ThreadLocal<JsonExcludeFilteredPaths> states = new ThreadLocal<>();

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addBeanSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription desc, JsonSerializer<?> serializer) {
                if (serializer instanceof BeanSerializer) {
                    return new BeanSerializer((BeanSerializer) serializer.withFilterId("default")) {
                        @Override
                        protected void serializeFieldsFiltered(Object bean, JsonGenerator gen, SerializerProvider provider) throws IOException {
                            boolean first = false;
                            JsonExcludeFilteredPaths _states = states.get();
                            try {
                                if (_states == null) {
                                    _states = new JsonExcludeFilteredPaths();
                                    first = true;
                                    states.set(_states);
                                    JsonExcludeNestedProps excludeProps = bean.getClass().getAnnotation(JsonExcludeNestedProps.class);
                                    if (excludeProps != null && excludeProps.value().length > 0) {
                                        _states.excludedPaths.addAll(Arrays.asList(excludeProps.value()));
                                    }
                                    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                                    if (requestAttributes != null) { //Whether to consider action annotation or not
                                        ModelAndViewContainer mavContainer = (ModelAndViewContainer) requestAttributes.getAttribute(REQUEST_MODEL_VIEW_CONTAINER, SCOPE_REQUEST);
                                        if (mavContainer != null && mavContainer.isRequestHandled()) {
                                            HandlerMethod handlerMethod = (HandlerMethod) requestAttributes.getAttribute(HANDLER_METHOD, SCOPE_REQUEST);
                                            if ((excludeProps = handlerMethod.getMethodAnnotation(JsonExcludeNestedProps.class)) != null) {
                                                _states.excludedPaths.addAll(Arrays.asList(excludeProps.value()));
                                            }
                                        }
                                    }
                                }
                                super.serializeFieldsFiltered(bean, gen, provider);
                            } catch (StackOverflowError error) {
                                if(first) {
                                    logger.error("Stack Overflow Error Processing Bean \n" + _states.toString() + "\n" + bean.getClass());
                                } else {
                                    throw error;
                                }
                            } catch (Throwable error) {
                                if(first) {
                                    logger.error("Error occurred in serialization for state \n" + _states.toString() + "\n", error);
                                } else {
                                    throw error;
                                }
                            } finally {
                                if (first) {
                                    states.remove();
                                }
                            }
                        }
                    };
                }
                return serializer;
            }
        });
    }
}