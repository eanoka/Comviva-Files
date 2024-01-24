package javax.servlet.http;

import java.io.IOException;

public interface HttpServletResponse {
    void sendRedirect(String location) throws IOException;
}