package com.grameenphone.wipro.fmfs.cbp.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.grameenphone.wipro.fmfs.cbp.Application;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class HeartBeatFilter implements Filter {
    public final List<String> ips = new ArrayList<>();

    public HeartBeatFilter() {
        String urls = Application.environment.getProperty("heartbit.urls");
        if(urls != null && !"".equals(urls = urls.trim())) {
            ips.addAll(Arrays.stream(urls.split(",")).map(u -> u.trim()).collect(Collectors.toList()));
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(ips.contains(request.getRemoteAddr())) {
            ((HttpServletResponse) response).setStatus(200);
            try {
                response.getOutputStream().write(new byte[]{'O', 'K'});
            } catch (IOException e) {
            }
            return;
        }
        chain.doFilter(request, response);
    }
}