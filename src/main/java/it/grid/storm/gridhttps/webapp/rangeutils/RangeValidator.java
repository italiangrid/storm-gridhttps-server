package it.grid.storm.gridhttps.webapp.rangeutils;

import io.milton.http.Range;
import io.milton.resource.GetableResource;

import java.util.List;

/**
 * HTTP range validator.
 * 
 * @author andreaceccanti
 *
 */
public interface RangeValidator {

  /**
   * Validates a list of byte ranges can be satisfied for a given
   * {@link GetableResource}.
   * 
   * 
   * @param ranges
   * @param resource
   * @throws InvalidRangeError, if an invalid range is found.
   */
  public void validateRanges(List<Range> ranges, GetableResource resource)
    throws InvalidRangeError;
  

}