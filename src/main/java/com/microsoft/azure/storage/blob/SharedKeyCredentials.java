package com.microsoft.azure.storage.blob;

import com.microsoft.rest.v2.http.HttpHeader;
import com.microsoft.rest.v2.http.HttpHeaders;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import com.microsoft.rest.v2.policy.RequestPolicy;
import io.netty.handler.codec.http.QueryStringDecoder;
import rx.Single;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.microsoft.azure.storage.blob.Utility.getGMTTime;

public final class SharedKeyCredentials  implements CredentialsInterface {

    private final String accountName;

    private final byte[] key;

    /**
     * Initialized a new instance of SharedKeyCredentials contains an account's name and its primary or secondary key.
     * @param accountName
     *      The account name associated with the request.
     * @param key
     *      A string that represent the account access key.
     */
    public SharedKeyCredentials(String accountName, String key) {
        this.accountName = accountName;
        this.key = key.getBytes();
    }

    /**
     * Gets the account name associated with the request.
     * @return
     *      The account name.
     */
    public String getAccountName() {
        return accountName;
    }

    private final class SharedKeyCredentialsPolicy implements RequestPolicy {

        final RequestPolicy nextPolicy;

        final SharedKeyCredentials factory;

        public SharedKeyCredentialsPolicy(RequestPolicy nextPolicy, SharedKeyCredentials factory) {
            this.nextPolicy = nextPolicy;
            this.factory = factory;
        }

        /**
         * Signed the request
         * @param request
         *      the request to sign
         * @return
         *      A {@link Single} representing the HTTP response that will arrive asynchronously.
         */
        @Override
        public Single<HttpResponse> sendAsync(final HttpRequest request) {
            if (request.headers().value(Constants.HeaderConstants.DATE) == null) {
                request.headers().set(Constants.HeaderConstants.DATE, getGMTTime(new Date()));
            }

            try {
                final String stringToSign = factory.buildStringToSign(request);
                final String computedBase64Signature = factory.computeHmac256(stringToSign);
                request.headers().set(Constants.HeaderConstants.AUTHORIZATION, "SharedKey " + accountName + ":"  + computedBase64Signature);
            } catch (Exception e) {
                return Single.error(e);
            }

            return nextPolicy.sendAsync(request);
        }
    }

    /**
     * Creates a new <code>RequestPolicy</code> object to sign requests.
     * @param nextPolicy
     *      A <code>RequestPolicy</code> to execute before and after signing the request.
     * @return
     *      A <code>RequestPolicy</code> which includes the signing policy.
     */
    @Override
    public RequestPolicy create(RequestPolicy nextPolicy) {
        return new SharedKeyCredentialsPolicy(nextPolicy, this);
    }

    /**
     * Constructs a canonicalized string for signing a request.
     *
     * @param request
     *  the request to canonicalize
     * @return a canonicalized string.
     */
    private String buildStringToSign(final HttpRequest request) throws Exception {
        final HttpHeaders httpHeaders = request.headers();
        String contentLength = getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_LENGTH);
        contentLength = contentLength.equals("0") ? Constants.EMPTY_STRING : contentLength;


        return String.join(
                "/n",
                request.httpMethod(),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_ENCODING),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_LANGUAGE),
                contentLength,
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_MD5),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_TYPE),
                // x-ms-date header exists, so don't sign date header
                Constants.EMPTY_STRING,
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.IF_MODIFIED_SINCE),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.IF_MATCH),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.IF_NONE_MATCH),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.IF_UNMODIFIED_SINCE),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.RANGE),
                getAdditionalXmsHeaders(httpHeaders),
                getCanonicalizedResource(request.url())
        );
    }

    private void appendCanonicalizedElement(final StringBuilder builder, final String element) {
        builder.append("\n");
        builder.append(element);
    }

    private String getAdditionalXmsHeaders(final HttpHeaders headers) {

        // Add only headers that begin with 'x-ms-'
        final ArrayList<String> xmsHeaderNameArray = new ArrayList<String>();
        for (HttpHeader header : headers) {
            String lowerCaseHeader = header.name().toLowerCase(Utility.LOCALE_US);
            if (lowerCaseHeader.startsWith(Constants.PREFIX_FOR_STORAGE_HEADER)) {
                xmsHeaderNameArray.add(lowerCaseHeader);
            }
        }

        if (xmsHeaderNameArray.isEmpty()) {
            return Constants.EMPTY_STRING;
        }

        Collections.sort(xmsHeaderNameArray);

        final StringBuilder canonicalizedHeaders = new StringBuilder();
        for (final String key : xmsHeaderNameArray) {
            if (canonicalizedHeaders.length() > 0) {
                canonicalizedHeaders.append('\n');
            }

            canonicalizedHeaders.append(key);
            canonicalizedHeaders.append(':');
            canonicalizedHeaders.append(headers.value(key));
        }

        return canonicalizedHeaders.toString();
    }

    /**
     * Canonicalized the resource to sign.
     * @param requestURL
     *      A string that represents the request URL.
     * @return
     *      The canonicalized resource to sign.
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException
     */
    private String getCanonicalizedResource(String requestURL) throws MalformedURLException, UnsupportedEncodingException {
        requestURL = Utility.safeDecode(requestURL);
        // Resource path
        final StringBuilder canonicalizedResource = new StringBuilder("/");
        canonicalizedResource.append(this.accountName);

        // Note that AbsolutePath starts with a '/'.
        QueryStringDecoder urlDecoder = new QueryStringDecoder(requestURL);
        if (urlDecoder.path().length() > 0) {
            canonicalizedResource.append(urlDecoder.path());
        }
        else {
            canonicalizedResource.append('/');
        }

        // check for no query params and return
        Map<String, List<String>> queryParams = urlDecoder.parameters();
        if (queryParams.size() == 0) {
            canonicalizedResource.append('\n');
            return canonicalizedResource.toString();
        }

        ArrayList<String> queryParamNames = new ArrayList<String>(queryParams.keySet());
        Collections.sort(queryParamNames);

        for (int i = 0; i < queryParamNames.size(); i++) {
            final String queryParamName = queryParamNames.get(i);
            final List<String> queryParamValues = queryParams.get(queryParamName);
            Collections.sort(queryParamValues);

            // concatenation of the query param name + colon + join of query param values which are commas separated
            canonicalizedResource.append("\n" + queryParamName.toLowerCase(Locale.US) + ":" + String.join(",", queryParamValues));

            // if not the last query param append a newline character
//            if (i != queryParamNames.size() - 1) {
//                canonicalizedResource.append('\n');
//            }
        }

        // append to main stringbuilder the join of completed params with new line
        return canonicalizedResource.toString();
    }

    /**
     * Returns the standard header value from the specified connection request, or an empty string if no header value
     * has been specified for the request.
     *
     * @param httpHeaders
     *      A <code>HttpHeaders</code> object that represents the headers for the request.
     * @param headerName
     *      A <code>String</code> that represents the name of the header being requested.
     *
     * @return A <code>String</code> that represents the header value, or <code>null</code> if there is no corresponding
     *      header value for <code>headerName</code>.
     */
    private String getStandardHeaderValue(final HttpHeaders httpHeaders, final String headerName) {
        final String headerValue = httpHeaders.value(headerName);

        return headerValue == null ? Constants.EMPTY_STRING : headerValue;
    }

    /**
     * Computes a signature for the specified string using the HMAC-SHA256 algorithm.
     *
     * @param stringToSign
     *      The UTF-8-encoded string to sign.
     *
     * @return
     *      A <code>String</code> that contains the HMAC-SHA256-encoded signature.
     *
     * @throws InvalidKeyException
     *      If the key is not a valid Base64-encoded string.
     */
    private synchronized String computeHmac256(final String stringToSign) throws InvalidKeyException {
        byte[] utf8Bytes = null;
        try {
            utf8Bytes = stringToSign.getBytes(Constants.UTF8_CHARSET);
        }
        catch (final UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }

        // Initializes the HMAC-SHA256 Mac and SecretKey.
        Mac hmacSha256;
        try {
            hmacSha256 = Mac.getInstance("HmacSHA256");
        }
        catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException();
        }

        hmacSha256.init(new SecretKeySpec(this.key, "HmacSHA256"));
        return Base64.encode(hmacSha256.doFinal(utf8Bytes));
    }
}
