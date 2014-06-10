package it.grid.storm.gridhttps.webapp.rangeutils;

import io.milton.http.AbstractResponse;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.Http11ResponseHandler;
import io.milton.http.http11.PartialGetHelper;
import io.milton.resource.GetableResource;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormPartialGetHelper extends PartialGetHelper {

  public static final Logger log = LoggerFactory
    .getLogger(StormPartialGetHelper.class);

  @Override
  public void sendPartialContent(GetableResource resource, Request request,
    Response response, List<Range> ranges, Map<String, String> params,
    Http11ResponseHandler responseHandler) throws NotAuthorizedException,
    BadRequestException, IOException, NotFoundException {

    if (ranges.size() == 1) {
      super.sendPartialContent(resource, request, response, ranges, params,
        responseHandler);
    } else {
      
      String boundary = HTTPHelper.generateRandomMultipartBoundary("StoRM:",4);
      
      AbstractResponse res = (AbstractResponse) response;
      
      res.setNonStandardHeader("Accept-ranges", "bytes");
      res.setContentTypeHeader("multipart/byteranges; boundary="+boundary); 
      
      response.setStatus(Response.Status.SC_PARTIAL_CONTENT);
      
      String contentType = resource.getContentType(request.getAcceptHeader());
      
      if (contentType == null)
        contentType = "text/plain";
      
      StormPartialEntity spe = new StormPartialEntity(ranges, 
        resource, 
        boundary, 
        contentType);
      
      long contentLength = spe.getContentLenght();
      response.setContentLengthHeader(contentLength);
      response.setDateHeader(new Date());
      response.setEntity(spe);
      
    }
  }
}
