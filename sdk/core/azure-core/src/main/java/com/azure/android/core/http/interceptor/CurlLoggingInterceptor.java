// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.core.http.interceptor;

import androidx.annotation.NonNull;

import com.azure.android.core.util.CoreUtils;
import com.azure.android.core.util.logging.ClientLogger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Pipeline interceptor that logs HTTP requests as cURL commands.
 */
public class CurlLoggingInterceptor implements Interceptor {
    private final ClientLogger logger;
    private boolean compressed;
    private StringBuilder curlCommand;

    public CurlLoggingInterceptor() {
        this(ClientLogger.getDefault(CurlLoggingInterceptor.class));
    }

    public CurlLoggingInterceptor(ClientLogger clientLogger) {
        logger = clientLogger;
        compressed = false;
        curlCommand = new StringBuilder("curl");
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Headers headers = request.headers();

        curlCommand.append(" -X ")
            .append(request.method());

        addHeadersToCurlCommand(headers, curlCommand);

        RequestBody requestBody = request.body();
        String bodyEvaluation = LogUtils.evaluateBody(headers);

        if (!bodyEvaluation.equals("Log body")) {
            curlCommand.append(bodyEvaluation);
        } else if (requestBody != null) {
            addBodyToCurlCommand(requestBody, curlCommand);
        }

        curlCommand.append(" \"")
            .append(request.url())
            .append("\"");

        // TODO: Add log level guard for headers and body.
        logger.debug("╭--- cURL " + request.url());
        logger.debug(curlCommand.toString());
        logger.debug("╰--- (copy and paste the above line to a terminal)");

        return chain.proceed(chain.request());
    }

    /**
     * Adds HTTP headers to the StringBuilder that is generating the cURL command.
     *
     * @param headers     HTTP headers on the request or response.
     * @param curlCommand The StringBuilder that is generating the cURL command.
     */
    private void addHeadersToCurlCommand(Headers headers, StringBuilder curlCommand) {
        int size = headers.size();
        for (int i = 0; i < size; i++) {
            String headerName = headers.name(i);
            String headerValue = headers.value(i);
            if (headerValue.startsWith("\"") || headerValue.endsWith("\"")) {
                headerValue = "\\\"" + headerValue.replaceAll("\"", "") + "\\\"";
            }

            curlCommand.append(" -H \"")
                .append(headerName)
                .append(": ")
                .append(headerValue)
                .append("\"");

            if (headerValue.equalsIgnoreCase("gzip")) {
                compressed = true;
            }
        }
    }

    /**
     * Adds HTTP headers into the StringBuilder that is generating the cURL command.
     *
     * @param requestBody Body of the request.
     * @param curlCommand The StringBuilder that is generating the cURL command.
     */
    private void addBodyToCurlCommand(RequestBody requestBody, StringBuilder curlCommand) {
        try {
            Buffer buffer = new Buffer();
            MediaType contentType = requestBody.contentType();
            Charset charset = (contentType == null) ? UTF_8 : contentType.charset(UTF_8);

            requestBody.writeTo(buffer);

            if (charset != null) {
                String requestBodyString = buffer.readString(charset);
                Map<Character, CharSequence> toReplace = new HashMap<>();

                toReplace.put('\n', "\\n");
                toReplace.put('\"', "\\\"");

                curlCommand.append(" --data $'")
                    .append(CoreUtils.replace(requestBodyString, toReplace))
                    .append("'");

                if (compressed) {
                    curlCommand.append(" --compressed");
                }
            } else {
                logger.warning("Could not log the response body. No encoding charset found.");
            }
        } catch (IOException e) {
            logger.warning("Could not log the request body", e);
        }
    }
}
