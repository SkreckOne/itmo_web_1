package org.lab6.fastcgi;


import com.fastcgi.FCGIInterface;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FastCGIServer {

    private static double epsilon = 0.000000000000000000000000001;
    public static void main(String[] args) {
        FCGIInterface fcgiInterface = new FCGIInterface();
        while (fcgiInterface.FCGIaccept() >= 0){
            try {
                String http_method = FCGIInterface.request.params.getProperty("REQUEST_METHOD");
                if (http_method == null) {
                    System.out.println(errorResult("HTTP method is null."));
                    continue;
                }
                if (http_method.equalsIgnoreCase("GET")) {
                    String queryString = FCGIInterface.request.params.getProperty("QUERY_STRING");
                    if (queryString != null) {
                        continue;
                    } else {
                        System.out.println(errorResult("No parameters provided"));
                    }
                    continue;
                }
                if (http_method.equals("POST")) {
                    var contentType = FCGIInterface.request.params.getProperty("CONTENT_TYPE");
                    var queryString = FCGIInterface.request.params.getProperty("REQUEST_URI");

                    if (!queryString.equals("/fcgi-bin/hello-world.jar") && !queryString.equals("/fcgi-bin/hello-world.jar/")) {
                        System.out.println(errorResult("Url is not correct!"));
                        continue;
                    }

                    if (contentType == null) {
                        System.out.println(errorResult("Content-Type is null"));
                        continue;
                    }

                    if (!contentType.equals("application/json")) {
                        System.out.println(errorResult("Content-Type is not supported"));
                        continue;
                    }

                    var requestBody = readRequestBody();
                    JsonObject requestBodyJson = null;

                    try (JsonReader reader = Json.createReader(new StringReader(requestBody))) {
                        requestBodyJson = reader.readObject();
                    } catch (Exception e) {
                        System.out.println(errorResult("Request body could not be parsed as JSON"));
                        continue;
                    }

                    if (!requestBodyJson.containsKey("x") || !requestBodyJson.containsKey("y") || !requestBodyJson.containsKey("r")) {
                        System.out.println(errorResult("Missing x, y, or r value"));
                        continue;
                    }

                    JsonNumber xNumber = requestBodyJson.getJsonNumber("x");
                    JsonNumber yNumber = requestBodyJson.getJsonNumber("y");
                    JsonNumber rNumber = requestBodyJson.getJsonNumber("r");

                    String xStr = xNumber.toString();
                    String yStr = yNumber.toString();
                    String rStr = rNumber.toString();

                    double x = xNumber.doubleValue();
                    double y = yNumber.doubleValue();
                    double r = rNumber.doubleValue();

                    BigDecimal xBigDecimal = BigDecimal.valueOf(x);
                    BigDecimal yBigDecimal = BigDecimal.valueOf(y);
                    BigDecimal rBigDecimal = BigDecimal.valueOf(r);

                    if (!xStr.equals(xBigDecimal.toPlainString())) {
                        System.out.println(errorResult("Precision error in 'x' value"));
                        continue;
                    }

                    if (!yStr.equals(yBigDecimal.toPlainString())) {
                        System.out.println(errorResult("Precision error in 'y' value"));
                        continue;
                    }

                    if (!rStr.equals(rBigDecimal.toPlainString())) {
                        System.out.println(errorResult("Precision error in 'r' value"));
                        continue;
                    }

                    List<Double> validXValues = Arrays.asList(-5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0);
                    List<Double> validRValues = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);

                    if (!validXValues.contains(x) || doubleCompare(y, -5.0) == 1 || doubleCompare(y, 3.0) == -1 || !validRValues.contains(r)) {
                        System.out.println(errorResult("Invalid x, y, or r value"));
                        continue;
                    }

                    System.out.println(checkCoords(x, y, r));
                } else {
                    System.out.println(errorResult("Unsupported HTTP method: " + http_method));
                }
            } catch (Exception e){
                System.err.println(e.getMessage());
                System.out.println(errorResult("Something went wrong."));
            }
        }
    }

    private static String readRequestBody() throws IOException {
        FCGIInterface.request.inStream.fill();

        var contentLength = FCGIInterface.request.inStream.available();
        ByteBuffer buffer = ByteBuffer.allocate(contentLength);
        var readBytes = FCGIInterface.request.inStream.read(buffer.array(), 0, contentLength);

        var requestBodyRaw = new byte[readBytes];
        buffer.get(requestBodyRaw);
        buffer.clear();

        return new String(requestBodyRaw, StandardCharsets.UTF_8);
    }

    private static String checkCoords(double x, double y, double r){
        long start= System.currentTimeMillis();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        boolean isInArea = checkShapes(x, y, r);
        long execTime = System.currentTimeMillis() - start;
        String result = "{\"isInArea\": " + isInArea + ", \"currentTime\": \"" + now + "\", \"execTime\": " + execTime + ", \"x\": " + x +
                ", \"y\": " + y + ", \"r\": " + r +"}";
        return successResponse(result);
    }

    private static Boolean checkShapes(double x, double y, double r){
        if (Double.compare(x, 0) <= 0 && Double.compare(x, -r/2.0) >=0 && Double.compare(y, 0) >= 0 && Double.compare(y, 3) <= 0 )
            return true;

        if (Double.compare(x, 0) >= 0 && Double.compare(y, x / 2 - r / 2) >= 0 && Double.compare(y, 0) <= 0) {
            return true;
        }

        return Double.compare(x, 0) <=0 && Double.compare(y, 0) <= 0 && Double.compare(x * x + y * y, r * r) <= 0;
    }

    private static boolean compareWithEpcilon(double a, double b){
        return Math.abs(a-b) < epsilon;
    }

    private static int doubleCompare(double a, double b) {
        if (compareWithEpcilon(a, b)) {
            return 0;
        } else if (a < b) {
            return -1;
        } else {
            return 1;
        }
    }


    private static String errorResult(String error) {
        return """
                HTTP/1.1 400 Bad Request
                Content-Type: text/html
                Content-Length: %d
                
                %s
                """.formatted(error.getBytes(StandardCharsets.UTF_8).length, error);
    }
    private static String successResponse(String content) {
        return """
                HTTP/1.1 200 OK
                Content-Type: application/json
                Content-Length: %d

                %s
                """.formatted(content.getBytes(StandardCharsets.UTF_8).length, content);
    }
}
