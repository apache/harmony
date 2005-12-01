# Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Makefile for module 'launcher'
#

APPVER=4.0
TARGETOS=WIN95
_WIN32_IE=0x0500
SEHMAP = TRUE

!include <win32.mak>

all: 
	$(MAKE) /NOLOGO -f makefile.javae
	$(MAKE) /NOLOGO -f makefile.javaw

clean:
	$(MAKE) /NOLOGO -f makefile.javae clean
	$(MAKE) /NOLOGO -f makefile.javaw clean
	-del *.obj
	-del vc70.pdb
