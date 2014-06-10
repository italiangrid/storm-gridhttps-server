package it.grid.storm.gridhttps.webapp.rangeutils;

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
  
  private long computeHeaderLength(Range r){
   
    return 12 + 2 + contentType.length() + 2 // Content-Type: <ct>CRLF
      + 13 + 2 + rangeByteInfo(r).length() + 2; // Content-range: <...>CRLF
  }
  
  private long computeRangeLength(){
    
    if (ranges == null || ranges.size() == 0) 
      return 0;
    
    long lengthSoFar = 0L;
    long headerLengthSoFar = 0L;
      
    for (Range r: ranges){
      Long length = r.getLength();
      headerLengthSoFar+= computeHeaderLength(r);
      
      if (length != null && length <= resource.getContentLength()){
        lengthSoFar += length;
      } else {
        
        if (resource.getContentLength() > 0){
          lengthSoFar += resource.getContentLength() - r.getStart() + 1;
        } else {
          continue;
        }
      }
    }
    
    return lengthSoFar+headerLengthSoFar;
  }
    
  public long getContentLenght(){
    int numRanges = ranges.size();
    
    // The content is encoded as follows:
    //    CRLF (2)
    // 
    // then, for each range: 
    //    --<boundary>CRLF
    //    Content-type: ... CRLF
    //    Content-Range: ... CRLF
    //    CRLF
    //    <data>
    //    CRLF
    // and finally
    //    --<boundary>--CRLF
    
    return 2 
      + numRanges*(4+boundary.length()+4)
      + computeRangeLength() // the actual data + headers
      + (6+boundary.length()); // --<boundary>--CRLF
    
  }

}
