package org.lab6.fastcgi;


import com.fastcgi.FCGIInterface;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class FastCGIServer {
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
                    if (contentType == null) {
                        System.out.println(errorResult("Content-Type is null"));
                        continue;
                    }

                    if (!contentType.equals("application/json")) {
                        System.out.println(errorResult("Content-Type is not supported"));
                        continue;
                    }

                    var requestBody = readRequestBody();
//                    System.out.println(successResponse(requestBody));
                    JsonObject requestBodyJson = null;
                    try (JsonReader reader = Json.createReader(new StringReader(requestBody))) {
                        requestBodyJson = reader.readObject();
                    } catch (Exception e) {
                        System.out.println(errorResult("Request body could not be parsed as JSON"));
                        continue;
                    }
//                    System.out.println(successResponse(requestBody));

                    if (!requestBodyJson.containsKey("x") || !requestBodyJson.containsKey("y") || !requestBodyJson.containsKey("r")) {
                        System.out.println(errorResult("Missing x, y or R value"));
                        continue;
                    }

                    double x = requestBodyJson.getJsonNumber("x").doubleValue();
                    double y = requestBodyJson.getJsonNumber("y").doubleValue();
                    double r = requestBodyJson.getJsonNumber("r").doubleValue();



                    List<Double> validXValues = Arrays.asList(-5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0);
                    List<Double> validRValues = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);

                    if (!validXValues.contains(x) || y < -5.0 || y > 3.0 || !validRValues.contains(r)) {
                        System.out.println(errorResult("Invalid x, y or r value"));
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
        String result = "{\"isInArea\": " + isInArea + ", \"currentTime\": \"" + now + "\", \"execTime\": " + execTime + "}";
        return successResponse(result);
    }

    private static Boolean checkShapes(double x, double y, double r){
        if (x <= 0 && x >= -r/2 && y >= 0 && y <= 3)
            return true;

        if (x >= 0 && y >= x / 2 - r / 2 && y <= 0) {
            return true;
        }

        return x <= 0 && y <= 0 && (x * x + y * y <= r * r);
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
