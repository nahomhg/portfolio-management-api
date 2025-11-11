package io.github.nahomgh.portfolio.exceptions;

import java.util.List;

public record ValidationsErrorResponse (String message, List<String> errors){}
