import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

public class VulnApp {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Роут для веб-интерфейса
        server.createContext("/", new UIHandler()); 
        // Уязвимый API
        server.createContext("/api/v1/execute", new UniversalHandler());
        
        server.setExecutor(null);
        server.start();
        System.out.println("DevOps Diagnostic System started on port 8080...");
    }

    static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String html = "<!DOCTYPE html>\n" +
                          "<html>\n" +
                          "<head>\n" +
                          "    <title>DevOps System Diagnostics</title>\n" +
                          "    <style>\n" +
                          "        body { font-family: monospace; background-color: #1e1e1e; color: #00ff00; padding: 20px; }\n" +
                          "        .panel { border: 1px solid #00ff00; padding: 20px; width: 450px; border-radius: 5px; background: #2a2a2a; }\n" +
                          "        select, button { background: #444; color: #fff; border: 1px solid #777; padding: 8px; margin-top: 15px; width: 100%; cursor: pointer; }\n" +
                          "        button:hover { background: #555; }\n" +
                          "        pre { background: #000; padding: 15px; border: 1px solid #444; color: #00ff00; overflow-x: auto; }\n" +
                          "        h2 { margin-top: 0; }\n" +
                          "    </style>\n" +
                          "</head>\n" +
                          "<body>\n" +
                          "    <h2>[ DevOps Diagnostic Panel v1.2 ]</h2>\n" +
                          "    <div class=\"panel\">\n" +
                          "        <p>Select diagnostic module to query system properties:</p>\n" +
                          "        <select id=\"pluginSelect\">\n" +
                          "            <option value=\"os.name\">Check OS Name</option>\n" +
                          "            <option value=\"os.arch\">Check OS Architecture</option>\n" +
                          "            <option value=\"java.version\">Check Java Version</option>\n" +
                          "        </select>\n" +
                          "        <button onclick=\"runDiagnostic()\">Run Check</button>\n" +
                          "    </div>\n" +
                          "    <h3>Console Output:</h3>\n" +
                          "    <pre id=\"output\">Ready...</pre>\n" +
                          "    \n" +
                          "    <script>\n" +
                          "        function runDiagnostic() {\n" +
                          "            const arg = document.getElementById(\"pluginSelect\").value;\n" +
                          "            // The API uses dynamic class loading for flexible diagnostics\n" +
                          "            const url = `/api/v1/execute?class=java.lang.System&method=getProperty&arg=${arg}`;\n" +
                          "            \n" +
                          "            document.getElementById(\"output\").innerText = \"Executing...\";\n" +
                          "            fetch(url)\n" +
                          "                .then(res => res.text())\n" +
                          "                .then(data => document.getElementById(\"output\").innerText = data)\n" +
                          "                .catch(err => document.getElementById(\"output\").innerText = \"Error: \" + err);\n" +
                          "        }\n" +
                          "    </script>\n" +
                          "</body>\n" +
                          "</html>";
            t.sendResponseHeaders(200, html.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(html.getBytes());
            os.close();
        }
    }

    static class UniversalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Map<String, String> params = parseQuery(t.getRequestURI().getQuery());
            String response;

            try {
                String className = params.get("class");
                String methodName = params.get("method");
                String arg = params.get("arg");

                if (className == null || methodName == null || arg == null) {
                    response = "Usage: ?class=...&method=...&arg=...";
                } else {
                    Class<?> clazz = Class.forName(className);
                    
                    if (className.equals("java.lang.Runtime")) {
                        Method getRuntime = clazz.getMethod("getRuntime");
                        Object runtimeObj = getRuntime.invoke(null);
                        Method methodToCall = clazz.getMethod(methodName, String.class);
                        
                        Process p = (Process) methodToCall.invoke(runtimeObj, arg);
                        
                        response = readStream(p.getInputStream());
                        if (response.isEmpty()) response = readStream(p.getErrorStream());
                    } else {
                        Method methodToCall = clazz.getMethod(methodName, String.class);
                        Object result = methodToCall.invoke(null, arg);
                        response = result != null ? result.toString() : "Executed: NULL";
                    }
                }
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                response = "Error: " + cause.toString();
            }

            t.sendResponseHeaders(200, response.getBytes().length == 0 ? 1 : response.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private String readStream(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query == null) return result;
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    try {
                        String key = java.net.URLDecoder.decode(entry[0], "UTF-8");
                        String value = java.net.URLDecoder.decode(entry[1], "UTF-8");
                        result.put(key, value);
                    } catch (Exception e) { }
                }
            }
            return result;
        }
    }
}