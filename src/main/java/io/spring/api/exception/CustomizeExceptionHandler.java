package io.spring.api.exception;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import java.io.IOException;
import java.time.Instant;

@RestControllerAdvice
public class CustomizeExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler({InvalidRequestException.class})
  public ResponseEntity<Object> handleInvalidRequest(RuntimeException e, WebRequest request) {
    InvalidRequestException ire = (InvalidRequestException) e;

    List<FieldErrorResource> errorResources =
        ire.getErrors().getFieldErrors().stream()
            .map(
                fieldError ->
                    new FieldErrorResource(
                        fieldError.getObjectName(),
                        fieldError.getField(),
                        fieldError.getCode(),
                        fieldError.getDefaultMessage()))
            .collect(Collectors.toList());

    ErrorResource error = new ErrorResource(errorResources);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    return handleExceptionInternal(e, error, headers, UNPROCESSABLE_ENTITY, request);
  }

  @ExceptionHandler(InvalidAuthenticationException.class)
  public ResponseEntity<Object> handleInvalidAuthentication(
      InvalidAuthenticationException e, WebRequest request) {
    return ResponseEntity.status(UNPROCESSABLE_ENTITY)
        .body(
            new HashMap<String, Object>() {
              {
                put("message", e.getMessage());
              }
            });
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    List<FieldErrorResource> errorResources =
        e.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new FieldErrorResource(
                        fieldError.getObjectName(),
                        fieldError.getField(),
                        fieldError.getCode(),
                        fieldError.getDefaultMessage()))
            .collect(Collectors.toList());

    return ResponseEntity.status(UNPROCESSABLE_ENTITY).body(new ErrorResource(errorResources));
  }

  @ExceptionHandler({ConstraintViolationException.class})
  @ResponseStatus(UNPROCESSABLE_ENTITY)
  @ResponseBody
  public ErrorResource handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request) {
    List<FieldErrorResource> errors = new ArrayList<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      FieldErrorResource fieldErrorResource =
          new FieldErrorResource(
              violation.getRootBeanClass().getName(),
              getParam(violation.getPropertyPath().toString()),
              violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
              violation.getMessage());
      errors.add(fieldErrorResource);
    }

    return new ErrorResource(errors);
  }

  private String getParam(String s) {
    String[] splits = s.split("\\.");
    if (splits.length == 1) {
      return s;
    } else {
      return String.join(".", Arrays.copyOfRange(splits, 2, splits.length));
    }
  }

  @Bean
  public FilterRegistrationBean<LoggingFilter> loggingFilter() {
    FilterRegistrationBean<LoggingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new LoggingFilter());
    registrationBean.addUrlPatterns("/*");
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registrationBean;
  }

  static class LoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      long startTime = System.currentTimeMillis();
      chain.doFilter(request, response);
      long elapsed = System.currentTimeMillis() - startTime;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      String timestamp = Instant.now().toString();
      logger.info("{} {} {} {} {}ms", timestamp, httpRequest.getMethod(), httpRequest.getRequestURI(),
          httpResponse.getStatus(), elapsed);
    }

    @Override
    public void destroy() {
    }
  }
}
