# Licensed to the Apache Software Foundation (ASF)
# see the full text at the end of file

SOURCES = $(wildcard *test.cpp test*.cpp)
PROGRAMS = $(SOURCES:%.cpp=%.exe)
INCLUDES = ../../../vmcore/src/jvmti .

all: $(PROGRAMS)
	set -e; for i in $(PROGRAMS); do ./$$i -Xwarn; done

clean:
	$(RM) $(PROGRAMS)

CPPFLAGS = $(addprefix -I,$(INCLUDES))

OS = $(shell uname -s)
ifeq ($(OS),Linux)
%.exe: %.cpp
	$(CXX) $(CXXFLAGS) -o $@ $< $(CPPFLAGS)
else
CXX = cl
%.exe: %.cpp
	$(CXX) $(CXXFLAGS) -EHsc $< $(CPPFLAGS)
endif

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
