/*!
 * @file MainArgs.java
 *
 * @brief Test class for checking args[] array parameter to main()
 *
 *
 * @section Control
 *
 * \$URL$ \$Id$
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
 * @version \$LastChangedRevision$
 *
 * @date \$LastChangedDate$
 *
 * @author \$LastChangedBy$
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

package harmony.bootjvm.test;

/*!
 * @brief Test ability of main() to recognize args[] array.
 *
 */
public class MainArgs
{

    public static void main(String[] args)
    {
        System.out.println("args[0] = /" + args[0] +
                         "/ args[1] = /" + args[1] +
                         "/ args[2] = /" + args[2] +
                         "/ args[3] = /" + args[3]);
        System.exit(3);
    }

} /* END of harmony.bootjvm.test.MainArgs */


/* EOF */
