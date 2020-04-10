package org.cloudfoundry.identity.uaa.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.View;

/** @author Dave Syer */
public class ConvertingExceptionView implements View {

  private static final Logger logger = LoggerFactory.getLogger(ConvertingExceptionView.class);
  private final HttpMessageConverter<?>[] messageConverters;
  private ResponseEntity<? extends ExceptionReport> responseEntity;

  public ConvertingExceptionView(
      ResponseEntity<? extends ExceptionReport> responseEntity,
      HttpMessageConverter<?>[] messageConverters) {
    this.responseEntity = responseEntity;
    this.messageConverters = messageConverters;
  }

  @Override
  public String getContentType() {
    return MediaType.ALL_VALUE;
  }

  @Override
  public void render(
      Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
    try {
      HttpInputMessage inputMessage = createHttpInputMessage(request);
      HttpOutputMessage outputMessage = createHttpOutputMessage(response);
      handleHttpEntityResponse(responseEntity, inputMessage, outputMessage);
    } catch (Exception invocationEx) {
      logger.error("Invoking request method resulted in exception", invocationEx);
    }
  }

  /**
   * Template method for creating a new HttpInputMessage instance.
   *
   * <p>The default implementation creates a standard {@link ServletServerHttpRequest}. This can be
   * overridden for custom {@code HttpInputMessage} implementations
   *
   * @param servletRequest current HTTP request
   * @return the HttpInputMessage instance to use
   */
  protected HttpInputMessage createHttpInputMessage(HttpServletRequest servletRequest) {
    return new ServletServerHttpRequest(servletRequest);
  }

  /**
   * Template method for creating a new HttpOuputMessage instance.
   *
   * <p>The default implementation creates a standard {@link ServletServerHttpResponse}. This can be
   * overridden for custom {@code HttpOutputMessage} implementations
   *
   * @param servletResponse current HTTP response
   * @return the HttpInputMessage instance to use
   */
  protected HttpOutputMessage createHttpOutputMessage(HttpServletResponse servletResponse) {
    return new ServletServerHttpResponse(servletResponse);
  }

  private void handleHttpEntityResponse(
      ResponseEntity<? extends ExceptionReport> responseEntity,
      HttpInputMessage inputMessage,
      HttpOutputMessage outputMessage)
      throws Exception {
    if (outputMessage instanceof ServerHttpResponse) {
      ((ServerHttpResponse) outputMessage).setStatusCode(responseEntity.getStatusCode());
    }
    if (responseEntity.getBody() != null) {
      writeWithMessageConverters(responseEntity.getBody(), inputMessage, outputMessage);
    } else {
      // flush headers
      outputMessage.getBody();
    }
  }

  @SuppressWarnings("unchecked")
  private void writeWithMessageConverters(
      Object returnValue, HttpInputMessage inputMessage, HttpOutputMessage outputMessage)
      throws IOException, HttpMediaTypeNotAcceptableException {
    List<MediaType> acceptedMediaTypes = inputMessage.getHeaders().getAccept();
    if (acceptedMediaTypes.isEmpty()) {
      acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
    }
    MediaType.sortByQualityValue(acceptedMediaTypes);
    Class<?> returnValueType = returnValue.getClass();
    List<MediaType> allSupportedMediaTypes = new ArrayList<MediaType>();
    if (messageConverters != null) {
      for (MediaType acceptedMediaType : acceptedMediaTypes) {
        for (@SuppressWarnings("rawtypes")
        HttpMessageConverter messageConverter : messageConverters) {
          if (messageConverter.canWrite(returnValueType, acceptedMediaType)) {
            messageConverter.write(returnValue, acceptedMediaType, outputMessage);
            if (logger.isDebugEnabled()) {
              MediaType contentType = outputMessage.getHeaders().getContentType();
              if (contentType == null) {
                contentType = acceptedMediaType;
              }
              logger.debug(
                  "Written ["
                      + returnValue
                      + "] as \""
                      + contentType
                      + "\" using ["
                      + messageConverter
                      + "]");
            }
            // this.responseArgumentUsed = true;
            return;
          }
        }
      }
      for (@SuppressWarnings("rawtypes")
      HttpMessageConverter messageConverter : messageConverters) {
        allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
      }
    }
    throw new HttpMediaTypeNotAcceptableException(allSupportedMediaTypes);
  }
}
