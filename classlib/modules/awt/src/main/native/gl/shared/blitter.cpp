/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
 
/**
 * @author Igor V. Stolyarov
 */
 
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "org_apache_harmony_awt_gl_render_NativeImageBlitter.h"
#include "org_apache_harmony_awt_gl_Surface.h"
#include "blitter.h"

void (* src_over_blt[14][14])(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);
void (* src_blt[14][14])(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);
void (* xor_blt[14][14])(int, int, SURFACE_STRUCTURE *, void *, int, int, SURFACE_STRUCTURE *, void *, int, int, int);

void src_over_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs = 255, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fd = 255 - MUL(alpha, sa);
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

void src_over_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs = 255, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              fd = 255 - MUL(alpha, _sa);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

void src_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              if(alpha == 0){
                  setRGB(_dx, _dy, dstStruct, dstData, 0, 0, 0, 0, true);
              }else{
                  setRGB(_dx, _dy, dstStruct, dstData, MUL(alpha, sr), MUL(alpha, sg), 
                      MUL(alpha, sb), MUL(alpha, sa), true);
              }
          }
      }
  }

void src_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, fs = 255, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              if(alpha == 0){
                  setRGB(_dx, _dy, dstStruct, dstData, 0, 0, 0, 0, true);
              }else{
                  setRGB(_dx, _dy, dstStruct, dstData, MUL(alpha, _sr), MUL(alpha, _sg), 
                      MUL(alpha, _sb), MUL(alpha, _sa), true);
              }
          }
      }
  }

 void xor_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 255 - da;
              fd = 255 - MUL(alpha, sa);
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void xor_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fs = 255;
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 255 - da;
              fd = 255 - MUL(alpha, _sa);
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void src_atop_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = da;
              fd = 255 - MUL(alpha, sa);
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void src_atop_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fs = 255;
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = da;
              fd = 255 - MUL(alpha, _sa);
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void src_in_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = da;
              fd = 0;
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void src_in_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fs = 255;
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = da;
              fd = 0;
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void src_out_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 255 - da;
              fd = 0;
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void src_out_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fs = 255;
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 255 - da;
              fd = 0;
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void dst_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){
  
  }

 void dst_atop_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 255 - da;
              fd = MUL(alpha, sa);
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void dst_atop_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fs = 255;
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 255 - da;
              fd = MUL(alpha, _sa);
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void dst_in_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 0;
              fd = MUL(alpha, sa);
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void dst_in_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fs = 255;
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 0;
              fd = MUL(alpha, _sa);
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void dst_out_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 0;
              fd = 255 - MUL(alpha, sa);
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void dst_out_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fs = 255;
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 0;
              fd = 255 - MUL(alpha, _sa);
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void dst_over_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs, fd;
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 255 - da;
              fd = 255;
              COMPOSE_EXT(sa, sr, sg, sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void dst_over_custom_bg
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha, int bgcolor){

      unsigned char a, r, g, b, _sa, _sr, _sg, _sb, sr, sg, sb, sa, dr, dg, db, da, fs, fd;
          a = (unsigned char)((bgcolor >> 24) & 0xff);
          r = (unsigned char)((bgcolor >> 16) & 0xff);
          g = (unsigned char)((bgcolor >> 8) & 0xff);
          b = (unsigned char)(bgcolor & 0xff);
          r = MUL(a, r);
          g = MUL(a, g);
          b = MUL(a, b);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
                          _sa = a;
                          _sr = r;
                          _sg = g; 
                          _sb = b;
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, true);
                          fs = 255;
                          fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, _sa, _sr, _sg, _sb, fd);
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
              fs = 255 - da;
              fd = 255;
              COMPOSE_EXT(_sa, _sr, _sg, _sb, fs, da, dr, dg, db, fd, alpha);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, true);
          }
      }
  }

 void clear_custom
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              setRGB(_dx, _dy, dstStruct, dstData, 0, 0, 0, 0, true);
          }
      }
  }

void src_over_intrgb_intargb
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      int sstride, dstride;

          if(alpha == 255){
                  unsigned int *srcPtr = (unsigned int *)srcData;
                  unsigned int *dstPtr = (unsigned int *)dstData;
                  unsigned int *sp, *dp;

                  sstride = srcStruct->scanline_stride;
                  dstride = dstStruct->scanline_stride;

                  srcPtr += srcY * sstride + width + srcX - 1;
                  dstPtr += dstY * dstride + width + dstX - 1;

                  for(int y = height; y > 0; y--, srcPtr += sstride, dstPtr += dstride){
                          sp = srcPtr;
                          dp = dstPtr;
                          for(int x = width; x > 0; x--){
                                  *dp-- = 0xff000000 | *sp--;
                          }
                  }
          }else{
                  unsigned char *srcPtr = (unsigned char *)srcData;
                  unsigned char *dstPtr = (unsigned char *)dstData;
                  unsigned char *sp, *dp, *_dp;

                  sstride = srcStruct->scanline_stride << 2;
                  dstride = dstStruct->scanline_stride << 2;

                  srcPtr += srcY * sstride + (srcX << 2);
                  dstPtr += dstY * dstride + (dstX << 2);

                  int x, y;
                  unsigned char sr, sg, sb, sa, dr, dg, db, da, fs = 255, fd;
                  for(y = 0; y < height; y++, srcPtr += sstride, dstPtr += dstride){
                          sp = srcPtr;
                          dp = dstPtr;
                          _dp = dp;

                          for(x = 0; x < width; x++){
                                  sb = *sp++;
                                  sg = *sp++;
                                  sr = *sp++;
                                  sp++;
                                  sa = MUL(alpha, 255);
                                  sb = MUL(sa, sb);
                                  sg = MUL(sa, sg);
                                  sr = MUL(sa, sr);
                                  db = *dp++;
                                  dg = *dp++;
                                  dr = *dp++;
                                  da = *dp++;
                                  fd = 255 - sa;
                                  COMPOSE(sa, sr, sg, sb, fs, da, dr, dg, db, fd);
                                  db = DIV(da, db);
                                  dg = DIV(da, dg);
                                  dr = DIV(da, dr);
                                  *_dp++ = db;
                                  *_dp++ = dg;
                                  *_dp++ = dr;
                                  *_dp++ = da;
                          }
                  }
          }
  }

void src_over_intrgb_intrgb
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

          unsigned int *srcPtr = (unsigned int *)srcData;
          unsigned int *dstPtr = (unsigned int *)dstData;
          
          int sstride = srcStruct->scanline_stride;
          int dstride = dstStruct->scanline_stride;
          srcPtr += srcY * sstride + srcX;
          dstPtr += dstY * dstride + dstX;
          int bytes = width << 2;
          if(alpha != 0){
                  for(int i = height; i > 0; i--, srcPtr += sstride, dstPtr += dstride){
                          memcpy((void *)dstPtr, (void *)srcPtr, bytes);
                  }
          }else{
                  for(int i = height; i > 0; i--, dstPtr += dstride){
                          memset(dstPtr, 0, width);
                  }
          }
  }

void src_over_intargb_intrgb
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char *srcPtr = (unsigned char *)srcData;
      unsigned char *dstPtr = (unsigned char *)dstData;
      unsigned char *sp, *dp, *_dp;

      int sstride = srcStruct->scanline_stride << 2;
      int dstride = dstStruct->scanline_stride << 2;

      srcPtr += srcY * sstride + ((width + srcX) << 2) - 1;
      dstPtr += dstY * dstride + ((width + dstX) << 2) - 1;

      int x, y;
      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs = 255, fd;
      for(y = height; y > 0 ; y--, srcPtr += sstride, dstPtr += dstride){
          sp = srcPtr;
          dp = _dp = dstPtr;

          for(x = width; x > 0; x--){
              sa = *sp--;
              sr = *sp--;
              sg = *sp--;
              sb = *sp--;
              sa = MUL(alpha, sa);
                         if(sa != 255){
                                 sb = MUL(sa, sb);
                                 sg = MUL(sa, sg);
                                 sr = MUL(sa, sr);
                         }
              dp--;
              da = 255;
              dr = *dp--;
              dg = *dp--;
              db = *dp--;
              fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, da, dr, dg, db, fd);
                         if(da != 255){
                                 *_dp-- = 255;
                                 *_dp-- = DIV(da, dr);
                                 *_dp-- = DIV(da, dg);
                                 *_dp-- = DIV(da, db);
                         }else{
                                 *_dp-- = 255;
                                 *_dp-- = dr;
                                 *_dp-- = dg;
                                 *_dp-- = db;
                         }
          }
      }
  }

void src_over_intargb_intargb
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      unsigned char *srcPtr = (unsigned char *)srcData;
      unsigned char *dstPtr = (unsigned char *)dstData;
      unsigned char *sp, *dp, *_dp;

      int sstride = srcStruct->scanline_stride_byte;
      int dstride = dstStruct->scanline_stride_byte;

      srcPtr += srcY * sstride + ((width + srcX) << 2) - 1;
      dstPtr += dstY * dstride + ((width + dstX) << 2) - 1;

      int x, y;
      unsigned char sr, sg, sb, sa, dr, dg, db, da, fs = 255, fd;
      for(y = height; y > 0; y--, srcPtr += sstride, dstPtr += dstride){
          sp = srcPtr;
          dp = _dp = dstPtr;

          for(x = width; x > 0; x--){
              sa = *sp--;
              sr = *sp--;
              sg = *sp--;
              sb = *sp--;
              sa = MUL(alpha, sa);
                         if(sa != 255){
                                 sb = MUL(sa, sb);
                                 sg = MUL(sa, sg);
                                 sr = MUL(sa, sr);
                         }
              da = *dp--;
              dr = *dp--;
              dg = *dp--;
              db = *dp--;
                         if(da != 255){
                                 db = MUL(da, db);
                                 dg = MUL(da, dg);
                                 dr = MUL(da, dr);
                         }
              fd = 255 - sa;
              COMPOSE(sa, sr, sg, sb, fs, da, dr, dg, db, fd);
              *_dp-- = da;
                         if(da != 255){
                                 *_dp-- = DIV(da, dr);
                                 *_dp-- = DIV(da, dg);
                                 *_dp-- = DIV(da, db);
                         }else{
                                 *_dp-- = dr;
                                 *_dp-- = dg;
                                 *_dp-- = db;
                         }
          }
      }
  }

void src_over_byteindexed_intargb
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int alpha){

      int sstride = srcStruct->scanline_stride;
      int dstride = dstStruct->scanline_stride;

      unsigned char *srcPtr = (unsigned char *)srcData + srcY * sstride + srcX;
      unsigned int *dstPtr = (unsigned int *)dstData + dstY * dstride + dstX;;
      unsigned char *sp;
      unsigned int *dp;

      int transparency = srcStruct->transparency;
      unsigned int sargb, dargb, sr, sg, sb, sa, dr, dg, db, da, fs = 255, fd;
      for(int y = height; y > 0; y--, srcPtr += sstride, dstPtr += dstride){
          sp = srcPtr;
          dp = dstPtr;

          for(int x = width; x > 0; x--){
              unsigned char pixel = *sp++;
              if(transparency == GL_OPAQUE){
                  *dp++ = *(srcStruct->colormap + pixel);
              }else if(transparency == GL_BITMASK){
                  if(pixel != srcStruct->transparent_pixel){
                      *dp++ = *(srcStruct->colormap + pixel);
                  }
              }else {
                  sargb = *(srcStruct->colormap + pixel);
                  dargb = *dp;
                  sa = (sargb >> 24) & 0xff;
                  sr = (sargb >> 16) & 0xff;
                  sg = (sargb >> 8) & 0xff;
                  sb = sargb & 0xff;
                  sa = MUL(alpha, sa);
                  sb = MUL(sa, sb);
                  sg = MUL(sa, sg);
                  sr = MUL(sa, sr);

                  da = (dargb >> 24) & 0xff;
                  dr = (dargb >> 16) & 0xff;
                  dg = (dargb >> 8) & 0xff;
                  db = dargb & 0xff;
                  db = MUL(da, db);
                  dg = MUL(da, dg);
                  dr = MUL(da, dr);
                  fd = 255 - sa;
                  COMPOSE(sa, sr, sg, sb, fs, da, dr, dg, db, fd);
                  db = DIV(da, db);
                  dg = DIV(da, dg);
                  dr = DIV(da, dr);
                  *dp++ = (da << 24) | (dr << 16) | (dg << 8) | db;
              }
          }
      }
  }

 void xor_mode_blt
  (int srcX, int srcY, SURFACE_STRUCTURE *srcStruct, void *srcData,
  int dstX, int dstY, SURFACE_STRUCTURE *dstStruct, void *dstData,
  int width, int height, int xorcolor){

      unsigned char r, g, b, sr, sg, sb, sa, dr, dg, db, da;
      r = (unsigned char)((xorcolor >> 16) & 0xff);
      g = (unsigned char)((xorcolor >> 8) & 0xff);
      b = (unsigned char)(xorcolor & 0xff);
      for(int _sy = srcY, _dy = dstY, maxY = srcY + height; _sy < maxY; _sy++, _dy++){
          for(int _sx = srcX, _dx = dstX, maxX = srcX + width; _sx < maxX; _sx++, _dx++){
              getRGB(_sx, _sy, srcStruct, srcData, sr, sg, sb, sa, false);
              if(sa < 128) continue;
              getRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, false);
              dr ^= (r ^ sr);
              dg ^= (g ^ sg);
              db ^= (b ^ sb);
              setRGB(_dx, _dy, dstStruct, dstData, dr, dg, db, da, false);
          }
      }
  }

void getRGB
  (int x, int y, SURFACE_STRUCTURE *surfStruct, void *data, 
  unsigned char &r, unsigned char &g, unsigned char &b, unsigned char &a, bool alphaPre){

      int type = surfStruct->ss_type;
      unsigned char *p, pixel;
      unsigned short *sp;
      int rc, gc, bc, rgb, pixelBits, bitnum, shift, bitMask, elem;

      switch(type){
          case INT_RGB:
              p = (unsigned char *)data + ((y * surfStruct->scanline_stride + x) << 2);
              b = *p++;
              g = *p++;
              r = *p;
              a = 255;
              return;

          case INT_ARGB:
              p = (unsigned char *)data + ((y * surfStruct->scanline_stride + x) << 2);
              b = *p++;
              g = *p++;
              r = *p++;
              a = *p;
              if(alphaPre){
                  r = MUL(a, r);
                  g = MUL(a, g);
                  b = MUL(a, b);
              }
              return;

          case INT_ARGB_PRE:
              p = (unsigned char *)data + ((y * surfStruct->scanline_stride + x) << 2);
              b = *p++;
              g = *p++;
              r = *p++;
              a = *p;
              if(!alphaPre){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              return;

          case INT_BGR:
              p = (unsigned char *)data + ((y * surfStruct->scanline_stride + x) << 2);
              r = *p++;
              g = *p++;
              b = *p;
              a = 255;
              return;

          case BYTE_BGR:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + x * 3;
              b = *p++;
              g = *p++;
              r = *p;
              a = 255;
              return;

          case BYTE_ABGR:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + (x << 2);
              a = *p++;
              b = *p++;
              g = *p++;
              r = *p;
              if(alphaPre){
                  r = MUL(a, r);
                  g = MUL(a, g);
                  b = MUL(a, b);
              }
              return;

          case BYTE_ABGR_PRE:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + (x << 2);
              a = *p++;
              b = *p++;
              g = *p++;
              r = *p;
              if(!alphaPre){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              return;

          case USHORT_565:
              sp = (unsigned short *)data + y * surfStruct->scanline_stride + x;
              rgb = *sp;
              rc = (rgb >> 11) & 0x1f;
              gc = (rgb >> 5) & 0x3f;
              bc = rgb & 0x1f;
              b = DIV(31, bc);
              g = DIV(63, gc);
              r = DIV(31, rc);
              a = 255;
              return;

          case USHORT_555:
              sp = (unsigned short *)data + y * surfStruct->scanline_stride + x;
              rgb = *sp;
              rc = (rgb >> 10) & 0x1f;
              gc = (rgb >> 5) & 0x1f;
              bc = rgb & 0x1f;
              b = DIV(31, bc);
              g = DIV(31, gc);
              r = DIV(31, rc);
              a = 255;
              return;

          case BYTE_GRAY:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + x;
              r = g = b = *p;
              a = 255;
              return;

          case USHORT_GRAY:
              sp = (unsigned short *)data + y * surfStruct->scanline_stride + x;
              r = g = b = (unsigned char)((float)*sp / 257.0 + 0.5);
              a = 255;
              return;

          case BYTE_BINARY:
              pixelBits = surfStruct->pixel_stride;
              bitnum = x * pixelBits;
              p = (unsigned char *)data + y * surfStruct->scanline_stride + bitnum / 8;
              elem = *p;
              shift = 8 - (bitnum & 7) - pixelBits;
              bitMask = (1 << pixelBits) - 1;
              pixel = (elem >> shift) & bitMask;
              rgb = *(surfStruct->colormap + pixel);
              r = (unsigned char)((rgb >> 16) & 0xff);
              g = (unsigned char)((rgb >> 8) & 0xff);
              b = (unsigned char)(rgb & 0xff);
              a = 255;
              return;

          case BYTE_INDEXED:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + x;
              pixel = *p;
              rgb = *(surfStruct->colormap + pixel);
              r = (unsigned char)((rgb >> 16) & 0xff);
              g = (unsigned char)((rgb >> 8) & 0xff);
              b = (unsigned char)(rgb & 0xff);
              switch(surfStruct->transparency){
                  case GL_OPAQUE:
                      a = 255;
                      break;
                  case GL_BITMASK:
                      if(pixel == surfStruct->transparent_pixel){
                          a = 0;
                          r = 0;
                          g = 0;
                          b = 0;
                      }else{
                          a = 255;
                      }
                      break;
                  case GL_TRANSLUCENT:
                      a = (unsigned char)((rgb >> 24) & 0xff);
                      if(alphaPre){
                          r = MUL(a, r);
                          g = MUL(a, g);
                          b = MUL(a, b);
                      }
              }
              return;

          default:
              a  = r = g = b = 0;
              // TODO
      }
  }

void setRGB
  (int x, int y, SURFACE_STRUCTURE *surfStruct, void *data, 
  unsigned char r, unsigned char g, unsigned char b, unsigned char a, bool alphaPre){

      int type = surfStruct->ss_type;
      unsigned char *p, pixel = 0;
      unsigned short *sp;
      int rc, gc, bc, ac, rgb, pixelBits, bitnum, shift, bitMask, elem, gray, mask,
          error, minError, alphaError, minAlphaError, buf;

      switch(type){
          case INT_RGB:
              p = (unsigned char *)data + ((y * surfStruct->scanline_stride + x) << 2);
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              *p++ = b;
              *p++ = g;
              *p++ = r ;
              *p = 255;
              return;

          case INT_ARGB:
              p = (unsigned char *)data + ((y * surfStruct->scanline_stride + x) << 2);
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }

              *p++ = b;
              *p++ = g;
              *p++ = r;
              *p = a;
              return;

          case INT_ARGB_PRE:
              p = (unsigned char *)data + ((y * surfStruct->scanline_stride + x) << 2);
              if(!alphaPre){
                  r = MUL(a, r);
                  g = MUL(a, g);
                  b = MUL(a, b);
              }
              *p++ = b;
              *p++ = g;
              *p++ = r;
              *p = a;
              return;

          case INT_BGR:
              p = (unsigned char *)data + ((y * surfStruct->scanline_stride + x) << 2);
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              *p++ = r;
              *p++ = g;
              *p++ = b;
              *p = 255;
              return;

          case BYTE_BGR:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + x * 3;
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              *p++ = b;
              *p++ = g;
              *p = r;
              return;

          case BYTE_ABGR:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + (x << 2);
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              *p++ = a;
              *p++ = b;
              *p++ = g;
              *p = r;
              return;

          case BYTE_ABGR_PRE:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + (x << 2);
              if(!alphaPre){
                  r = MUL(a, r);
                  g = MUL(a, g);
                  b = MUL(a, b);
              }
              *p++ = a;
              *p++ = b;
              *p++ = g;
              *p = r;
              return;

          case USHORT_565:
              sp = (unsigned short *)data + y * surfStruct->scanline_stride + x;
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              rc = MUL(r, 31);
              gc = MUL(g, 63);
              bc = MUL(b, 31);
              *sp = (unsigned short)((rc << 11) | (gc << 5) | bc);

              return;

          case USHORT_555:
              sp = (unsigned short *)data + y * surfStruct->scanline_stride + x;
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              rc = MUL(r, 31);
              gc = MUL(g, 31);
              bc = MUL(b, 31);
              *sp = (unsigned short)((rc << 10) | (gc << 5) | bc);
              return;

          case BYTE_GRAY:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + x;
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              if(r == g && g == b){
                  *p = r;
              }else{
                  *p = (unsigned char)(r * 0.299 + g * 0.587 + b * 0.114+ 0.5);
              }
              return;

          case USHORT_GRAY:
              sp = (unsigned short *)data + y * surfStruct->scanline_stride + x;
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }
              if(r == g && g == b){
                  *sp = (unsigned short)r * 257;
              }else{
                  *sp = (unsigned short)(r * 0.299 + g * 0.587 + b * 0.114 + 0.5) * 257;
              }
              return;

          case BYTE_BINARY:
              gray = (int)(r * 0.299 + g * 0.587 + b * 0.114 + 0.5);
              minError = 255;
              error = 0;
              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }

              for (int i = 0; i < surfStruct->colormap_size; i++) {
                  error = abs((*(surfStruct->colormap + i) & 0xff) - gray);
                  if (error < minError) {
                      pixel = i;
                      if (error == 0) break;
                      minError = error;
                  }
              }

              pixelBits = surfStruct->pixel_stride;
              bitnum = x * pixelBits;
              p = (unsigned char *)data + y * surfStruct->scanline_stride + bitnum / 8;
              elem = *p;
              shift = 8 - (bitnum & 7) - pixelBits;
              bitMask = (1 << pixelBits) - 1;
              mask = ~(bitMask << shift);
              elem &= mask;
              elem |= (pixel & bitMask) << shift;
              *p = elem;
              return;

          case BYTE_INDEXED:
              p = (unsigned char *)data + y * surfStruct->scanline_stride + x;

              if(alphaPre && a != 255){
                  r = DIV(a, r);
                  g = DIV(a, g);
                  b = DIV(a, b);
              }

              if (!surfStruct->has_alpha && surfStruct->isGrayPallete) {
                  gray = (int)(r * 0.299 + g * 0.587 + b * 0.114 + 0.5);
                  minError = 255;
                  error = 0;

                  for (int i = 0; i < surfStruct->colormap_size; i++) {
                      error = abs((*(surfStruct->colormap + i) & 0xff) - gray);
                      if (error < minError) {
                          pixel = i;
                          if (error == 0) break;
                          minError = error;
                      }
                  }
              } else if (a == 0 && surfStruct->transparent_pixel > -1) {
                  pixel = surfStruct->transparent_pixel;
              } else  {
                  minAlphaError = 255;
                  minError = 195075; // 255^2 + 255^2 + 255^2
                  error = 0;

                  for (int i = 0; i < surfStruct->colormap_size; i++) {
                      rgb = *(surfStruct->colormap + i);
                      ac = (rgb >> 24) & 0xff;
                      rc = (rgb >> 16) & 0xff;
                      gc = (rgb >> 8) & 0xff;
                      bc = rgb & 0xff;

                      alphaError = abs(ac - a);
                      if (alphaError <= minAlphaError) {
                          minAlphaError = alphaError;
                          buf = rc - r;
                          error = buf * buf;

                          if (error < minError) {
                              buf = gc - g;
                              error += buf * buf;

                              if (error < minError) {
                                  buf = bc - b;
                                  error += buf * buf;

                                  if (error < minError) {
                                      pixel = i;
                                      minError = error;
                                  }
                              }
                          }
                      }
                  }
              }
              *p = pixel;
              return;

          default:
              a  = r = g = b = 0;
              // TODO
      }
  }

void blitMapInit(){
    for(int y = 0; y < 14; y++){
        for(int x = 0; x < 14; x++){
            src_over_blt[x][y] = src_over_custom;
            src_blt[x][y] = src_over_custom;
            xor_blt[x][y] = xor_custom;
        }
    }
    src_over_blt[1][1] = src_over_intrgb_intrgb;
    src_over_blt[1][2] = src_over_intrgb_intargb;
    src_over_blt[2][1] = src_over_intargb_intrgb;
    src_over_blt[2][2] = src_over_intargb_intargb;
    src_over_blt[13][2] = src_over_byteindexed_intargb;

    //src_blt[1][1] = src_over_intrgb_intrgb;
    //src_blt[1][2] = src_over_intrgb_intrgb;
    //src_blt[2][1] = src_over_intrgb_intrgb;
    //src_blt[2][2] = src_over_intrgb_intrgb;
}

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_render_NativeImageBlitter_bltBG
  (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jobject srcData, 
  jint dstX, jint dstY, jlong dstSurfStruct, jobject dstData, jint width, jint height, 
  jint bgcolor, jint compType, jfloat alpha, jintArray clip, jboolean invalidated){

      if(compType == COMPOSITE_DST) return;

      SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
      SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;
          int srcType = srcSurf->ss_type;
          int dstType = dstSurf->ss_type;
          if(srcType < 0 || dstType < 0) return;

      int a = (int)(alpha * 255 + 0.5);

      int srcW = srcSurf->width;
      int srcH = srcSurf->height;

      int dstW = dstSurf->width;
      int dstH = dstSurf->height;

      int srcX2 = srcW - 1;
      int srcY2 = srcH - 1;
      int dstX2 = dstW - 1;
      int dstY2 = dstH - 1;

      if(srcX > srcX2 || srcY > srcY2) return;
      if(dstX > dstX2 || dstY > dstY2) return;

      if(srcX < 0){
          width += srcX;
          srcX = 0;
      }
      if(srcY < 0){
          height += srcY;
          srcY = 0;
      }

      if(dstX < 0){
          width += dstX;
                  srcX -= dstX;
          dstX = 0;
      }
      if(dstY < 0){
          height += srcY;
                  srcY -= dstY;
          srcY = 0;
      }

      if(srcX + width > srcX2) width = srcX2 - srcX + 1;
      if(srcY + height > srcY2) height = srcY2 - srcY + 1;
      if(dstX + width > dstX2) width = dstX2 - dstX + 1;
      if(dstY + height > dstY2) height = dstY2 - dstY + 1;

      if(width <= 0 || height <= 0) return;

      int *rects = (int *)env->GetPrimitiveArrayCritical(clip, 0);
      int numRects = *rects;
          void *srcDataPtr = env->GetPrimitiveArrayCritical((jarray)srcData, 0);
          void *dstDataPtr = env->GetPrimitiveArrayCritical((jarray)dstData, 0);

          if(!numRects || !srcDataPtr || !dstDataPtr){
                  if(clip){
                          env->ReleasePrimitiveArrayCritical(clip, (void *)rects, 0);
                  }
                  if(srcData){
                          env->ReleasePrimitiveArrayCritical((jarray)srcData, srcDataPtr, 0);
                  }
                  if(dstData){
                          env->ReleasePrimitiveArrayCritical((jarray)dstData, dstDataPtr, 0);
                  }
                  return;
          }

      for(int i = 1; i < numRects; i += 4){
          int _sx = srcX;
          int _sy = srcY;

          int _dx = dstX;
          int _dy = dstY;

          int _w = width;
          int _h = height;

          int cx = rects[i];            // Clipping left top X
          int cy = rects[i + 1];        // Clipping left top Y
          int cx2 = rects[i + 2];       // Clipping right bottom X
          int cy2 = rects[i + 3];       // Clipping right bottom Y

          if(_dx > cx2 || _dy > cy2 || dstX2 < cx || dstY2 < cy) continue;

          if(cx > _dx){
              int shx = cx - _dx;
              _w -= shx;
              _dx = cx;
              _sx += shx;
          }

          if(cy > _dy){
              int shy = cy - _dy;
              _h -= shy;
              _dy = cy;
              _sy += shy;
          }

          if(_dx + _w > cx2 + 1){
              _w = cx2 - _dx + 1;
          }

          if(_dy + _h > cy2 + 1){
              _h = cy2 - _dy + 1;
          }

          if(_sx > srcX2 || _sy > srcY2) continue;

          switch(compType){
              case COMPOSITE_SRC_OVER:
                  src_over_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_SRC:
                  src_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_SRC_ATOP:
                  src_atop_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_SRC_IN:
                  src_in_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_SRC_OUT:
                  src_out_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_XOR:
                  xor_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_CLEAR:
                  clear_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_DST:
                  dst_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_DST_ATOP:
                  dst_atop_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_DST_IN:
                  dst_in_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_DST_OUT:
                  dst_out_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
              case COMPOSITE_DST_OVER:
                  dst_over_custom_bg(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a, bgcolor);
                  break;
          }

      }
      env->ReleasePrimitiveArrayCritical(clip, (void *)rects, 0);
      env->ReleasePrimitiveArrayCritical((jarray)srcData, srcDataPtr, 0);
      env->ReleasePrimitiveArrayCritical((jarray)dstData, dstDataPtr, 0);

  }

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_render_NativeImageBlitter_blt
  (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jobject srcData, 
  jint dstX, jint dstY, jlong dstSurfStruct, jobject dstData, jint width, jint height, 
  jint compType, jfloat alpha, jintArray clip, jboolean invalidated){

      if(compType == COMPOSITE_DST) return;

      SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
      SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

      int srcType = srcSurf->ss_type;
      int dstType = dstSurf->ss_type;
      if(srcType < 0 || dstType < 0) return;

      int a = (int)(alpha * 255 + 0.5);

      int *rects = (int *)env->GetPrimitiveArrayCritical(clip, 0);
      int numRects = *rects;

      void *srcDataPtr, *dstDataPtr;

      if(srcData == NULL){
          srcDataPtr = NULL;

#ifdef _WIN32
          if(srcSurf->bmpData == NULL){
              srcSurf->bmpData = (BYTE *)malloc(srcSurf->bmpInfo.bmiHeader.biSizeImage);
          }
          GetDIBits(srcSurf->gi->hdc, srcSurf->gi->bmp, 0, srcSurf->height, srcSurf->bmpData, 
              (BITMAPINFO *)&srcSurf->bmpInfo, DIB_RGB_COLORS);
          srcDataPtr = srcSurf->bmpData;

#endif

#ifdef unix
          if(!srcSurf->ximage){
              if(XImageByteOrder(srcSurf->display) == LSBFirst){
                  srcSurf->ximage = XGetImage(srcSurf->display, srcSurf->drawable, 0, 0, 
                     srcSurf->width, srcSurf->height, ~(0L), ZPixmap);
              }else{
                  XImage *tmp = XGetImage(srcSurf->display, srcSurf->drawable, 0, 0, 
                      1, 1, ~(0L), ZPixmap);

                  srcSurf->ximage = XCreateImage(srcSurf->display, srcSurf->visual_info->visual,
                      tmp->depth, tmp->format, tmp->xoffset, (char *)malloc(tmp->width * tmp->height * tmp->bytes_per_line),
                      srcSurf->width, srcSurf->height, tmp->bitmap_pad, 0);

                  XDestroyImage(tmp);
 
                  srcSurf->ximage->byte_order = LSBFirst;

                  XGetSubImage(srcSurf->display, srcSurf->drawable, 0, 0, 
                      srcSurf->width, srcSurf->height, ~(0L), ZPixmap, srcSurf->ximage, 0, 0);
              }
              srcSurf->scanline_stride_byte = srcSurf->ximage->bytes_per_line;

              char *info = (char *)srcSurf->visual_info;
              int visual_class = (int)*((int *)(info + sizeof(Visual *) + sizeof(VisualID) + sizeof(int) + sizeof(unsigned int)));
              int bpp = srcSurf->ximage->bits_per_pixel;

              switch(visual_class){ 
              case TrueColor:
              case DirectColor:
                  if(bpp == 32){
                      srcSurf->scanline_stride = srcSurf->scanline_stride_byte >> 2;
                      if(srcSurf->visual_info->red_mask == 0xff0000 && srcSurf->visual_info->green_mask == 0xff00 &&
                          srcSurf->visual_info->blue_mask == 0xff){
 
                          srcSurf->ss_type = INT_RGB;
                          srcSurf->red_mask = 0xff0000;
                          srcSurf->green_mask = 0xff00;
                          srcSurf->blue_mask = 0xff;
                      } else if (srcSurf->visual_info->red_mask == 0xff && srcSurf->visual_info->green_mask == 0xff00 &&
                          srcSurf->visual_info->blue_mask == 0xff0000){

                          srcSurf->ss_type = INT_BGR;
                          srcSurf->red_mask = 0xff;
                          srcSurf->green_mask = 0xff00;
                          srcSurf->blue_mask = 0xff0000;
                      } else {
                          srcSurf->ss_type = -1;
                      }
                  }else if(bpp == 16){
                      srcSurf->scanline_stride = srcSurf->scanline_stride_byte >> 1;
                      if(srcSurf->visual_info->red_mask == 0x7c00 && srcSurf->visual_info->green_mask == 0x03e0 &&
                          srcSurf->visual_info->blue_mask == 0x1f){

                          srcSurf->ss_type = USHORT_555;
                          srcSurf->red_mask = 0x7c00;
                          srcSurf->green_mask = 0x03e0;
                          srcSurf->blue_mask = 0x1f;
                      } else if (srcSurf->visual_info->red_mask == 0xf800 && srcSurf->visual_info->green_mask == 0x07e0 &&
                          srcSurf->visual_info->blue_mask == 0x1f){

                          srcSurf->ss_type = USHORT_565;
                          srcSurf->red_mask = 0xf800;
                          srcSurf->green_mask = 0x07e0;
                          srcSurf->blue_mask = 0x1f;
                      } else {
                          srcSurf->ss_type = -1;
                      }
                  }else{
                          srcSurf->ss_type = -1;
                  }
                  break;
              case StaticGray:
              case PseudoColor:
              case GrayScale:
              case StaticColor: 
                  // TODO: Need to implement parsing of others visual types
                  srcSurf->ss_type = -1;
                  break;
              default:
                  srcSurf->ss_type = -1;
              }
          } else {
                 XGetSubImage(srcSurf->display, srcSurf->drawable, 0, 0, 
                     srcSurf->width, srcSurf->height, ~(0L), ZPixmap, srcSurf->ximage, 0, 0);
          }
          srcDataPtr = srcSurf->ximage->data;
#endif
          srcType = srcSurf->ss_type;
      }else{
          srcDataPtr = env->GetPrimitiveArrayCritical((jarray)srcData, 0);
      }

      dstDataPtr = env->GetPrimitiveArrayCritical((jarray)dstData, 0);

      if(!numRects || !srcDataPtr || !dstDataPtr){
          if(clip){
              env->ReleasePrimitiveArrayCritical(clip, (void *)rects, 0);
          }
          if(srcData){
              env->ReleasePrimitiveArrayCritical((jarray)srcData, srcDataPtr, 0);
          }
          if(dstData){
              env->ReleasePrimitiveArrayCritical((jarray)dstData, dstDataPtr, 0);
          }
          return;
      }

      int srcW = srcSurf->width;
      int srcH = srcSurf->height;

      int dstW = dstSurf->width;
      int dstH = dstSurf->height;

      int srcX2 = srcW - 1;
      int srcY2 = srcH - 1;
      int dstX2 = dstW - 1;
      int dstY2 = dstH - 1;

      if(srcX > srcX2 || srcY > srcY2) return;
      if(dstX > dstX2 || dstY > dstY2) return;

      if(srcX < 0){
          width += srcX;
          srcX = 0;
      }
      if(srcY < 0){
          height += srcY;
          srcY = 0;
      }

      if(dstX < 0){
          width += dstX;
          srcX -= dstX;
          dstX = 0;
      }
      if(dstY < 0){
          height += srcY;
          srcY -= dstY;
          dstY = 0;
      }

      if(srcX + width > srcX2) width = srcX2 - srcX + 1;
      if(srcY + height > srcY2) height = srcY2 - srcY + 1;
      if(dstX + width > dstX2) width = dstX2 - dstX + 1;
      if(dstY + height > dstY2) height = dstY2 - dstY + 1;

      if(width <= 0 || height <= 0) return;

      for(int i = 1; i < numRects; i += 4){
          int _sx = srcX;
          int _sy = srcY;

          int _dx = dstX;
          int _dy = dstY;

          int _w = width;
          int _h = height;

          int cx = rects[i];            // Clipping left top X
          int cy = rects[i + 1];        // Clipping left top Y
          int cx2 = rects[i + 2];       // Clipping right bottom X
          int cy2 = rects[i + 3];       // Clipping right bottom Y

          if(_dx > cx2 || _dy > cy2 || dstX2 < cx || dstY2 < cy) continue;

          if(cx > _dx){
              int shx = cx - _dx;
              _w -= shx;
              _dx = cx;
              _sx += shx;
          }

          if(cy > _dy){
              int shy = cy - _dy;
              _h -= shy;
              _dy = cy;
              _sy += shy;
          }

          if(_dx + _w > cx2 + 1){
              _w = cx2 - _dx + 1;
          }

          if(_dy + _h > cy2 + 1){
              _h = cy2 - _dy + 1;
          }

          if(_sx > srcX2 || _sy > srcY2) continue;

          switch(compType){
              case COMPOSITE_SRC_OVER:
                  src_over_blt[srcType][dstType](_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_SRC:
                  src_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_SRC_ATOP:
                  src_atop_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_SRC_IN:
                  src_in_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_SRC_OUT:
                  src_out_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_XOR:
                  xor_blt[srcType][dstType](_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_CLEAR:
                  clear_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_DST:
                  dst_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_DST_ATOP:
                  dst_atop_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_DST_IN:
                  dst_in_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_DST_OUT:
                  dst_out_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
              case COMPOSITE_DST_OVER:
                  dst_over_custom(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, a);
                  break;
          }

      }
      env->ReleasePrimitiveArrayCritical(clip, (void *)rects, 0);
      if(srcData) env->ReleasePrimitiveArrayCritical((jarray)srcData, srcDataPtr, 0);
      env->ReleasePrimitiveArrayCritical((jarray)dstData, dstDataPtr, 0);

  }

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_render_NativeImageBlitter_xor
  (JNIEnv *env, jobject obj, jint srcX, jint srcY, jlong srcSurfStruct, jobject srcData, 
  jint dstX, jint dstY, jlong dstSurfStruct, jobject dstData, jint width, jint height, 
  jint xorcolor, jintArray clip, jboolean invalidated){

      SURFACE_STRUCTURE *srcSurf = (SURFACE_STRUCTURE *)srcSurfStruct;
      SURFACE_STRUCTURE *dstSurf = (SURFACE_STRUCTURE *)dstSurfStruct;

      int srcType = srcSurf->ss_type;
      int dstType = dstSurf->ss_type;
      if(srcType < 0 || dstType < 0) return;

      int *rects = (int *)env->GetPrimitiveArrayCritical(clip, 0);
      int numRects = *rects;

      void *srcDataPtr, *dstDataPtr;

      if(srcData == NULL){
          srcDataPtr = NULL;

#ifdef _WIN32
          if(srcSurf->bmpData == NULL){
              srcSurf->bmpData = (BYTE *)malloc(srcSurf->bmpInfo.bmiHeader.biSizeImage);
          }
          GetDIBits(srcSurf->gi->hdc, srcSurf->gi->bmp, 0, srcSurf->height, srcSurf->bmpData, 
              (BITMAPINFO *)&srcSurf->bmpInfo, DIB_RGB_COLORS);
          srcDataPtr = srcSurf->bmpData;

#endif

      }else{
          srcDataPtr = env->GetPrimitiveArrayCritical((jarray)srcData, 0);
      }

      dstDataPtr = env->GetPrimitiveArrayCritical((jarray)dstData, 0);

      if(!numRects || !srcDataPtr || !dstDataPtr){
          if(clip){
              env->ReleasePrimitiveArrayCritical(clip, (void *)rects, 0);
          }
          if(srcData){
              env->ReleasePrimitiveArrayCritical((jarray)srcData, srcDataPtr, 0);
          }
          if(dstData){
              env->ReleasePrimitiveArrayCritical((jarray)dstData, dstDataPtr, 0);
          }
          return;
      }

      int srcW = srcSurf->width;
      int srcH = srcSurf->height;

      int dstW = dstSurf->width;
      int dstH = dstSurf->height;

      int srcX2 = srcW - 1;
      int srcY2 = srcH - 1;
      int dstX2 = dstW - 1;
      int dstY2 = dstH - 1;

      if(srcX > srcX2 || srcY > srcY2) return;
      if(dstX > dstX2 || dstY > dstY2) return;

      if(srcX < 0){
          width += srcX;
          srcX = 0;
      }
      if(srcY < 0){
          height += srcY;
          srcY = 0;
      }

      if(dstX < 0){
          width += dstX;
          srcX -= dstX;
          dstX = 0;
      }
      if(dstY < 0){
          height += srcY;
          srcY -= dstY;
          dstY = 0;
      }

      if(srcX + width > srcX2) width = srcX2 - srcX + 1;
      if(srcY + height > srcY2) height = srcY2 - srcY + 1;
      if(dstX + width > dstX2) width = dstX2 - dstX + 1;
      if(dstY + height > dstY2) height = dstY2 - dstY + 1;

      if(width <= 0 || height <= 0) return;

      for(int i = 1; i < numRects; i += 4){
          int _sx = srcX;
          int _sy = srcY;

          int _dx = dstX;
          int _dy = dstY;

          int _w = width;
          int _h = height;

          int cx = rects[i];            // Clipping left top X
          int cy = rects[i + 1];        // Clipping left top Y
          int cx2 = rects[i + 2];       // Clipping right bottom X
          int cy2 = rects[i + 3];       // Clipping right bottom Y

          if(_dx > cx2 || _dy > cy2 || dstX2 < cx || dstY2 < cy) continue;

          if(cx > _dx){
              int shx = cx - _dx;
              _w -= shx;
              _dx = cx;
              _sx += shx;
          }

          if(cy > _dy){
              int shy = cy - _dy;
              _h -= shy;
              _dy = cy;
              _sy += shy;
          }

          if(_dx + _w > cx2 + 1){
              _w = cx2 - _dx + 1;
          }

          if(_dy + _h > cy2 + 1){
              _h = cy2 - _dy + 1;
          }

          if(_sx > srcX2 || _sy > srcY2) continue;

          xor_mode_blt(_sx, _sy, srcSurf, srcDataPtr,
                      _dx, _dy, dstSurf, dstDataPtr, _w, _h, xorcolor);

      }
      env->ReleasePrimitiveArrayCritical(clip, (void *)rects, 0);
      if(srcData) env->ReleasePrimitiveArrayCritical((jarray)srcData, srcDataPtr, 0);
      env->ReleasePrimitiveArrayCritical((jarray)dstData, dstDataPtr, 0);


  }

JNIEXPORT void JNICALL Java_org_apache_harmony_awt_gl_Surface_initIDs
(JNIEnv *env, jclass obj){

    blitMapInit();
    init_divLUT();
    init_mulLUT();
}

