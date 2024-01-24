package com.grameenphone.wipro.utility;

import org.apache.http.entity.ContentType;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class TextView extends ModelAndView {
    public TextView(final String text) {
        super(new View() {
            @Override
            public String getContentType() {
                return ContentType.TEXT_PLAIN.toString();
            }

            @Override
            public void render(Map<String, ?> map, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
                httpServletResponse.getOutputStream().write(text.getBytes());
            }
        });
    }
}