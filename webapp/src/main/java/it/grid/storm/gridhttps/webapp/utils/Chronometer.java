package it.grid.storm.gridhttps.webapp.utils;

public final class Chronometer{
	
	public class ElapsedTime {
		private long millis;
		private long secs;
		private long mins;
		private long hrs;
		public ElapsedTime(long milliseconds) {
			millis = milliseconds % 1000;
			secs = milliseconds / 1000 % 60;
			mins = milliseconds / 1000 / 60 % 60;
			hrs = milliseconds / 1000 / 60 / 60 % 60;
		}
		public long getMilliseconds() {
			return millis;
		}
		public long getSeconds() {
			return secs;
		}
		public long getMinutes() {
			return mins;
		}
		public long getHours() {
			return hrs;
		}
	}
	
    private long begin, end;
 
    public void start(){
        begin = System.currentTimeMillis();
    }
 
    public void stop(){
        end = System.currentTimeMillis();
    }
 
    public long getTime() {
        return end-begin;
    }
 
    public long getMilliseconds() {
        return end-begin;
    }
 
    public double getSeconds() {
        return (end - begin) / 1000.0;
    }
 
    public double getMinutes() {
        return (end - begin) / 60000.0;
    }
 
    public double getHours() {
        return (end - begin) / 3600000.0;
    }
    
    public ElapsedTime getElapsedTime() {
    	return new ElapsedTime(end-begin);    	
    }
 
}