package it.grid.storm.gridhttps.webapp.rangeutils;

import io.milton.http.AbstractResponse;
import io.milton.http.AuthenticationService;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.Response.Status;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.ContentGenerator;
import io.milton.http.http11.DefaultHttp11ResponseHandler;
import io.milton.http.http11.ETagGenerator;
import io.milton.resource.GetableResource;

import java.util.Date;
import java.util.List;
import java.util.Map;


public class StoRMResponseHandler extends DefaultHttp11ResponseHandler{

  private static final String DEFAULT_CONTENT_TYPE = "text/plain";
  private final RangeValidator rangeValidator = new DefaultRangeValidator();
  
  
  public StoRMResponseHandler(AuthenticationService authenticationService,
    ETagGenerator eTagGenerator, ContentGenerator contentGenerator) {

    super(authenticationService, eTagGenerator, contentGenerator);
    
  }
  
  private void set416ResponseContentRangeHeader(Response response, 
    GetableResource resource){
    
    String headerValue = String.format("*/%d", resource.getContentLength()); 
    
    // Response interface doesn't expose a generic setHeader method
    ((AbstractResponse)response)
      .setResponseHeader(Response.Header.CONTENT_RANGE, headerValue);  
  }
  
  @Override
  public void respondPartialContent(GetableResource resource,
    Response response, Request request, Map<String, String> params,
    List<Range> ranges) throws NotAuthorizedException, BadRequestException,
    NotFoundException {

    String boundary = HTTPHelper.generateRandomMultipartBoundary("StoRM:",4);
        
    try{
      
      rangeValidator.validateRanges(ranges, resource);
      
      response.setStatus(Response.Status.SC_PARTIAL_CONTENT);
      response.setAcceptRanges("bytes");
      response.setContentTypeHeader("multipart/byteranges; boundary="+boundary);
      response.setDateHeader(new Date());
      
      String contentType = resource.getContentType(request.getAcceptHeader());
      
      if (contentType == null)
        contentType = DEFAULT_CONTENT_TYPE;
      
      StormPartialEntity spe = new StormPartialEntity(ranges, 
        resource, 
        boundary, 
        contentType);
      
      long contentLength = spe.getContentLenght();
      
      response.setContentLengthHeader(contentLength);
      response.setEntity(spe);
      
    }catch(InvalidRangeError e){
      set416ResponseContentRangeHeader(response, resource);
      response.sendError(Status.SC_REQUESTED_RANGE_NOT_SATISFIABLE, 
        e.getMessage());
    }
  }  
}
