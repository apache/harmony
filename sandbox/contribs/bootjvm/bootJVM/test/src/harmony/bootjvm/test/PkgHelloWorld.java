/*!
 * @file PkgHelloWorld.java
 *
 * @brief Test class for checking if a packaged class can be
 * properly processed by the class loader.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/PkgHelloWorld.java $ \$Id: PkgHelloWorld.java 0 09/28/2005 dlydick $
 *
 * Copyright 2005 The Apache Software Foundation
 * or its licensors, as applicable.
 *
 * Licensed under the Apache License, Version 2.0 ("the License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * @version \$LastChangedRevision: 0 $
 *
 * @date \$LastChangedDate: 09/28/2005 $
 *
 * @author \$LastChangedBy: dlydick $
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

package harmony.bootjvm.test;

/*!
 * @brief Simple hello world program, but in a package.
 *
 */
public class PkgHelloWorld
{

    public static void main(String[] args)
    {
        System.out.println("Hello, packaged world!");
        System.exit(7);
    }

} /* END of harmony.bootjvm.test.PkgHelloWorld */


/* EOF */
