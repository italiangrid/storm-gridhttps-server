package it.grid.storm.gridhttps.webapp.common;

import io.milton.config.HttpManagerBuilder;
import io.milton.http.http11.DefaultHttp11ResponseHandler.BUFFERING;
import io.milton.property.PropertySource;
import it.grid.storm.gridhttps.webapp.StormStandardFilter;
import it.grid.storm.gridhttps.webapp.rangeutils.StoRMResponseHandler;

import java.util.ArrayList;


public class StoRMHttpManagerBuilder extends HttpManagerBuilder {
  
  public StoRMHttpManagerBuilder() {
    // Creates a custom response handler to properly handle
    // partial get requests.
    
    setHttp11ResponseHandler(new StoRMResponseHandler(
      getAuthenticationService(),
      geteTagGenerator(), 
      getContentGenerator()));
    
    setDefaultStandardFilter(new StormStandardFilter());
    
    setEnabledJson(false);
    
    setBuffering(BUFFERING.never);
    
    setEnableBasicAuth(false);
    setEnableCompression(false);
    setEnableExpectContinue(false);
    setEnableFormAuth(false);
    setEnableCookieAuth(false);
    setPropertySources(new ArrayList<PropertySource>());
  }

  
}
