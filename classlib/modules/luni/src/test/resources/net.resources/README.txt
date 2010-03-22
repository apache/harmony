README
======



Contents
--------
This directory contains resources that can be used to carry out unit testing of
the java.net types. The resources are separated according to the type of 
server that will host them. 

FTP
---
The FTP folder contains a single text file (nettest.txt) which should be placed
in the root or home directory of an FTP account with id "jcltest" with password
"jclpass". Please do not modify nettest.txt as the tests have been written to 
check on the precise contents of this file.


HTTP
----
The HTTP folder contains files to be deployed on an HTTP server.

Directory cgi-bin contains a single Perl script called test.pl which should be
placed in the normal bin directory for your HTTP server (e.g. "cgi-bin" for the
Apache HTTP server). The intent is that this script can be accessed with the URL
"http://<your host>/cgi-bin/test.pl" .

Directory html/testres231 contains a number of text and binary resources. This
folder and all of its subfolders should be copied to the root directory of your
web server from which static documents are served (e.g. "htdocs" for the Apache
HTTP server)...


<DOCUMENT_ROOT>/testres231
       |   RESOURCE.TXT
       |
       +---JUC
       |       lf.jar
       |
       +---subdir1
       |       RESOURCE.TXT
       |
       +---UCL
       |       UCL.jar
       |
       \---URLConnectionTest
               Harmony.html



The intent is that these resources can be accessed with URLs beginning
"http://<your host>/testres231/" . 

As with the FTP resource, please do not modify these files in any way as the 
unit test code has been written to expect specific information about these
files such as content and date of last modification.


SERVER CONFIGURATION
--------------------
Before running the unit tests for the java.net types the following steps should 
be taken. 

1. Install the FTP and HTTP documents to their respective server locations as
described above. 
2. Enable proxying capability on the HTTP server.
3. Start the HTTP and FTP servers. 
4. Start up a SOCKS server.



RUNNING THE TESTS
-----------------
The success of the java.net tests rely on a number of text values which can be 
supplied in a properties file. These properties include the hostname of the 
HTTP server where the testres231 files are hosted, the network location of the 
FTP and SOCKS servers and so on. Isolating these values in a properties file
enables the tests to be run in any network environment without recourse to 
updating values in the test case source code. 

The location of the properties file can be specified to the running tests
through setting the property "test.ini.file". An example of this is available
in the "run-tests" target of the <EXTRACT_DIR>/Harmony/make/build-java.xml file
contained in this zip file. There the "test.ini.file" property holds the 
path to the file 
<EXTRACT_DIR>/Harmony/Harmony_Tests/src/test/resources/config/localhost.ini
that contains suitable property values if the HTTP, FTP and SOCKS servers were
all running on the local machine of the tests user. 

The key properties required by the java.net tests are as follows ...


* DomainAddress : The domain name of the host where the HTTP server is running.

* WebName : The unqualified name of the host where the HTTP server is running.

* TestResourcesDir : the name of the folder on the HTTP server beneath the
                     document root where the where the web resources have been
                     deployed. 

* HomeAddressResponse : the expected response returned from a successful get 
                        from the HTTP server.

* HomeAddressSoftware : the expected information about the HTTP server's
                        software.
                        
* ProxyServerTestHost : the fully qualified location of the HTTP proxy host 
                        used in the tests.

* SocksServerTestHost : the fully qualified location of the SOCKS server used
                        in the tests.

* SocksServerTestPort : the test SOCKS server's port number

* UnresolvedIP : an IP address that does not resolve to a host name

* InetTestAddress : the string name of a network host with IP address given
                    in the corresponding "InetTestIP" property.

* InetTestIP : the IP address of a network host whose string name is supplied
               in the corresponding "InetTestAddress" property.

* NonLocalAddressBytes : the bytes representing the address of any machine which
                         cannot be bound to during the tests. Used in the socket
                         tests to verify we get the expected error when trying
                         to bind to an address that cannot be reached. 

* InetTestAddress2 : the string name of a network host with IP address given
                    in the corresponding "InetTestIP" property.

* InetTestIP2 : the IP address of a network host whose string name is supplied
               in the corresponding "InetTestAddress" property.

* InetTestHashcode : the expected hashcode value of the "InetTestAddress".

* SpecialInetTestAddress : An address that resolves to more than one IP address.
                           The expected number of IP addresses that this address
                           resolves to should be set in the
                           "SpecialInetTestAddressNumber" property. Used to test
                           the java.net.InetAddress.getAllByName() method. 

* SpecialInetTestAddressNumber : The number of IP addresses that resolve to the
                                 host named in the property
                                 "SpecialInetTestAddress". Used to test the
                                 java.net.InetAddress.getAllByName() method. 

* FTPTestAddress : a string of the form "jcltest:jclpass@<server address>" that
                   points to the network location of the FTP resources.

* URLConnectionLastModifiedString : string which gives the precise date and time
                                    that the HTTP server resources were last
                                    modified. If you make any changes to the
                                    web resources this value will need to
                                    change.

* URLConnectionLastModified : the corresponding numeric equivalent of the 
                              "URLConnectionLastModifiedString" string value. 
                              Any changes to the web resources will affect this
                              value. 

* URLConnectionDate : the expected date when the file
                      "http://<your HTTP host>/testres231/RESOURCE.TXT" was last
                      modified. Making any changes to the RESOURCE.TXT file will
                      mean this property value will require updating.

* InetTestIP6 : an IPv6 address to help test out serialization of the type
                java.net.Inet6Address .

* ResolvedNotExistingHost : an IP address that resolves to a host that is not
                            present on the local network. This allows us to
                            check the timeouts for socket connections.

* InetAlias1 : this property and "InetAlias2" should be different host names
               that resolve to the same IP address.

* InetAlias2 : this property and "InetAlias1" should be different host names
               that resolve to the same IP address.

* IPv6GlobalAddressJcl4 : the IPv6 address for a host on the network. 



If any of these properties are not set the *hard coded* defaults in the class 
tests.support.Support_Configuration will be used (see
<EXTRACT_DIR>/Harmony/Harmony_Tests/src/test/java/tests/support/Support_Configuration.java
in this zip file).

