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
# Main Makefile
#

all: \
	_sig _common _pool _thread _port _fdlibm _zip \
	_zlib _vmi _luni _nio _archive \
	_math _text _launcher

_pool: 
	(cd pool && $(MAKE) && cd ..)

_common:
	(cd common && $(MAKE) && cd ..)

_thread: _pool _common
	(cd thread && $(MAKE) && cd ..)

_port: _thread _sig
	(cd port && $(MAKE) && cd ..)

_sig:
	(cd sig && $(MAKE) && cd ..)

_fdlibm: 
	(cd fdlibm && $(MAKE) && cd ..)

_zip: 
	(cd zip && $(MAKE) && cd ..)

_zlib: 
	(cd zlib && $(MAKE) && cd ..)

_vmi: _zip _pool
	(cd vmi && $(MAKE) && cd ..)

_luni: _common _sig _zip _zlib _pool _fdlibm _thread _vmi
	(cd luni && $(MAKE) && cd ..)
	
_nio: _sig _zip _zlib _pool _fdlibm _thread _vmi
	(cd nio && $(MAKE) && cd ..)
	
_archive: _common _sig _zip _zlib _pool _fdlibm _thread _vmi
	(cd archive && $(MAKE) && cd ..)
	
_math: _common _sig _zip _zlib _pool _fdlibm _thread _vmi
	(cd math && $(MAKE) && cd ..)
	
_text: _sig _zip _zlib _pool _fdlibm _thread _vmi
	(cd text && $(MAKE) && cd ..)
	
_launcher: _thread _port
	(cd launcher && $(MAKE) && cd ..)	

clean:
	(cd sig && $(MAKE) clean && cd ..)
	(cd pool  && $(MAKE) clean && cd ..)
	(cd thread && $(MAKE) clean && cd ..)
	(cd port && $(MAKE) clean && cd ..)
	(cd fdlibm && $(MAKE) clean && cd ..)
	(cd zip && $(MAKE) clean && cd ..)
	(cd zlib && $(MAKE) clean && cd ..)
	(cd vmi && $(MAKE) clean && cd ..)
	(cd common && $(MAKE) clean && cd ..)
	(cd luni && $(MAKE) clean && cd ..)
	(cd nio && $(MAKE) clean && cd ..)
	(cd archive && $(MAKE) clean && cd ..)
	(cd math && $(MAKE) clean && cd ..)
	(cd text && $(MAKE) clean && cd ..)
	(cd launcher && $(MAKE) clean && cd ..)
