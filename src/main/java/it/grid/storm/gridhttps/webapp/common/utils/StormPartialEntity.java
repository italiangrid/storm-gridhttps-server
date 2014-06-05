package it.grid.storm.gridhttps.webapp.common.utils;

import io.milton.http.Range;
import io.milton.http.Response;
import io.milton.resource.GetableResource;

import java.io.OutputStream;
import java.util.List;

public class StormPartialEntity implements Response.Entity {

  private final String contentType;

  private final List<Range> ranges;
  private final GetableResource resource;

  private final String boundary;
  

  public StormPartialEntity(List<Range> ranges, GetableResource resource,
    String boundary, String contentType) {

    this.ranges = ranges;
    this.resource = resource;
    this.boundary = boundary;
    this.contentType = contentType;
  }

  /**
   * Validates ranges applicability to current resource
   * 
   * @return <code>true</code> if ranges are valid, <code>false</code> otherwise
   */
  protected boolean validRanges() {

    return true;
  }


  private String rangeByteInfo(Range r) {

    Long rangeStart = r.getStart();
    Long rangeEnd = r.getFinish();

    if (rangeEnd == null) {
      rangeEnd = resource.getContentLength() - 1;
    }

    return String.format("%d-%d/%d", rangeStart, rangeEnd,
      resource.getContentLength());

  }

  

  public void write(Response response, OutputStream os)
    throws Exception {

    final HTTPHelper httpHelper = new HTTPHelper(boundary,os);
    
    if (!validRanges()) {
      return;
    }

    try{
      
      httpHelper.writeNewline();
      
      for (Range r : ranges) {
        
        
        httpHelper.writeBoundaryStart();
        httpHelper.writeContentTypeHeader(contentType);
        httpHelper.writeContentRangeHeader(rangeByteInfo(r));
        httpHelper.writeNewline();
        
        resource.sendContent(os, r, null, contentType);
        
        httpHelper.writeNewline();
        
      }
      
      httpHelper.writeBoundaryEnd();
    
    }catch(Throwable t){
      response.sendError(Response.Status.SC_INTERNAL_SERVER_ERROR, 
        "Error handling multirange partial get: " + t.getMessage());
    }
  }

}
