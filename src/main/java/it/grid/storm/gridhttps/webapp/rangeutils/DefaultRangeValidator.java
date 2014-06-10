package it.grid.storm.gridhttps.webapp.rangeutils;

import io.milton.http.Range;
import io.milton.resource.GetableResource;

import java.util.List;

public class DefaultRangeValidator implements RangeValidator {

  public void validateRanges(List<Range> ranges, GetableResource resource)
    throws InvalidRangeError {

    if (ranges == null) {
      throw new NullPointerException("Please provide a non-null list of ranges");
    }

    for (Range r : ranges) {

      if (r.getFinish() >= resource.getContentLength()
        || r.getStart() >= resource.getContentLength()) {

        final String msg = String.format(
          "Requested range is not contained in resource content."
            + " Range: %s. Resource content length: %d", r,
          resource.getContentLength());

        throw new InvalidRangeError(msg);
      }

    }
  }
}
