package javax.servlet.http;

import java.io.IOException;

public class HttpServletResponseImpl implements HttpServletResponse {
    final private jakarta.servlet.http.HttpServletResponse response;

    public HttpServletResponseImpl(jakarta.servlet.http.HttpServletResponse orgResponse) {
        response = orgResponse;
    }

    public void sendRedirect(String location) throws IOException {
        response.sendRedirect(location);
    }
}