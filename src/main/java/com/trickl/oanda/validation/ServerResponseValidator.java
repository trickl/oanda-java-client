package com.trickl.oanda.validation;

import java.text.MessageFormat;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ServerWebInputException;

@RequiredArgsConstructor
public class ServerResponseValidator {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  /**
   * Validate a server response.
   * @param <T> The type of response
   * @param response the response to validate
   * @throws ServerWebInputException if validation fails
   */
  public <T> void validate(T response) {
    Set<ConstraintViolation<T>> violations = validator.validate(response);
    if (!violations.isEmpty()) {
      throw new ServerWebInputException(formatConstraintErrorMessage(violations));
    }
  }

  private static <T> String formatConstraintErrorMessage(Set<ConstraintViolation<T>> violations) {
    return violations.stream()
        .map(
            violation -> MessageFormat.format(
                  "At {0} {1} but got {2}}",
                  new Object[] {
                    violation.getPropertyPath(), violation.getMessage(), violation.getInvalidValue()
                  })
            )
        .collect(Collectors.joining(","));
  }
}
