# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#  
#      http://www.apache.org/licenses/LICENSE-2.0
#  
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

#
# Configuration Makefile
#

CFLAGS := $(DEFINES) $(INCLUDES) $(OPT) $(PLATFORM) $(CFLAGS) $(WARNFLAGS)
CXXFLAGS := $(DEFINES) $(INCLUDES) $(OPT) $(PLATFORM) $(CXXFLAGS) $(WARNFLAGS)
EXPFILE = $(HY_BIN)$(notdir $(basename $(DLLNAME))).exp

BUILDFILES := $(addprefix $(HY_BIN),$(BUILDFILES))

ifneq ($(HY_OS),zos)
# Convert $(LIBPATH)libblah.so to -L$(LIBPATH) ... -lblah, also for $(DLLPATH)
MDLLIBARGS := \
  $(MDLLIBPREFIX) -L$(LIBPATH) -L$(DLLPATH) \
  $(patsubst $(LIBPATH)lib%$(HY_LINKLIB_SUFFIX),-l%, \
    $(patsubst $(DLLPATH)lib%$(HY_LINKLIB_SUFFIX),-l%, $(MDLLIBFILES))) \
  $(MDLLIBSUFFIX)
else
# Do not change on zOS
MDLLIBARGS := \
  $(MDLLIBPREFIX) $(MDLLIBFILES) $(MDLLIBSUFFIX)
endif

all: $(HY_BIN) $(DLLNAME) $(EXENAME) $(LIBNAME)

$(LIBNAME): $(BUILDFILES)
	$(AR) $(ARFLAGS) $(ARCREATE) $@ $(BUILDFILES)
	$(RANLIB) $@

$(EXPFILE): exports.txt
ifeq ($(HY_OS),aix)
	cp $< $@
else
	echo "$(EXPNAME) {" >$@
	echo "  global :" >>$@
	sed -e's/^/    /;s/$$/;/' <$< >>$@
	echo "  local : *;" >>$@
	echo "};" >>$@
endif

$(DLLNAME): $(BUILDFILES) $(MDLLIBFILES) $(EXPFILE)
	$(DLL_LD) $(DLL_LDFLAGS) $(LDFLAGS) $(VMLINK) -o $@ \
	$(BUILDFILES) $(MDLLIBARGS) $(OSLIBS)
ifeq ($(HY_CAN_LINK_DEBUG),yes)
	objcopy --only-keep-debug $@ $@.dbg
	strip --strip-debug --strip-unneeded $@
	( cd $(@D) && objcopy --add-gnu-debuglink=$(@F).dbg $(@F) )
	-mkdir -p $(DBGPATH)
	test "$@.dbg" = "$(DBGPATH)$(@F).dbg" || \
		mv $@.dbg $(DBGPATH)$(@F).dbg
endif
ifeq ($(HY_OS),zos)
	mv $(notdir $(basename $(DLLNAME)))$(HY_LINKLIB_SUFFIX) $(LIBPATH)
endif

$(EXENAME): $(BUILDFILES) $(MDLLIBFILES)
	$(CC) $(VMLINK) $(EXELDFLAGS) \
	$(BUILDFILES) $(MDLLIBARGS) -o $@ $(OSLIBS) \
	$(EXERPATHPREFIX) -L$(DLLPATH)
	@chmod 755 $(EXENAME)

clean:
	-rm -f $(BUILDFILES) $(DLLNAME) $(EXENAME) $(LIBNAME) $(EXPFILE) \
	       $(CLEANFILES) $(DBGPATH)$(notdir $(DLLNAME)).dbg

$(HY_BIN):
	mkdir -p $(HY_BIN)

# C rules
$(HY_BIN)%.o: $(HY_PLATFORM)/%.c
	$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(HY_ARCH)/%.c
	$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(HY_OS)/%.c
	$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(SHAREDSUB)%.c
	$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(SHAREDSUB)additional/%.c
	$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: %.c
	$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(OSS_DIST)%.c # for zlib_dist / fdlibm_dist
	$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

# C++ rules
$(HY_BIN)%.o: $(HY_ARCH)/%.cpp
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(HY_OS)/%.cpp
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(SHAREDSUB)%.cpp
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: %.cpp
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<


# assembler rules
$(HY_BIN)%.o: $(HY_PLATFORM)/%.s
	$(AS) $(ASFLAGS) -o $@ $<

$(HY_BIN)%.o: $(HY_ARCH)/%.s
	$(AS) $(ASFLAGS) -o $@ $<

$(HY_BIN)%.o: $(HY_OS)/%.s
	$(AS) $(ASFLAGS) -o $@ $<

$(HY_BIN)%.o: $(SHAREDSUB)%.s
	$(AS) $(ASFLAGS) -o $@ $<

$(HY_BIN)%.o: %.s
	$(AS) $(ASFLAGS) -o $@ $<

