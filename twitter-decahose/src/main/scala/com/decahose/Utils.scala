/**
 * (C) Copyright IBM Corp. 2015, 2015
 *
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
 *
 */
package com.decahose

import java.io.{PrintWriter, StringWriter}

object Utils {

  def printException(thr: Throwable, module: String) =
  {
    println("Exception on: " + module)
    val content = new StringWriter
    thr.printStackTrace(new PrintWriter(content))
    content.append("Caused by:")
    if (thr.getCause != null){
      thr.getCause.printStackTrace(new PrintWriter(content))
    }
    println(content.toString)
  }
}
