package arc.util;

import arc.func.*;
import arc.struct.*;
import arc.util.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Utility class for making HTTP requests. */
public class Http{
    protected static ExecutorService exec = Threads.unboundedExecutor("HTTP", 1);

    /** @return a new HttpRequest that must be configured & submitted. */
    public static HttpRequest request(HttpMethod method, String url){
        if(url == null) throw new NullPointerException("url cannot be null.");
        return new HttpRequest(method).url(url);
    }

    /** @return a new GET HttpRequest that must be configured & submitted. */
    public static HttpRequest get(String url){
        if(url == null) throw new NullPointerException("url cannot be null.");
        return new HttpRequest(HttpMethod.GET).url(url);
    }

    /** Creates and submits a HTTP GET request. */
    public static void get(String url, ConsT<HttpResponse, Exception> callback){
        get(url).submit(callback);
    }

    /** Creates and submits a HTTP GET request, with an error handler. */
    public static void get(String url, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
        get(url).error(error).submit(callback);
    }

    /** @return a new POST HttpRequest that must be configured & submitted. */
    public static HttpRequest post(String url){
        return post(url, (String)null);
    }

    /** Creates and submits a HTTP POST request. */
    public static void post(String url, ConsT<HttpResponse, Exception> callback){
        post(url).submit(callback);
    }

    /** @return a new POST HttpRequest that must be configured & submitted. */
    public static HttpRequest post(String url, String content){
        if(url == null) throw new NullPointerException("url cannot be null.");
        return new HttpRequest(HttpMethod.POST).url(url).content(content);
    }

    public static class HttpResponse{
        private final HttpURLConnection connection;
        private HttpStatus status;

        protected HttpResponse(HttpURLConnection connection) throws IOException{
            this.connection = connection;
            this.status = HttpStatus.byCode(connection.getResponseCode());
        }

        /** @return the length of received content in bytes as a long. May throw an exception (?) */
        public long getContentLength(){
            return connection.getContentLength();
        }

        /**
         * Returns the data of the HTTP response as a byte[].
         * <p>
         * <b>Note</b>: This method may only be called once per response.
         * </p>
         * @return the result as a byte[] or null in case of a timeout or if the operation was canceled/terminated abnormally. The
         * timeout is specified when creating the HTTP request, with {@link HttpRequest#timeout(int)}
         */
        public byte[] getResult(){
            InputStream input = getResultAsStream();

            // If the response does not contain any content, input will be null.
            if(input == null){
                return Streams.emptyBytes;
            }

            try{
                return Streams.copyBytes(input, connection.getContentLength());
            }catch(IOException e){
                return Streams.emptyBytes;
            }finally{
                Streams.close(input);
            }
        }

        /**
         * Returns the data of the HTTP response as a {@link String}.
         * <p>
         * <b>Note</b>: This method may only be called once per response.
         * </p>
         * @return the result as a string or null in case of a timeout or if the operation was canceled/terminated abnormally. The
         * timeout is specified when creating the HTTP request, with {@link HttpRequest#timeout(int)}
         */
        public String getResultAsString(){
            InputStream input = getResultAsStream();

            // If the response does not contain any content, input will be null.
            if(input == null){
                return "";
            }

            try{
                return Streams.copyString(input, connection.getContentLength());
            }catch(IOException e){
                return "";
            }finally{
                Streams.close(input);
            }
        }

        /**
         * Returns the data of the HTTP response as an {@link InputStream}. <b><br>
         * Warning:</b> Do not store a reference to this InputStream. The underlying HTTP connection will be closed after that
         * callback finishes executing. Reading from the InputStream after it's connection has been closed will lead to exception.
         * @return An {@link InputStream} with the {@link HttpResponse} data.
         */
        public InputStream getResultAsStream(){
            try{
                return connection.getInputStream();
            }catch(IOException e){
                return connection.getErrorStream();
            }
        }

        /** @return the {@link HttpStatus} containing the statusCode of the HTTP response. */
        public HttpStatus getStatus(){
            return status;
        }

        /** @return the value of the header with the given name as a {@link String}, or null if the header is not set. */
        public String getHeader(String name){
            return connection.getHeaderField(name);
        }

        /**
         * Returns a Map of the headers. The keys are Strings that represent the header name. Each values is a List of Strings that
         * represent the corresponding header values.
         */
        public ObjectMap<String, Seq<String>> getHeaders(){
            //convert between the struct types
            ObjectMap<String, Seq<String>> out = new ObjectMap<>();
            Map<String, List<String>> fields = connection.getHeaderFields();
            for(String key : fields.keySet()){
                if(key != null){
                    out.put(key, Seq.with(fields.get(key).toArray(new String[0])));
                }
            }
            return out;
        }

    }

    public static class HttpRequest{
        public HttpMethod method = HttpMethod.GET;
        /** The URL to send this request to.*/
        public String url;
        public ObjectMap<String, String> headers = new ObjectMap<>();
        /**The time to wait for the HTTP request to be processed, use 0 to block until it is done. The timeout is used for both
         * the timeout when establishing TCP connection, and the timeout until the first byte of data is received.*/
        public int timeout = 8000;

        /**The content to be used in the HTTP request: A string encoded in the corresponding Content-Encoding set in the headers, with the data to send with the
         * HTTP request. For example, in case of HTTP GET, the content is used as the query string of the GET while on a
         * HTTP POST it is used to send the POST data.*/
        public String content;

        /**The content as a stream to be used for a POST for example, to transmit custom data.*/
        public InputStream contentStream;

        /**Sets whether 301 and 302 redirects are followed. By default true. Can't be changed in the web backend because this uses
         * XmlHttpRequests which always redirect.*/
        public boolean followRedirects = true;
        /** Whether a cross-origin request will include credentials. Default: false */
        public boolean includeCredentials = false;
        /** Handler for 4xx + 5xx errors, as well as exceptions thrown during connection. */
        public Cons<Throwable> errorHandler = Log::err;

        protected HttpRequest(){

        }

        protected HttpRequest(HttpMethod method){
            this.method = method;
        }

        public HttpRequest error(Cons<Throwable> failed){
            errorHandler = failed;
            return this;
        }

        public HttpRequest method(HttpMethod method){
            this.method = method;
            return this;
        }

        public HttpRequest url(String url){
            this.url = url;
            return this;
        }

        public HttpRequest timeout(int timeout){
            this.timeout = timeout;
            return this;
        }

        public HttpRequest redirects(boolean followRedirects){
            this.followRedirects = followRedirects;
            return this;
        }

        public HttpRequest credentials(boolean includeCredentials){
            this.includeCredentials = includeCredentials;
            return this;
        }

        public HttpRequest header(String name, String value){
            headers.put(name, value);
            return this;
        }

        public HttpRequest content(String content){
            this.content = content;
            return this;
        }

        public HttpRequest content(InputStream contentStream){
            this.contentStream = contentStream;
            return this;
        }

        /** Submits this request asynchronously. */
        public void submit(ConsT<HttpResponse, Exception> success){
            Http.exec.submit(() -> block(success));
        }

        /** Blocks until this request is done. */
        public void block(ConsT<HttpResponse, Exception> success){
            if(url == null){
                errorHandler.get(new ArcRuntimeException("can't process a HTTP request without URL set"));
                return;
            }

            try{
                URL url;

                if(method == HttpMethod.GET){
                    String queryString = "";
                    String value = content;
                    if(value != null && !"".equals(value)) queryString = "?" + value;
                    url = new URL(this.url + queryString);
                }else{
                    url = new URL(this.url);
                }

                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                //should be enabled to upload data.
                boolean doingOutPut = method == HttpMethod.POST || method == HttpMethod.PUT;
                connection.setDoOutput(doingOutPut);
                connection.setDoInput(true);
                connection.setRequestMethod(method.toString());
                HttpURLConnection.setFollowRedirects(followRedirects);

                //set headers
                headers.each(connection::addRequestProperty);

                //timeouts
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);

                try{
                    // Set the content for POST and PUT (GET has the information embedded in the URL)
                    if(doingOutPut){
                        // we probably need to use the content as stream here instead of using it as a string.
                        if(content != null){
                            try(OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), Strings.utf8)){
                                writer.write(content);
                            }
                        }else{
                            if(contentStream != null){
                                try(OutputStream os = connection.getOutputStream()){
                                    Streams.copy(contentStream, os);
                                }
                            }
                        }
                    }

                    connection.connect();

                    try{
                        int code = connection.getResponseCode();

                        //4xx or 5xx error
                        if(code >= 400){
                            HttpStatus status = HttpStatus.byCode(code);
                            errorHandler.get(new HttpStatusException("HTTP request failed with error: " + code + " (" + status + ", URL = " + url + ")", status, new HttpResponse(connection)));
                        }else{
                            success.get(new HttpResponse(connection));
                        }

                    }finally{
                        connection.disconnect();
                    }

                }catch(Throwable e){
                    connection.disconnect();
                    errorHandler.get(e);
                }
            }catch(Throwable e){
                errorHandler.get(e);
            }
        }
    }

    /** Exception returned when a 4xx or 5xx error is encountered. */
    public static class HttpStatusException extends RuntimeException{
        /** The 4xx or 5xx error code. */
        public HttpStatus status;
        /** The response that was sent along with the status code. */
        public HttpResponse response;

        public HttpStatusException(String message, HttpStatus status, HttpResponse response){
            super(message);
            this.status = status;
            this.response = response;
        }
    }

    /** Provides all HTTP methods to use when creating a {@link HttpRequest}.*/
    public enum HttpMethod{
        GET, POST, PUT, DELETE, HEAD, CONNECT, OPTIONS, TRACE
    }

    /** Defines the status of an HTTP request.*/
    public enum HttpStatus{
        UNKNOWN_STATUS(-1),

        //1xx - informational
        CONTINUE(100),
        SWITCHING_PROTOCOLS(101),
        PROCESSING(102),
        EARLY_HINTS(103),

        //2xx - success
        OK(200),
        CREATED(201),
        ACCEPTED(202),
        NON_AUTHORITATIVE_INFORMATION(203),
        NO_CONTENT(204),
        RESET_CONTENT(205),
        PARTIAL_CONTENT(206),
        MULTI_STATUS(207),
        ALREADY_REPORTED(208),
        IM_USED(226),

        //3xx - redirects
        MULTIPLE_CHOICES(300),
        MOVED_PERMANENTLY(301),
        MOVED_TEMPORARILY(302),
        SEE_OTHER(303),
        NOT_MODIFIED(304),
        USE_PROXY(305),
        SWITCH_PROXY(306),
        TEMPORARY_REDIRECT(307),
        PERMANENT_REDIRECT(308),

        //4xx - client error
        BAD_REQUEST(400),
        UNAUTHORIZED(401),
        PAYMENT_REQUIRED(402),
        FORBIDDEN(403),
        NOT_FOUND(404),
        METHOD_NOT_ALLOWED(405),
        NOT_ACCEPTABLE(406),
        PROXY_AUTHENTICATION_REQUIRED(407),
        REQUEST_TIMEOUT(408),
        CONFLICT(409),
        GONE(410),
        LENGTH_REQUIRED(411),
        PRECONDITION_FAILED(412),
        REQUEST_TOO_LONG(413),
        REQUEST_URI_TOO_LONG(414),
        UNSUPPORTED_MEDIA_TYPE(415),
        REQUESTED_RANGE_NOT_SATISFIABLE(416),
        EXPECTATION_FAILED(417),
        IM_A_TEAPOT(418),
        INSUFFICIENT_SPACE_ON_RESOURCE(419),
        METHOD_FAILURE(420),
        MISDIRECTED_REQUEST(421),
        UNPROCESSABLE_ENTITY(422),
        LOCKED(423),
        FAILED_DEPENDENCY(424),
        TOO_EARLY(425),
        UPGRADE_REQUIRED(426),
        PRECONDITION_REQUIRED(428),
        TOO_MANY_REQUESTS(429),
        REQUEST_HEADER_FIELDS_TOO_LARGE(431),
        UNAVAILABLE_FOR_LEGAL_REASONS(451),

        //5xx - server error
        INTERNAL_SERVER_ERROR(500),
        NOT_IMPLEMENTED(501),
        BAD_GATEWAY(502),
        SERVICE_UNAVAILABLE(503),
        GATEWAY_TIMEOUT(504),
        HTTP_VERSION_NOT_SUPPORTED(505),
        VARIANT_ALSO_NEGOTIATES(506),
        INSUFFICIENT_STORAGE(507),
        LOOP_DETECTED(508),
        NOT_EXTENDED(510),
        NETWORK_AUTHENTICATION_REQUIRED(511);

        private static IntMap<HttpStatus> byCode;

        public final int code;

        HttpStatus(int code){
            this.code = code;
        }

        /** Find an HTTP status enum by code. */
        public static synchronized HttpStatus byCode(int code){
            if(byCode == null){
                byCode = new IntMap<>();
                for(HttpStatus status : HttpStatus.values()){
                    byCode.put(status.code, status);
                }
            }
            return byCode.get(code, UNKNOWN_STATUS);
        }
    }
}

