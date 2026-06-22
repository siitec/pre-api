package mx.tsj.connect.general.web.support;

public final class ApiResponses {
    private ApiResponses() {
    }

    public static ErrorResponse error(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, null));
    }
}
