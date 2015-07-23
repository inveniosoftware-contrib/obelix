package web;

import static spark.Spark.before;
import static spark.Spark.halt;

/**
 * Web Auth for Obelix REST API.
 */
public final class ObelixWebAuth {

    private ObelixWebAuth() {
    }

    public static final int STATUS_CODE_FORBIDDEN = 401;

    public static void enableCORS(final String origin, final String methods, final String headers) {
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
        });
    }

    public static void requireValidToken() {
        before((request, response) -> halt(STATUS_CODE_FORBIDDEN, "403 forbidden"));
    }
}
