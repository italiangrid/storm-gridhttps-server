package it.grid.storm.gridhttps.webapp.rangeutils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import org.apache.commons.lang.RandomStringUtils;

public class HTTPHelper {

  static final int BUFFER_SIZE = 1024;
  static final int DEFAULT_BOUNDARY_SIZE = 4;

  static final String CONTENT_TYPE = "Content-type";
  static final String CONTENT_RANGE = "Content-range";
  
  static final Charset DEFAULT_CHARSET = Charset.forName("US-ASCII");
  
  static final byte[] CR_LF =
    { '\r', '\n' };
  
  static final byte[] DASH_DASH =
    { '-', '-' };
  
  static final byte[] COLON_SPACE =
    { ':', ' ' };

  private final byte[] boundary;
  
  private final ByteBuffer buffer;
  private final WritableByteChannel outputChannel;

  public HTTPHelper(String boundaryString, OutputStream os) {

    buffer = ByteBuffer.allocate(BUFFER_SIZE);
    outputChannel = Channels.newChannel(os);
    boundary = boundaryString.getBytes(DEFAULT_CHARSET);
  }

  private void write() throws IOException {

    outputChannel.write(buffer);
  }

  public void writeBoundaryStart()
    throws IOException {

    buffer.clear();
    
    buffer.put(DASH_DASH);
    buffer.put(boundary);
    buffer.put(CR_LF);
    buffer.flip();
    write();

  }

  public void writeBoundaryEnd()
    throws IOException {

    buffer.clear();
    buffer.put(DASH_DASH);
    buffer.put(boundary);
    buffer.put(DASH_DASH);
    buffer.put(CR_LF);
    buffer.flip();
    write();

  }

  private void writeHeader(String headerName, String headerValue) 
    throws IOException {

    buffer.clear();
    buffer.put(headerName.getBytes(DEFAULT_CHARSET));
    buffer.put(COLON_SPACE);
    buffer.put(headerValue.getBytes(DEFAULT_CHARSET));
    buffer.put(CR_LF);
    buffer.flip();
    write();
  
  }

  public void writeContentTypeHeader(String contentType)
    throws IOException {

    writeHeader("Content-type", contentType);

  }

  public void writeContentRangeHeader(String rangeValue)
    throws IOException {

    writeHeader("Content-range", rangeValue);

  }

  public void writeNewline() throws IOException {
    buffer.clear();
    buffer.put(CR_LF);
    buffer.flip();
    write();
  }

  
  public static String generateRandomMultipartBoundary(String prefix, int size) {

    final StringBuilder builder = new StringBuilder();
    
    if (prefix != null && prefix.length() > 0)
      builder.append(prefix);
    
    if (size <= 0){
      size = DEFAULT_BOUNDARY_SIZE;
    }
    
    builder.append(RandomStringUtils.randomAlphanumeric(size).toUpperCase());
    
    return builder.toString();
  }
}
