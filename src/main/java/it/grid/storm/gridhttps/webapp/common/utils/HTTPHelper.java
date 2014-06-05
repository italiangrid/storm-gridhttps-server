package it.grid.storm.gridhttps.webapp.common.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import org.apache.commons.lang.RandomStringUtils;

public class HTTPHelper {

  static final int BUFFER_SIZE = 1024;

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

  private void encodeBoundaryStart() throws IOException {

    buffer.clear();
    
    buffer.put(DASH_DASH);
    buffer.put(boundary);
    buffer.put(CR_LF);
    buffer.flip();
    write();
    
  }

  private void encodeBoundaryEnd() throws IOException {

    buffer.clear();
    buffer.put(DASH_DASH);
    buffer.put(boundary);
    buffer.put(DASH_DASH);
    buffer.put(CR_LF);
    buffer.flip();
    write();
  }

  private void encodeHeader(String headerName, String headerValue)
    throws IOException {

    buffer.clear();
    buffer.put(headerName.getBytes(DEFAULT_CHARSET));
    buffer.put(COLON_SPACE);
    buffer.put(headerValue.getBytes(DEFAULT_CHARSET));
    buffer.put(CR_LF);
    buffer.flip();
    write();
  }

  private void write() throws IOException {

    outputChannel.write(buffer);
  }

  public void writeBoundaryStart()
    throws IOException {

    encodeBoundaryStart();

  }

  public void writeBoundaryEnd()
    throws IOException {

    encodeBoundaryEnd();

  }

  private void writeHeader(String headerName, String headerValue) 
    throws IOException {

    encodeHeader(headerName, headerValue);

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

  public static String generateRandomMultipartBoundary() {

    StringBuilder builder = new StringBuilder();

    builder.append("StoRM:");
    builder.append(RandomStringUtils.randomAlphanumeric(32).toUpperCase());

    return builder.toString();
  }
}
