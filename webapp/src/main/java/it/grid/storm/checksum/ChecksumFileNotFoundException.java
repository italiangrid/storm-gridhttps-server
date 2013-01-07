/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.grid.storm.checksum;

public class ChecksumFileNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 9148740822736185248L;

    public ChecksumFileNotFoundException() {
        // TODO Auto-generated constructor stub
    }

    public ChecksumFileNotFoundException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public ChecksumFileNotFoundException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public ChecksumFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}