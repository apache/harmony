
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

#define LOG_DOMAIN LOG_CLASS_INFO
#include "cxxlog.h"

#include "port_filepath.h"
#include "environment.h"
#include "classloader.h"
#include "Class.h"
#include "class_member.h"
#include "vm_strings.h"
#include "open/vm_util.h"
#include "bytereader.h"
#include "compile.h"
#include "interpreter_exports.h"
#include "jarfile_util.h"

#include "unicode/uchar.h"

/*
 *  References to the JVM spec below are the references to Java Virtual Machine
 *  Specification, Second Edition
 */

#define REPORT_FAILED_CLASS_FORMAT(klass, msg)   \
    {                                                               \
    std::stringstream ss;                                       \
    ss << klass->get_name()->bytes << " : " << msg;                                               \
    klass->get_class_loader()->ReportFailedClass(klass, "java/lang/ClassFormatError", ss);              \
    }

//returns constant pool tag symbolic name
static const char* get_tag_name(ConstPoolTags tag) {
    switch(tag) {
        case CONSTANT_Utf8:
            return "CONSTANT_Utf8";
        case CONSTANT_Integer:
            return "CONSTANT_Integer";
        case CONSTANT_Float:
            return "CONSTANT_Float";
        case CONSTANT_Long:
            return "CONSTANT_Long";
        case CONSTANT_Double:
            return "CONSTANT_Double";
        case CONSTANT_Class:
            return "CONSTANT_Class";
        case CONSTANT_String:
            return "CONSTANT_String";
        case CONSTANT_Fieldref:
            return "CONSTANT_Fieldref";
        case CONSTANT_Methodref:
            return "CONSTANT_Methodref";
        case CONSTANT_InterfaceMethodref:
            return "CONSTANT_InterfaceMethodref";
        case CONSTANT_NameAndType:
            return "CONSTANT_NameAndType";
        case CONSTANT_UnusedEntry:
            return "CONSTANT_UnusedEntry";
    }
    return "(Unknown tag)";
}

//checks if constant pool index and tag are valid
inline static bool
valid_cpi(Class* clss, uint16 idx, ConstPoolTags tag, const char* msg) {
    if(!clss->get_constant_pool().is_valid_index(idx)) {
        //report error message about wrong index
        REPORT_FAILED_CLASS_FORMAT(clss, "invalid constant pool index: "
            << idx << " " << msg);
        return false;
    }
    if(clss->get_constant_pool().get_tag(idx) != tag) {
        //report error message about wrong tag
        REPORT_FAILED_CLASS_FORMAT(clss, "invalid constant pool tag "
            "(expected " << get_tag_name(tag) << " got " 
            << get_tag_name((ConstPoolTags)clss->get_constant_pool().get_tag(idx)) << ") "
            << msg);
        return false;
    }
    return true;
}

struct AttributeID {
    const char* name;
    String* interned_name;
    Attributes attr_id;
};

AttributeID field_attrs[] = {
    {"ConstantValue", NULL, ATTR_ConstantValue},
    {"Synthetic", NULL, ATTR_Synthetic},
    {"Deprecated", NULL, ATTR_Deprecated},
    {"Signature", NULL, ATTR_Signature},
    {"RuntimeVisibleAnnotations", NULL, ATTR_RuntimeVisibleAnnotations},
    {"RuntimeInvisibleAnnotations", NULL, ATTR_RuntimeInvisibleAnnotations},
    {NULL, NULL, ATTR_UNDEF} 
};

AttributeID method_attrs[] = {
    {"Code", NULL, ATTR_Code},
    {"Exceptions", NULL, ATTR_Exceptions},
    {"RuntimeVisibleParameterAnnotations", NULL, ATTR_RuntimeVisibleParameterAnnotations},
    {"RuntimeInvisibleParameterAnnotations", NULL, ATTR_RuntimeInvisibleParameterAnnotations},
    {"AnnotationDefault", NULL, ATTR_AnnotationDefault},
    {"Synthetic", NULL, ATTR_Synthetic},
    {"Deprecated", NULL, ATTR_Deprecated},
    {"Signature", NULL, ATTR_Signature},
    {"RuntimeVisibleAnnotations", NULL, ATTR_RuntimeVisibleAnnotations},
    {"RuntimeInvisibleAnnotations", NULL, ATTR_RuntimeInvisibleAnnotations},
    {NULL, NULL, ATTR_UNDEF}
};

AttributeID class_attrs[] = {
    {"SourceFile", NULL, ATTR_SourceFile},
    {"InnerClasses", NULL, ATTR_InnerClasses},
    {"SourceDebugExtension", NULL, ATTR_SourceDebugExtension},
    {"EnclosingMethod", NULL, ATTR_EnclosingMethod},
    {"Synthetic", NULL, ATTR_Synthetic},
    {"Deprecated", NULL, ATTR_Deprecated},
    {"Signature", NULL, ATTR_Signature},
    {"RuntimeVisibleAnnotations", NULL, ATTR_RuntimeVisibleAnnotations},
    {"RuntimeInvisibleAnnotations", NULL, ATTR_RuntimeInvisibleAnnotations},
    {NULL, NULL, ATTR_UNDEF} 
};

AttributeID code_attrs[] = {
    {"LineNumberTable", NULL, ATTR_LineNumberTable},
    {"LocalVariableTable", NULL, ATTR_LocalVariableTable},
    {"LocalVariableTypeTable", NULL, ATTR_LocalVariableTypeTable},
    {"StackMapTable", NULL, ATTR_StackMapTable},
    {NULL, NULL, ATTR_UNDEF}
};
 
static void preload(String_Pool& string_pool, AttributeID* attrs) {
    for(int i = 0; attrs[i].name != NULL; i++)
    {
        attrs[i].interned_name = string_pool.lookup(attrs[i].name);
    }
}


//
// initializes string pool by preloading it with commonly used strings
//
static bool preload_attrs(String_Pool& string_pool)
{
    preload(string_pool, method_attrs);
    preload(string_pool, field_attrs);
    preload(string_pool, class_attrs);
    preload(string_pool, code_attrs);

    return true;
}

static String* parse_signature_attr(ByteReader &cfs,
                             U_32 attr_len,
                             Class* clss)
{
    //See specification 4.8.8 about attribute length
    if (attr_len != 2) {
        REPORT_FAILED_CLASS_FORMAT(clss,
            "unexpected length of Signature attribute : " << attr_len);
        return NULL;
    }
    uint16 idx;
    if (!cfs.parse_u2_be(&idx)) {
        REPORT_FAILED_CLASS_FORMAT(clss,
            "truncated class file: failed to parse Signature index");
        return NULL;
    }
    if(!valid_cpi(clss, idx, CONSTANT_Utf8, "for signature at Signature attribute"))
        return NULL;

    return clss->get_constant_pool().get_utf8_string(idx);
}

static
Attributes parse_attribute(Class *clss,
                           ByteReader &cfs,
			   AttributeID* attrs,
                           U_32 *attr_len)
{
    static bool UNUSED init = preload_attrs(VM_Global_State::loader_env->string_pool);
    //See specification 4.8 about Attributes
    uint16 attr_name_index;
    if (!cfs.parse_u2_be(&attr_name_index))
    {
        REPORT_FAILED_CLASS_FORMAT(clss,
            "truncated class file: failed to parse attr_name_index");
        return ATTR_ERROR;
    }
    if (!cfs.parse_u4_be(attr_len))
    {
        REPORT_FAILED_CLASS_FORMAT(clss,
            "truncated class file: failed to parse attribute length");
        return ATTR_ERROR;
    }
    if(!valid_cpi(clss, attr_name_index, CONSTANT_Utf8, "for attribute name"))
        return ATTR_ERROR;
    String* attr_name = clss->get_constant_pool().get_utf8_string(attr_name_index);
    for (unsigned i=0; attrs[i].interned_name != NULL ; i++) {
        if (attrs[i].interned_name == attr_name)
            return attrs[i].attr_id;
    }
    //
    // unrecognized attribute; skip
    //
    if(!cfs.skip(*attr_len))
    {
            REPORT_FAILED_CLASS_FORMAT(clss,
                "Truncated class file");
            return ATTR_ERROR;
    }
    return ATTR_UNDEF;
} //parse_attribute

// forward declaration
static U_32
parse_annotation_value(AnnotationValue& value, ByteReader& cfs, Class* clss);

// returns number of read bytes, 0 if error occurred
static U_32
parse_annotation(Annotation** value, ByteReader& cfs, Class* clss)
{
    unsigned initial_offset = cfs.get_offset();

    uint16 type_idx;
    if (!cfs.parse_u2_be(&type_idx)) {
        REPORT_FAILED_CLASS_FORMAT(clss,
            "truncated class file: failed to parse type index of annotation");
        return 0;
    }
    if(!valid_cpi(clss, type_idx, CONSTANT_Utf8, "for annotation type"))
        return 0;
    String* type = clss->get_constant_pool().get_utf8_string(type_idx);

    uint16 num_elements;
    if (!cfs.parse_u2_be(&num_elements)) {
        REPORT_FAILED_CLASS_FORMAT(clss,
            "truncated class file: failed to parse number of annotation elements");
        return 0;
    }
    Annotation* antn = (Annotation*) clss->get_class_loader()->Alloc(
        sizeof(Annotation) + num_elements * sizeof(AnnotationElement));
    //FIXME: verav should throw OOM
    antn->type = type;
    antn->num_elements = num_elements;
    antn->elements = (AnnotationElement*)((POINTER_SIZE_INT)antn + sizeof(Annotation));
    *value = antn;

    for (unsigned j = 0; j < num_elements; j++)
    {
        uint16 name_idx;
        if (!cfs.parse_u2_be(&name_idx)) {
            REPORT_FAILED_CLASS_FORMAT(clss,
                "truncated class file: failed to parse element_name_index of annotation element");
            return 0;
        }
        if(!valid_cpi(clss, name_idx, CONSTANT_Utf8, "for annotation element name"))
            return 0;
        antn->elements[j].name = clss->get_constant_pool().get_utf8_string(name_idx);

        if (parse_annotation_value(antn->elements[j].value, cfs, clss) == 0) {
            return 0;
        }
    }

    return cfs.get_offset() - initial_offset;
}

// returns number of read bytes, 0 if error occurred
static U_32
parse_annotation_value(AnnotationValue& value, ByteReader& cfs, Class* clss)
{
    unsigned initial_offset = cfs.get_offset();

    U_8 tag;
    if (!cfs.parse_u1(&tag)) {
        REPORT_FAILED_CLASS_FORMAT(clss,
            "truncated class file: failed to parse annotation value tag");
        return 0;
    }
    value.tag = (AnnotationValueType)tag;

    ConstantPool& cp = clss->get_constant_pool();
    unsigned cp_size = cp.get_size();

    switch(tag) {
    case AVT_BOOLEAN:
    case AVT_BYTE:
    case AVT_SHORT:
    case AVT_CHAR:
    case AVT_INT:
    case AVT_LONG:
    case AVT_FLOAT:
    case AVT_DOUBLE:
    case AVT_STRING:
        {
            uint16 const_idx;
            if (!cfs.parse_u2_be(&const_idx)) {
                REPORT_FAILED_CLASS_FORMAT(clss,
                    "truncated class file: failed to parse const index of annotation value");
                return 0;
            }

            switch (tag) {
            case AVT_BOOLEAN:
            case AVT_BYTE:
            case AVT_SHORT:
            case AVT_CHAR:
            case AVT_INT:
                if (!valid_cpi(clss, const_idx, CONSTANT_Integer, "of const_value for annotation element value"))
                    return 0;
                value.const_value.i = cp.get_int(const_idx);
                break;
            case AVT_FLOAT:
                if (!valid_cpi(clss, const_idx, CONSTANT_Float, "of const_value for annotation element value"))
                    return 0;
                value.const_value.f = cp.get_float(const_idx);
                break;
            case AVT_LONG:
                if (!valid_cpi(clss, const_idx, CONSTANT_Long, "of const_value for annotation element value"))
                    return 0;
                value.const_value.l.lo_bytes = cp.get_8byte_low_word(const_idx);
                value.const_value.l.hi_bytes = cp.get_8byte_high_word(const_idx);
                break;
            case AVT_DOUBLE:
                if (!valid_cpi(clss, const_idx, CONSTANT_Double, "of const_value for annotation element value"))
                    return 0;
                value.const_value.l.lo_bytes = cp.get_8byte_low_word(const_idx);
                value.const_value.l.hi_bytes = cp.get_8byte_high_word(const_idx);
                break;
            case AVT_STRING:
                if (!valid_cpi(clss, const_idx, CONSTANT_Utf8, "of const_value for annotation element value"))
                    return 0;
                value.const_value.string = cp.get_utf8_string(const_idx);
                break;
            default:
                LDIE(68, "Annotation parsing internal error");
            }
        }
        break;

    case AVT_CLASS:
        {
            uint16 class_idx;
            if (!cfs.parse_u2_be(&class_idx)) {
                REPORT_FAILED_CLASS_FORMAT(clss,
                    "truncated class file: failed to parse class_info_index of annotation value");
                return 0;
            }
            if(!valid_cpi(clss, class_idx, CONSTANT_Utf8, " of class for annotation element value"))
                return 0;
            value.class_name = cp.get_utf8_string(class_idx);
        }
        break;

    case AVT_ENUM:
        {
            uint16 type_idx;
            if (!cfs.parse_u2_be(&type_idx)) {
                REPORT_FAILED_CLASS_FORMAT(clss,
                    "truncated class file: failed to parse type_name_index of annotation enum value");
                return 0;
            }
            if(!valid_cpi(clss, type_idx, CONSTANT_Utf8, "of type_name for annotation enum element value"))
                return 0;
            value.enum_const.type = cp.get_utf8_string(type_idx);
            uint16 name_idx;
            if (!cfs.parse_u2_be(&name_idx)) {
                REPORT_FAILED_CLASS_FORMAT(clss,
                    "truncated class file: failed to parse const_name_index of annotation enum value");
                return 0;
            }
            if(!valid_cpi(clss, name_idx, CONSTANT_Utf8, "of const_name for annotation enum element value"))
                return 0;
            value.enum_const.name = cp.get_utf8_string(name_idx);
        }
        break;

    case AVT_ANNOTN:
        {
            if (parse_annotation(&value.nested, cfs, clss) == 0) {
                return 0;
            }
        }
        break;

    case AVT_ARRAY:
        {
            uint16 num;
            if (!cfs.parse_u2_be(&num)) {
                REPORT_FAILED_CLASS_FORMAT(clss,
                    "truncated class file: failed to parse num_values of annotation array value");
                return 0;
            }
            value.array.length = num;
            if (num) {
                value.array.items = (AnnotationValue*) clss->get_class_loader()->Alloc(
                    num * sizeof(AnnotationValue));
                    //FIXME: verav should throw OOM
                for (int i = 0; i < num; i++) {
                    if (parse_annotation_value(value.array.items[i], cfs, clss) == 0) {
                        return 0;
                    }
                }
            }
        }
        break;

    default:
        REPORT_FAILED_CLASS_FORMAT(clss,
            "unrecognized annotation value tag : " << (int)tag);
        return 0;
    }

    return cfs.get_offset() - initial_offset;
}

// returns number of read bytes, 0 if error occurred
static U_32
parse_annotation_table(AnnotationTable ** table, ByteReader& cfs, Class* clss)
{
    unsigned initial_offset = cfs.get_offset();

    uint16 num_annotations;
    if (!cfs.parse_u2_be(&num_annotations)) {
        REPORT_FAILED_CLASS_FORMAT(clss,
            "truncated class file: failed to parse number of Annotations");
        return 0;
    }

    if (num_annotations) {
        *table = (AnnotationTable*) clss->get_class_loader()->Alloc(
            sizeof (AnnotationTable) + (num_annotations - 1)*sizeof(Annotation*));
        //FIXME:verav should throw OOM
        (*table)->length = num_annotations;

        for (unsigned i = 0; i < num_annotations; i++)
        {
            if (parse_annotation((*table)->table + i, cfs, clss) == 0) {
                return 0;
            }
        }
    } else {
        *table = NULL;
    }

    return cfs.get_offset() - initial_offset;
}

static U_32
parse_parameter_annotations(AnnotationTable *** table,
                                        U_8 num_annotations,
                                        ByteReader& cfs, Class* clss)
{
    unsigned initial_offset = cfs.get_offset();

    *table = (AnnotationTable**)clss->get_class_loader()->Alloc(
        num_annotations * sizeof (AnnotationTable*));
    //FIXME: verav should throw OOM
    for (unsigned i = 0; i < num_annotations; i++)
    {
        if(parse_annotation_table(*table + i, cfs, clss) == 0)
            return 0;
    }
    return cfs.get_offset() - initial_offset;
}

void* Class_Member::Alloc(size_t size) {
    ClassLoader* cl = get_class()->get_class_loader();
    assert(cl);
    return cl->Alloc(size);
}

bool Class_Member::parse(Class* clss, ByteReader &cfs)
{
    if (!cfs.parse_u2_be(&_access_flags)) {
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: "
            << "failed to parse member access flags");
        return false;
    }

    _class = clss;

    uint16 name_index;
    if (!cfs.parse_u2_be(&name_index)) {
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: "
            << "failed to parse member name index");
        return false;
    }

    uint16 descriptor_index;
    if (!cfs.parse_u2_be(&descriptor_index)) {
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: "
            << "failed to parse member descriptor index");
        return false;
    }

    ConstantPool& cp = clss->get_constant_pool();
    //
    // Look up the name_index and descriptor_index
    // utf8 string const pool entries.
    // See specification 4.6 about name_index and descriptor_index.
    //
    if(!valid_cpi(clss, name_index, CONSTANT_Utf8, "for member name"))
        return false;

    if(!valid_cpi(clss, descriptor_index, CONSTANT_Utf8, "for member descriptor"))
        return false;

    _name = cp.get_utf8_string(name_index);
    _descriptor = cp.get_utf8_string(descriptor_index);
    return true;
} //Class_Member::parse

static bool
is_identifier(const char *name, unsigned len)
{
    U_32 u_ch;
    for(unsigned i = 0; i < len;) {
        unsigned ch_len = 0;
        if(name[i] & 0x80) {
            assert(name[i] & 0x40);
            if(name[i] & 0x20) {
                U_32 x = name[i];
                U_32 y = name[i + 1];
                U_32 z = name[i + 2];
                u_ch = (U_32)(((0x0f & x) << 12) + ((0x3f & y) << 6) + ((0x3f & z)));
                ch_len = 3;
            } else {
                U_32 x = name[i];
                U_32 y = name[i + 1];
                u_ch = (U_32)(((0x1f & x) << 6) + (0x3f & y));
                ch_len = 2;
            }
        } else {
            u_ch = name[i];
            ch_len = 1;
        }
        if(i == 0) {
            if(!(u_isalpha(u_ch) || u_ch == '$' ||  u_ch == '_'))
                return false;                
        } else {
            if(!(u_isalnum(u_ch) || u_ch == '$' ||  u_ch == '_'))
                return false;
        }    
        i += ch_len;
    }
    return true;
}

// JVM spec:
// Unqualified names must not contain the characters ".", ";", "[" or "/". Method names are
// further constrained so that, with the exception of the special method names (§3.9)
// <init> and <clinit>, they must not contain the characters "<" or ">".
// <init> and <clinit> are not passed to this function
static inline bool
is_valid_member_name(const char *name, unsigned len, bool old_version, bool is_method)
{
    if(old_version) {
        return is_identifier(name, len);
    }else {
        for (unsigned i = 0; i < len; i++) {
            //check if symbol has byte size
            if(!(name[i] & 0x80)) {
                switch(name[i]) {
                case '.':
                case ';':
                case '[':
                case '/':
                    return false;
                case '<':
                case '>':
                    if(is_method)
                        return false;
                }
	    } else { //skip other symbols, they are not in exception list
                assert(name[i] & 0x40);
                if(name[i] & 0x20) {
                    if(len < i + 3) { //check array bound
                        return false;
                    }
                    i += 2;
                } else {
                    if(len < i + 2) {
                        return false;
                    }
                    i++;
                }    
            }
        }
    }
    return true;
}

static inline bool
is_valid_member_descriptor( const char *descriptor,
                        const char **next,
                        bool is_void_legal,
                        bool old_version)
{
    switch (*descriptor)
    {
    case 'B':
    case 'C':
    case 'D':
    case 'F':
    case 'I':
    case 'J':
    case 'S':
    case 'Z':
        *next = descriptor + 1;
        return true;
    case 'V':
        if( is_void_legal ) {
            *next = descriptor + 1;
            return true;
        } else {
            return false;
        }
    case 'L':
        {
            unsigned id_len = 0;
            //See specification 4.4.2 about field descriptors that
            //classname represents a fully qualified class or interface name in internal form.
            const char* iterator;
            for(iterator = ++descriptor;
                *iterator != ';';
                iterator++)
            {
                if( *iterator == '\0' ) {
                    // bad Java descriptor
                    return false;
                }
                if(*iterator == '/') {
                    if(!is_valid_member_name(descriptor, id_len, old_version, false))
                        return false;
                    id_len = 0;
                    descriptor = iterator + 1;
                } else {
                    id_len++;
                }
            }
            if(!is_valid_member_name(descriptor, id_len, old_version, false))
                return false;
            *next = iterator + 1;
            return true;
        }
    case '[':
        {
            //See specification 4.4.2 or 4.5.1 
	    //that array descriptor should represent array with 255 or fewer dimensions
            unsigned dim = 1;
            while(*(++descriptor) == '[') dim++;
            if (dim > 255) return false;

	    return is_valid_member_descriptor(descriptor, next, is_void_legal, old_version);
        }
    default:
        // bad Java descriptor
        return false;
    }
}

static bool is_magic_type_name(const String* name) {
    static String* MAGIC_TYPE_NAMES[]={
        VM_Global_State::loader_env->string_pool.lookup("Lorg/vmmagic/unboxed/Address;"),
        VM_Global_State::loader_env->string_pool.lookup("Lorg/vmmagic/unboxed/Offset;"), 
        VM_Global_State::loader_env->string_pool.lookup("Lorg/vmmagic/unboxed/Word;"),
        VM_Global_State::loader_env->string_pool.lookup("Lorg/vmmagic/unboxed/Extent;"),
        NULL
    };
    for (int i=0;;i++)    {
        String* magicClassName = MAGIC_TYPE_NAMES[i];
        if (magicClassName == NULL) break;
        if (magicClassName == name) {
            return true;
        }
    }
    return false;
}

static bool
is_trusted_classloader(const String *name) {
    if(name == NULL) //bootstrap classloader
        return true;
    if(name->len < 5)
        return false;
    static const char* buf = "java/";
    if (0 == strncmp(buf, name->bytes, 5)) {
        return true;
    } else {
        return false;
    }
}

//define constant for class file version 
static const uint16 JAVA5_CLASS_FILE_VERSION = 49;
static const uint16 JAVA6_CLASS_FILE_VERSION = 50;

bool Field::parse(Global_Env& env, Class *clss, ByteReader &cfs, bool is_trusted_cl)
{
    if(!Class_Member::parse(clss, cfs))
        return false;

    bool old_version = clss->get_version() < JAVA5_CLASS_FILE_VERSION;

    //check field name    
    if(is_trusted_cl) {
        if(env.verify_all
                && !is_valid_member_name(_name->bytes, _name->len, old_version, false))
        {
            REPORT_FAILED_CLASS_FORMAT(clss, "illegal field name : " << _name->bytes);
            return false;
        }
    } else {//always check field name if classloader is not trusted
        if(!is_valid_member_name(_name->bytes, _name->len, old_version, false))
        {
            REPORT_FAILED_CLASS_FORMAT(clss, "illegal field name : " << _name->bytes);
            return false;
        }
    }
    // check field descriptor
    //See specification 4.4.2 about field descriptors.
    const char* next;
    if(!is_valid_member_descriptor(_descriptor->bytes, &next, false, old_version)
            || *next != '\0') 
    {
        REPORT_FAILED_CLASS_FORMAT(clss, "illegal field descriptor : " << _descriptor->bytes);
        return false;
    }

    // check fields access flags
    //See specification 4.6 about access flags
    if(clss->is_interface()) {
        // check interface fields access flags
        if(!(is_public() && is_static() && is_final())){
            REPORT_FAILED_CLASS_FORMAT(clss, "interface field " << get_name()->bytes
                << " has invalid combination of access flags: "
                << "0x" << std::hex << _access_flags);
            return false;
        }
        if(_access_flags & ~(ACC_FINAL | ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC)){
            REPORT_FAILED_CLASS_FORMAT(clss, "interface field " << get_name()->bytes
                << " has invalid combination of access flags: "
                << "0x"<< std::hex << _access_flags);
            return false;
        }
        if(old_version) {
            //for class file version lower than 49 these two flags should be set to zero
            //See specification 4.5 Fields, for 1.4 Java.
            _access_flags &= ~(ACC_SYNTHETIC | ACC_ENUM);
        }
    } else if((is_public() && is_protected()
        || is_protected() && is_private()
        || is_public() && is_private())
        || (is_final() && is_volatile())) {
        REPORT_FAILED_CLASS_FORMAT(clss, "field " << get_name()->bytes
            << " has invalid combination of access flags: "
            << "0x" << std::hex << _access_flags);
        return false;
    }

    //check if field is magic type
     if (is_magic_type_name(_descriptor)) {
         _is_magic_type = 1;
     }

    //check field attributes
    uint16 attr_count;
    if(!cfs.parse_u2_be(&attr_count)) {
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: " 
            << "failed to parse attribute count for field " << get_name()->bytes);
        return false;
    }

    _offset_computed = 0;

    unsigned numConstantValue = 0;
    unsigned numRuntimeVisibleAnnotations = 0;
    unsigned numRuntimeInvisibleAnnotations = 0;
    U_32 attr_len = 0;

    ConstantPool& cp = clss->get_constant_pool();

    for (unsigned j=0; j<attr_count; j++)
    {
        // See specification 4.6 about attributes[]
        Attributes cur_attr = parse_attribute(clss, cfs, field_attrs, &attr_len);
        switch (cur_attr) {
        case ATTR_ConstantValue:
            {   // constant value attribute
                // a field can have at most 1 ConstantValue attribute
                // See specification 4.8.2 about ConstantValueAttribute.
                numConstantValue++;
                if (numConstantValue > 1) {
                    REPORT_FAILED_CLASS_FORMAT(clss, " field " <<
                        get_name()->bytes << " has more then one ConstantValue attribute");
                    return false;
                }
                // attribute length must be two (vm spec reference 4.7.3)
                if (attr_len != 2) {
                    REPORT_FAILED_CLASS_FORMAT(clss, " ConstantValue attribute has invalid length for field " 
                        << get_name()->bytes);
                    return false;
                }

                //For non-static field ConstantValue attribute must be silently ignored
                //See specification 4.8.2, second paragraph
                if(!is_static())
                {
                    if(!cfs.skip(attr_len))
                    {
                        REPORT_FAILED_CLASS_FORMAT(clss,
                            "Truncated class file");
                        return false;
                    }
                }
                else
                {
                    if(!cfs.parse_u2_be(&_const_value_index)) {
                        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse "
                            << "ConstantValue index for field " << get_name()->bytes);
                        return false;
                    }

                    if(!cp.is_valid_index(_const_value_index)) {
                        REPORT_FAILED_CLASS_FORMAT(clss, "invalid ConstantValue index for field " << get_name()->bytes);
                        return false;
                    }

                    Java_Type java_type = get_java_type();

                    switch(cp.get_tag(_const_value_index)) {
                    case CONSTANT_Long:
                        {
                            if (java_type != JAVA_TYPE_LONG) {
                                REPORT_FAILED_CLASS_FORMAT(clss, " data type CONSTANT_Long of ConstantValue " 
                                    << "does not correspond to the type of field " << get_name()->bytes);
                                return false;
                            }
                            const_value.l.lo_bytes = cp.get_8byte_low_word(_const_value_index);
                            const_value.l.hi_bytes = cp.get_8byte_high_word(_const_value_index);
                            break;
                        }
                    case CONSTANT_Float:
                        {
                            if (java_type != JAVA_TYPE_FLOAT) {
                                REPORT_FAILED_CLASS_FORMAT(clss, " data type CONSTANT_Float of ConstantValue "
                               	    << "does not correspond to the type of field " << get_name()->bytes);
                                return false;
                            }
                            const_value.f = cp.get_float(_const_value_index);
                            break;
                        }
                    case CONSTANT_Double:
                        {
                            if (java_type != JAVA_TYPE_DOUBLE) {
                                REPORT_FAILED_CLASS_FORMAT(clss, " data type CONSTANT_Double of ConstantValue "
                                    << "does not correspond to the type of field " << get_name()->bytes);
                                return false;
                            }
                            const_value.l.lo_bytes = cp.get_8byte_low_word(_const_value_index);
                            const_value.l.hi_bytes = cp.get_8byte_high_word(_const_value_index);
                            break;
                        }
                    case CONSTANT_Integer:
                        {
                            if ( !(java_type == JAVA_TYPE_INT         ||
                                java_type == JAVA_TYPE_SHORT       ||
                                java_type == JAVA_TYPE_BOOLEAN     ||
                                java_type == JAVA_TYPE_BYTE        ||
                                java_type == JAVA_TYPE_CHAR) )
                            {
                                REPORT_FAILED_CLASS_FORMAT(clss, " data type CONSTANT_Integer of ConstantValue "
                                    << "does not correspond to the type of field " << get_name()->bytes);
                                return false;
                            }
                            const_value.i = cp.get_int(_const_value_index);
                            break;
                        }
                    case CONSTANT_String:
                        {
                            if (java_type != JAVA_TYPE_CLASS) {
                                REPORT_FAILED_CLASS_FORMAT(clss, " data type CONSTANT_String of ConstantValue "
                                    << "does not correspond to the type of field " << get_name()->bytes);
                                return false;
                            }
                            const_value.string = cp.get_string(_const_value_index);
                            break;
                        }
                    case CONSTANT_UnusedEntry:
                        {
                            //do nothing here
                            break;
                        }
                    default:
                        {
                            REPORT_FAILED_CLASS_FORMAT(clss, " invalid data type tag of ConstantValue "
                                << "for field " << get_name()->bytes);
                            return false;
                        }
                    }//switch
                }//else for static field
            }//case ATTR_ConstantValue
            break;

        case ATTR_Synthetic:
            {
                if(attr_len != 0) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "attribute Synthetic has non-zero length");
                    return false;
                }
                _access_flags |= ACC_SYNTHETIC;
            }
            break;

        case ATTR_Deprecated:
            {
                if(attr_len != 0) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "attribute Deprecated has non-zero length");
                    return false;
                }
                _deprecated = true;
            }
            break;

        case ATTR_Signature:
            {
                if(_signature != NULL) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "more than one Signature attribute for the class");
                    return false;
                }
                if (!(_signature = parse_signature_attr(cfs, attr_len, clss))) {
                    return false;
                }
            }
            break;

        case ATTR_RuntimeVisibleAnnotations:
            {
                // Each field_info structure may contain at most one RuntimeVisibleAnnotations attribute.
                // See specification 4.8.14.
                numRuntimeVisibleAnnotations++;
                if(numRuntimeVisibleAnnotations > 1) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "more than one RuntimeVisibleAnnotations attribute");
                    return false;
                }

                U_32 read_len = parse_annotation_table(&_annotations, cfs, clss);
                if(read_len == 0)
                    return false;
                if (attr_len != read_len) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "error parsing Annotations attribute"
                        << "; declared length " << attr_len
                        << " does not match actual " << read_len);
                    return false;
                }
            }
            break;

        case ATTR_RuntimeInvisibleAnnotations:
            {
                // Each field_info structure may contain at most one RuntimeInvisibleAnnotations attribute.
                numRuntimeInvisibleAnnotations++;
                if(numRuntimeInvisibleAnnotations > 1) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "more than one RuntimeVisibleAnnotations attribute");
                    return false;
                }
                if(env.retain_invisible_annotations) {
                    U_32 read_len =
                        parse_annotation_table(&_invisible_annotations, cfs, clss);
                    if(read_len == 0)
                        return false;
                    if(attr_len != read_len) {
                        REPORT_FAILED_CLASS_FORMAT(clss,
                            "error parsing RuntimeInvisibleAnnotations attribute"
                            << "; declared length " << attr_len
                            << " does not match actual " << read_len);
                        return false;
                    }
                } else {
                    if(!cfs.skip(attr_len)) {
                        REPORT_FAILED_CLASS_FORMAT(_class,
                            "Truncated class file");
                        return false;
                    }
                }
            }
            break;

        case ATTR_UNDEF:
            // unrecognized attribute; skipped
            break;
        case ATTR_ERROR:
            return false;
        default:
            REPORT_FAILED_CLASS_CLASS(_class->get_class_loader(), _class, "java/lang/InternalError",
                _class->get_name()->bytes << ": unknown error occured "
                "while parsing attributes for field "
                << _name->bytes << _descriptor->bytes
                << "; unprocessed attribute " << cur_attr);
            return false;
        } // switch
    } // for

    TypeDesc* td = type_desc_create_from_java_descriptor(get_descriptor()->bytes, clss->get_class_loader());
    if( td == NULL ) {
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        return false;
    }
    set_field_type_desc(td);

    return true;
} //Field::parse


bool Handler::parse(Class* clss, unsigned code_length,
                    ByteReader& cfs, Method* method)
{
    //See specification 4.8.3 about exception_table
    uint16 start = 0;
    if(!cfs.parse_u2_be(&start)) {
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse start_pc"
            << " for exception handler of code attribute for method "
            << method->get_name()->bytes << method->get_descriptor()->bytes);
        return false;
    }
    
    _start_pc = (unsigned) start;

    if (_start_pc >= code_length){
        REPORT_FAILED_CLASS_FORMAT(clss, " illegal start_pc"
            << " for exception handler of code attribute for method "
            << method->get_name()->bytes << method->get_descriptor()->bytes);            
        return false;
    }

    uint16 end;
    if (!cfs.parse_u2_be(&end)){
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse end_pc"
            << " for exception handler of code attribute for method "
            << method->get_name()->bytes << method->get_descriptor()->bytes);
        return false;
    }
 
    _end_pc = (unsigned) end;

    if (_end_pc > code_length){
        REPORT_FAILED_CLASS_FORMAT(clss, " illegal end_pc"
            << " for exception handler of code attribute for method "
            << method->get_name()->bytes << method->get_descriptor()->bytes);
        return false;
    }

    if (_start_pc >= _end_pc){
        REPORT_FAILED_CLASS_FORMAT(clss, " start_pc is not less than end_pc"
            << " for exception handler of code attribute for method "
            << method->get_name()->bytes << method->get_descriptor()->bytes);
        return false;
    }

    uint16 handler;
    if (!cfs.parse_u2_be(&handler)){
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse handler_pc"
            << " for exception handler of code attribute for method "
            << method->get_name()->bytes << method->get_descriptor()->bytes);
        return false;
    }
    _handler_pc = (unsigned) handler;

    if (_handler_pc >= code_length){
        REPORT_FAILED_CLASS_FORMAT(clss, " illegal handler_pc"
            << " for exception handler of code attribute for method "
            << method->get_name()->bytes << method->get_descriptor()->bytes);
        return false;       
    }

    uint16 catch_index;
    if (!cfs.parse_u2_be(&catch_index)){
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse catch_type"
            << " for exception handler of code attribute for method "
            << method->get_name()->bytes << method->get_descriptor()->bytes);
        return false;
    }

    _catch_type_index = catch_index;

    if (catch_index == 0) {
        _catch_type = NULL;
    } else {
        if(!valid_cpi(clss, catch_index, CONSTANT_Class, "for exception handler class of code attribute"))
            return false;
        ConstantPool& cp = clss->get_constant_pool();
        if(!valid_cpi(clss, cp.get_class_name_index(catch_index), CONSTANT_Utf8, "for exception handler class name of code attribute"))
            return false;
        _catch_type = cp.get_utf8_string(cp.get_class_name_index(catch_index));
    }
    return true;
} //Handler::parse


bool Method::get_line_number_entry(unsigned index, jlong* pc, jint* line) {
    if (_line_number_table && index < _line_number_table->length) {
        *pc = _line_number_table->table[index].start_pc;
        *line = _line_number_table->table[index].line_number;
        return true;
    } else {
        return false;
    }
}

bool Method::get_local_var_entry(unsigned index, jlong* pc,
                         jint* length, jint* slot, String** name,
                         String** type, String** generic_type) {
    if ((_local_vars_table) && (index < _local_vars_table->length)) {
        *pc = _local_vars_table->table[index].start_pc;
        *length = _local_vars_table->table[index].length;
        *slot = _local_vars_table->table[index].index;
        *name = _local_vars_table->table[index].name;
        *type = _local_vars_table->table[index].type;
        *generic_type = _local_vars_table->table[index].generic_type;
        return true;
    } else {
        return false;
    }
}

#define REPORT_FAILED_METHOD(msg) REPORT_FAILED_CLASS_CLASS(_class->get_class_loader(), \
    _class, "java/lang/ClassFormatError", \
    _class->get_name()->bytes << " : " << msg << " for method "\
    << _name->bytes << _descriptor->bytes);


bool Method::_parse_exceptions(ConstantPool& cp, unsigned attr_len,
                               ByteReader& cfs)
{
    if(!cfs.parse_u2_be(&_n_exceptions)) {
        REPORT_FAILED_METHOD("truncated class file: failed to parse "
            << "number of exceptions");
        return false;
    }
    if (attr_len != _n_exceptions * sizeof(uint16) + sizeof(_n_exceptions) ) {
        REPORT_FAILED_METHOD(" invalid Exceptions attribute length "
            << "while parsing exceptions");
        return false;
    }
    _exceptions = new String*[_n_exceptions];
    //FIXME: verav Should throw OOM
    for (unsigned i=0; i<_n_exceptions; i++) {
        uint16 index;
        if(!cfs.parse_u2_be(&index)) {
            REPORT_FAILED_METHOD("truncated class file: failed to parse "
                << "exception class index while parsing exceptions");
            return false;
        }

        //See specification 4.8.4 about exception_index_table[].
        if(!valid_cpi(_class, index, CONSTANT_Class, "of exception_table entry for Exception attrubute"))
            return false;
        if(!valid_cpi(_class, cp.get_class_name_index(index), CONSTANT_Utf8, "of exception_table entry for Exception attrubute"))
            return false;
        _exceptions[i] = cp.get_utf8_string(cp.get_class_name_index(index));
    }

    return true;
} //Method::_parse_exceptions

bool Method::_parse_line_numbers(unsigned attr_len, ByteReader &cfs) {
    uint16 n_line_numbers;
    if(!cfs.parse_u2_be(&n_line_numbers)) {
        REPORT_FAILED_METHOD("could not parse line_number_table_length "
            "while parsing LineNumberTable attribute");
        return false;
    }
    //See specification 4.8.10 about attribute_length.
    unsigned real_lnt_attr_len = sizeof(n_line_numbers) + n_line_numbers * 2 * sizeof(uint16);
    if(real_lnt_attr_len != attr_len) {
        REPORT_FAILED_METHOD("LineNumberTable attribute has wrong length  ("
            << attr_len << " vs. " << real_lnt_attr_len << ")" );
        return false;
    }

    _line_number_table =
        (Line_Number_Table *)STD_MALLOC(sizeof(Line_Number_Table) +
        sizeof(Line_Number_Entry) * (n_line_numbers - 1));
    // ppervov: FIXME: should throw OOME
    _line_number_table->length = n_line_numbers;

    for (unsigned j = 0; j < n_line_numbers; j++) {
        uint16 start_pc;
        uint16 line_number;
        if(!cfs.parse_u2_be(&start_pc)) {
            REPORT_FAILED_METHOD("could not parse start_pc "
                "while parsing LineNumberTable");
            return false;
        }

        if(start_pc >= _byte_code_length) {
            REPORT_FAILED_METHOD("start_pc in LineNumberTable "
                "points outside the code");
            return false;
        }

        if(!cfs.parse_u2_be(&line_number)) {
            REPORT_FAILED_METHOD("could not parse line_number "
                "while parsing LineNumberTable");
            return false;
        }
        _line_number_table->table[j].start_pc = start_pc;
        _line_number_table->table[j].line_number = line_number;
    }
    return true;
} //Method::_parse_line_numbers


bool Method::_parse_local_vars(Local_Var_Table* table, LocalVarOffset *offset_list,
            Global_Env& env, ConstantPool& cp, ByteReader &cfs, const char *attr_name, Attributes attribute)
{
    for (unsigned j = 0; j < table->length; j++) {
        //go to the start of entry
        if(!cfs.go_to_offset(offset_list->value)){
            REPORT_FAILED_CLASS_FORMAT(_class, "could not go to the start of LVT entry");
            return false;
        }

        uint16 start_pc;
        if(!cfs.parse_u2_be(&start_pc)) {
            REPORT_FAILED_METHOD("truncated class file: failed to parse start_pc "
                "in " << attr_name << " attribute");
            return false;
        }

        uint16 length;
        if(!cfs.parse_u2_be(&length)) {
            REPORT_FAILED_METHOD("truncated class file: failed to parse length entry "
                "in " << attr_name << " attribute");
            return false;
        }

        if( (start_pc >= _byte_code_length)
            || (start_pc + (unsigned)length) > _byte_code_length ) {
            REPORT_FAILED_METHOD(attr_name << " entry "
                "[start_pc, start_pc + length) = [" << start_pc << ", " << start_pc + length << 
                ") points outside bytecode range");
            return false;
        }

        uint16 name_index;
        if(!cfs.parse_u2_be(&name_index)) {
            REPORT_FAILED_METHOD("truncated class file: failed to parse name index "
                "in " << attr_name << " attribute");
            return false;
        }

        uint16 descriptor_index;
        if(!cfs.parse_u2_be(&descriptor_index)) {
            REPORT_FAILED_METHOD("truncated class file: failed to parse descriptor index "
                "in " << attr_name << " attribute");
            return false;
        }
        
        if(!valid_cpi(_class, name_index, CONSTANT_Utf8, "for name of CONSTANT_Utf8 entry"))
            return false;
        String* name = cp.get_utf8_string(name_index);
        if(!is_valid_member_name(name->bytes, name->len,
                _class->get_version() < JAVA5_CLASS_FILE_VERSION, false))
        {
            REPORT_FAILED_METHOD("name of local variable: " << name->bytes <<
                " in " << attr_name << " attribute is not stored as unqualified name");
            return false;
        }
        if(!valid_cpi(_class, descriptor_index, CONSTANT_Utf8, "for descriptor of CONSTANT_Utf8 entry"))
            return false;
        String* descriptor = cp.get_utf8_string(descriptor_index);

        if(attribute == ATTR_LocalVariableTable)
        {
            const char *next;
            if(!is_valid_member_descriptor(descriptor->bytes, &next, false, _class->get_version() < JAVA5_CLASS_FILE_VERSION)
                    || *next != '\0')
            {
                REPORT_FAILED_METHOD("illegal field descriptor:  " << descriptor->bytes <<
                    " in " << attr_name << " attribute for local variable: " << name->bytes);
                return false;
            }
        }

        uint16 index;
        if(!cfs.parse_u2_be(&index)) {
            REPORT_FAILED_METHOD("could not parse index "
                "in " << attr_name << " attribute");
            return false;
        }
        //See specification about index value 4.8.11 and 4.8.12
        if((descriptor->bytes[0] == 'D' || descriptor->bytes[0] == 'J')
                && index >= _max_locals - 1)
        {
            REPORT_FAILED_METHOD("invalid local index "
                << index << " in " << attr_name << " attribute");
            return false;
        }

        if (index >= _max_locals) {
            REPORT_FAILED_METHOD("invalid local index "
                << index << " in " << attr_name << " attribute");
            return false;
        }

        //ensure that table has no duplicates
        unsigned i;
        for(i = 0; i < j; i++)
        {
            //RI does not check descriptors
            if(index == table->table[i].index
                && name == table->table[i].name
                && start_pc == table->table[i].start_pc
                && length == table->table[i].length)
            {
                break;
            }
        }

        if (j != 0 && i != j) {
            // duplicate entry found
            if (!env.verify || _class->get_version() < JAVA5_CLASS_FILE_VERSION) {
                //just ignore the entry
                --j;
                --table->length;
            } else {
                REPORT_FAILED_METHOD("Duplicate local variable "<< name->bytes
                    << " in attribute " << attr_name);
                return false;
            }
        } else {
            table->table[j].start_pc = start_pc;
            table->table[j].length = length;
            table->table[j].index = index;
            table->table[j].name = name;
            table->table[j].type = descriptor;
            table->table[j].generic_type = NULL;
        }

        offset_list = offset_list->next;
    }

    return true;
} //Method::_parse_local_vars


bool Method::_parse_code(Global_Env& env, ConstantPool& cp, unsigned code_attr_len,
                         ByteReader& cfs)
{
    unsigned real_code_attr_len = 0;
    if(!cfs.parse_u2_be(&_max_stack)) {
        REPORT_FAILED_METHOD("truncated class file: failed to parse max_stack "
            << "while parsing Code");
        return false;
    }

    if(!cfs.parse_u2_be(&_max_locals)) {
        REPORT_FAILED_METHOD("truncated class file: failed to parse max_locals "
            << "while parsing Code attribute");
        return false;
    }

    //See specification 4.8.3 about Code Attribute, max_locals.
    if(_max_locals < _arguments_slot_num) {
        REPORT_FAILED_METHOD(" wrong max_locals count "
            << "while parsing Code attribute");
        return false;
    }

    if(!cfs.parse_u4_be(& _byte_code_length)) {
        REPORT_FAILED_METHOD("truncated class file: failed to parse bytecode length "
            << "while parsing Code attribute");
        return false;
    }

    // See specification 4.8.3 and 4.10.1 about code_length.
    // code length for non-abstract java methods must not be 0
    if(_byte_code_length == 0
        || (_byte_code_length >= (1<<16)))
    {
        REPORT_FAILED_METHOD(" invalid bytecode length "
            << _byte_code_length);
        return false;
    }

    //See specification 4.8.3 about attribute_length value.
    real_code_attr_len += 8;

    //
    // allocate & parse code array
    //
    _byte_codes = new U_8[_byte_code_length];
    // ppervov: FIXME: should throw OOME

    unsigned i;
    for (i=0; i<_byte_code_length; i++) {
        if(!cfs.parse_u1(&_byte_codes[i])) {
            REPORT_FAILED_METHOD("truncated class file: failed to parse bytecode");
            return false;
        }
    }
    real_code_attr_len += _byte_code_length;

    if(!cfs.parse_u2_be(&_n_handlers)) {
        REPORT_FAILED_METHOD("truncated class file: failed to parse number of exception handlers");
        return false;
    }
    real_code_attr_len += 2;

    //
    // allocate & parse exception handler table
    //
    _handlers = new Handler[_n_handlers];
    // ppervov: FIXME: should throw OOME

    for (i=0; i<_n_handlers; i++) {
        if(!_handlers[i].parse(_class, _byte_code_length, cfs, this)) {
            return false;
        }
    }
    real_code_attr_len += _n_handlers*8; // for the size of exception_table entry see JVM Spec 4.8.3

    //
    // attributes of the Code attribute
    //
    uint16 n_attrs;
    if(!cfs.parse_u2_be(&n_attrs)) {
        REPORT_FAILED_METHOD("truncated class file: failed to parse number of attributes");
        return false;
    }
    real_code_attr_len += 2;

    static bool TI_enabled = VM_Global_State::loader_env->TI->isEnabled();

    U_32 attr_len = 0;
    LocalVarOffset* offset_lvt_array = NULL;
    LocalVarOffset* lvt_iter = NULL;
    LocalVarOffset* offset_lvtt_array = NULL;
    LocalVarOffset* lvtt_iter = NULL;
    unsigned num_lvt_entries = 0;
    unsigned num_lvtt_entries = 0;

    unsigned numStackMap = 0;
    m_stackmap = 0;

    for (i=0; i<n_attrs; i++) {
        Attributes cur_attr = parse_attribute(_class, cfs, code_attrs, &attr_len);
        switch(cur_attr) {
        case ATTR_LineNumberTable:
            {
                if  (!_parse_line_numbers(attr_len, cfs)) {
                    return false;
                }
                break;
            }
        case ATTR_LocalVariableTable:
            {

                uint16 n_local_vars;
                if(!cfs.parse_u2_be(&n_local_vars)) {
                    REPORT_FAILED_METHOD("could not parse local variables number "
                            "of LocalVariableTable attribute");
                    return false;
                }
                TRACE2("classloader.spec","number of local vars:" <<n_local_vars);
                unsigned lnt_attr_len = 2 + n_local_vars * 10;
                if(lnt_attr_len != attr_len) {
                    REPORT_FAILED_METHOD("real LocalVariableTable attribute length differ "
                        "from declared length ("
                        << attr_len << " vs. " << lnt_attr_len << ")" );
                    return false;
                }
                if(n_local_vars == 0) break;

                if(offset_lvt_array == NULL){
                    offset_lvt_array = lvt_iter =
                        (LocalVarOffset*)STD_ALLOCA(sizeof(LocalVarOffset) * n_local_vars);
                } else {
                    lvt_iter->next = (LocalVarOffset*)STD_ALLOCA(sizeof(LocalVarOffset) * n_local_vars);
                    lvt_iter = lvt_iter->next;
                }
                int off = cfs.get_offset();
                
                int j = 0;
                for(j = 0; j < n_local_vars - 1; j++, lvt_iter++)
                {
                    lvt_iter->value = off + 10*j;
                    lvt_iter->next = lvt_iter + 1;
                }
                lvt_iter->value = off + 10*j;
                lvt_iter->next = NULL;
                num_lvt_entries += n_local_vars;
                if (!cfs.skip(10*n_local_vars))
                {
                    REPORT_FAILED_CLASS_FORMAT(_class,
                            "Truncated class file");
                        return false;
                }
                break;
            }
        case ATTR_LocalVariableTypeTable:
            {
                if(_class->get_version() < JAVA5_CLASS_FILE_VERSION) {
                    //skip this attribute for class files of version less than 49
                    if (!cfs.skip(attr_len))
                    {
                        REPORT_FAILED_CLASS_FORMAT(_class,
                            "Truncated class file");
                        return false;
                    }
                } else {
                    uint16 n_local_vars;
                    if(!cfs.parse_u2_be(&n_local_vars)) {
                        REPORT_FAILED_METHOD("could not parse local variables number "
                                "of LocalVariableTypeTable attribute");
                        return false;
                    }
                    unsigned lnt_attr_len = 2 + n_local_vars * 10;
                    if(lnt_attr_len != attr_len) {
                        REPORT_FAILED_METHOD("real LocalVariableTypeTable attribute length differ "
                                "from declared length ("
                                << attr_len << " vs. " << lnt_attr_len << ")" );
                        return false;
                    }
                    if(n_local_vars == 0) break;

                    if(offset_lvtt_array == NULL){
                        offset_lvtt_array = lvtt_iter =
                            (LocalVarOffset*)STD_ALLOCA(sizeof(LocalVarOffset) * n_local_vars);
                    } else {
                        lvtt_iter->next = (LocalVarOffset*)STD_ALLOCA(sizeof(LocalVarOffset) * n_local_vars);
                        lvtt_iter = lvtt_iter->next;
                    }
                    int off = cfs.get_offset();
                    int j = 0;
                    for(j = 0; j < n_local_vars - 1; j++, lvtt_iter++)
                    {
                        lvtt_iter->value = off + 10*j;
                        lvtt_iter->next = lvtt_iter + 1;
                    }
                    lvtt_iter->value = off + 10*j;
                    lvtt_iter->next = NULL;
                    num_lvtt_entries += n_local_vars;

                    if (!cfs.skip(10*n_local_vars))
                    {
                        REPORT_FAILED_CLASS_FORMAT(_class,
                            "Truncated class file");
                        return false;
                    }
                }
                break;
            }

        case ATTR_StackMapTable:
            //TODO: skip if classfile version less than 50
            numStackMap++;
            if (numStackMap > 1) {
                REPORT_FAILED_METHOD(" there is more than one StackMap attribute");
                return false;
            }

            m_stackmap = (U_8*)Method::Alloc(attr_len + 6);
            if(!cfs.skip(-6)) { // read once again attribute head
                REPORT_FAILED_CLASS_CLASS(_class->get_class_loader(), _class, "java/lang/InternalError",
                    _class->get_name()->bytes << ": inernal error: unable to read beginning of an attribute");
                return false;
            }

            unsigned i;
            for (i=0; i<attr_len + 6; i++) {
                if(!cfs.parse_u1(&m_stackmap[i])) {
                    REPORT_FAILED_METHOD("truncated class file: failed to parse bytecode");
                    return false;
                }
            }

            break;

        case ATTR_UNDEF:
            // unrecognized attribute; skipped
            break;
        case ATTR_ERROR:
            return false;
        default:
            // error occured
            REPORT_FAILED_CLASS_CLASS(_class->get_class_loader(), _class, "java/lang/InternalError",
                _class->get_name()->bytes << ": unknown error occured "
                "while parsing attributes for code of method "
                << _name->bytes << _descriptor->bytes
                << "; unprocessed attribute " << cur_attr);
            return false;
        } // switch
        real_code_attr_len += 6 + attr_len; // u2 - attribute_name_index, u4 - attribute_length
    } // for
    if(code_attr_len != real_code_attr_len) {
        REPORT_FAILED_METHOD( " Code attribute length does not match real length "
            "in class file (" << code_attr_len << " vs. " << real_code_attr_len
            << ") while parsing attributes for code");
        return false;
    }

    // we should remember this point to return here
    // after complete LVT and LVTT parsing.
    int return_point = cfs.get_offset();

    if(_class->get_version() >= JAVA5_CLASS_FILE_VERSION) {
        if(num_lvt_entries == 0 && num_lvtt_entries != 0) {
            REPORT_FAILED_METHOD("if LocalVariableTable is empty "
                    "LocalVariableTypeTable must be empty too");
            return false;
        }
    }

    if(num_lvt_entries != 0) {
        //lvt and lvtt parsing
        Local_Var_Table* lv_table = NULL;
        Local_Var_Table* generic_vars = NULL;
        static const int LV_ALLOCATION_THRESHOLD = 30;

        if(num_lvtt_entries != 0) {
            if( num_lvtt_entries < LV_ALLOCATION_THRESHOLD ){
                generic_vars = (Local_Var_Table *)STD_ALLOCA(sizeof(Local_Var_Table) +
                        sizeof(Local_Var_Entry) * (num_lvtt_entries - 1));
            } else {
                generic_vars = (Local_Var_Table *)STD_MALLOC(sizeof(Local_Var_Table) +
                        sizeof(Local_Var_Entry) * (num_lvtt_entries - 1));
            }
            generic_vars->length = num_lvtt_entries;
        }

        if(TI_enabled) {
            lv_table = (Local_Var_Table *)_class->get_class_loader()->Alloc(
                sizeof(Local_Var_Table) +
                sizeof(Local_Var_Entry) * (num_lvt_entries - 1));
        } else {
            if( num_lvt_entries < LV_ALLOCATION_THRESHOLD){
                lv_table =(Local_Var_Table *)STD_ALLOCA(sizeof(Local_Var_Table) +
                    sizeof(Local_Var_Entry) * (num_lvt_entries - 1));
            } else {
                lv_table =(Local_Var_Table *)STD_MALLOC(sizeof(Local_Var_Table) +
                    sizeof(Local_Var_Entry) * (num_lvt_entries - 1));
            }
        }
        lv_table->length = num_lvt_entries;

	//this bool variable is needed to avoid memory leak in case
	//parsing of LVT failed 
        bool failed = false; 
        if (!_parse_local_vars(lv_table, offset_lvt_array, env, cp, cfs,
                "LocalVariableTable", ATTR_LocalVariableTable)
            || (generic_vars && !_parse_local_vars(generic_vars, offset_lvtt_array, env, cp, cfs,
                "LocalVariableTypeTable", ATTR_LocalVariableTypeTable)))
        {
            failed = true;
        }
        // JVM spec hints that LocalVariableTypeTable is meant to be a supplement to LocalVariableTable
        // See specification 4.8.13 second paragraph.
        if (!failed && generic_vars) {
            unsigned j = i = 0;
            for (i = 0; i < generic_vars->length; i++) {
                for (j = 0; j < lv_table->length; j++) {
                    if (generic_vars->table[i].name == lv_table->table[j].name
                        && generic_vars->table[i].start_pc == lv_table->table[j].start_pc
                        && generic_vars->table[i].length == lv_table->table[j].length
                        && generic_vars->table[i].index == lv_table->table[j].index)
                    {
                        lv_table->table[j].generic_type = generic_vars->table[i].type;
                        break;
                    }
                }
                if(j == lv_table->length && env.verify) {
                    REPORT_FAILED_METHOD("Element "<< generic_vars->table[i].name->bytes <<
                        " of LocalVariableTypeTable does not match any of LocalVariableTable entries");
                    failed = true;
                    break;
                }
            }
        }

        if(TI_enabled) {
            _local_vars_table = lv_table;
        } else {
            if(num_lvt_entries >= LV_ALLOCATION_THRESHOLD) {
                STD_FREE(lv_table);
            }
            if( num_lvtt_entries >= LV_ALLOCATION_THRESHOLD ){
                STD_FREE(generic_vars);
            }
        }

        if (failed) {
            return false;
        }
    }


    //return to the right ByteReader point
    if(!cfs.go_to_offset(return_point))
    {
        return false;
    }

    return true;
} //Method::_parse_code

static inline bool
check_method_descriptor(const char *descriptor, bool old_version)
{
    const char *next;
    bool result;

    if( *descriptor != '(' ) 
        return false;

    next = ++descriptor;
    while( descriptor[0] != ')' )
    {
        result = is_valid_member_descriptor(descriptor, &next, false, old_version);
        if( !result || *next == '\0' ) {
            return false;
        }
        descriptor = next;
    }
    next = ++descriptor;
    result = is_valid_member_descriptor(descriptor, &next, true, old_version);
    if( *next != '\0' ) 
        return false;
    return result;
}

bool Method::parse(Global_Env& env, Class* clss,
                   ByteReader &cfs, bool is_trusted_cl)
{
    if(!Class_Member::parse(clss, cfs))
        return false;
    //check method name
    if(is_trusted_cl && env.verify_all && !(_name == env.Init_String || _name == env.Clinit_String)
		    && !is_valid_member_name(_name->bytes, _name->len,
                        clss->get_version() < JAVA5_CLASS_FILE_VERSION, true)) {
                REPORT_FAILED_CLASS_FORMAT(clss, "illegal method name : " << _name->bytes);
                return false;
    } else {
        if(!(_name == env.Init_String || _name == env.Clinit_String)
            && !is_valid_member_name(_name->bytes, _name->len,
                    clss->get_version() < JAVA5_CLASS_FILE_VERSION, true))
        {
            REPORT_FAILED_CLASS_FORMAT(clss, "illegal method name : " << _name->bytes);
            return false;
        }
    }
    // check method descriptor
    if(!check_method_descriptor(_descriptor->bytes,
            clss->get_version() < JAVA5_CLASS_FILE_VERSION))
    {
        REPORT_FAILED_METHOD( " invalid descriptor");
        return false;
    }
    
    calculate_arguments_slot_num();

    //The total length of method parameters should be 255 or less.
    //See 4.4.3 in specification.
    if(_arguments_slot_num > 255) {
        REPORT_FAILED_METHOD("more than 255 arguments");
        return false;
    }
    // checked method descriptor

    _intf_method_for_fake_method = NULL;

    // set the has_finalizer, is_clinit and is_init flags
    if(_name == env.FinalizeName_String && _descriptor == env.VoidVoidDescriptor_String) {
        _flags.is_finalize = 1;
    }
    else if(_name == env.Init_String)
        _flags.is_init = 1;
    else if(_name == env.Clinit_String)
        _flags.is_clinit = 1;
    // check method access flags
    if(!is_clinit())
    {
        if(_class->is_interface())
        {
            if(!(is_abstract() && is_public())){
                REPORT_FAILED_CLASS_FORMAT(_class, " Interface method " 
                    << _name->bytes << _descriptor->bytes
                    << "must have both access flags ACC_ABSTRACT and ACC_PUBLIC set"
                    << "0x" << std::hex << _access_flags);
                return false;
            }
            if(_access_flags & ~(ACC_ABSTRACT | ACC_PUBLIC | ACC_VARARGS
                            | ACC_BRIDGE | ACC_SYNTHETIC)){
                REPORT_FAILED_CLASS_FORMAT(_class, " Interface method " 
                    << _name->bytes << _descriptor->bytes 
                    << " has invalid combination of access flags "
                    << "0x" << std::hex << _access_flags);
                return false;
            }
            //for class file version lower than 49 these three flags should be set to zero
            //See specification 4.6 Methods, for 1.4 Java.            
            if(_class->get_version() < JAVA5_CLASS_FILE_VERSION){
                _access_flags &= ~(ACC_BRIDGE | ACC_VARARGS | ACC_SYNTHETIC); 
            }
        } else {
            if(is_private() && is_protected()
                || is_private() && is_public()
                || is_protected() && is_public())
            {
                //See specification 4.7 Methods about access_flags
                REPORT_FAILED_CLASS_FORMAT(_class," Method "
                    << _name->bytes << _descriptor->bytes 
                    << " has invalid combination of access flags "
                    << "0x" << std::hex << _access_flags);
                return false;
            }
            if(is_abstract()
            && (is_final() || is_native() || is_private()
                    || is_static() || is_strict() || is_synchronized()))
            {
                bool bout = false;
                REPORT_FAILED_CLASS_FORMAT(_class, " Method " 
                    << _name->bytes << _descriptor->bytes
                    << " has invalid combination of access flags "
                    << "0x" << std::hex << _access_flags);
                return false;
            }
            if(is_init()) {
                if(_access_flags & ~(ACC_STRICT | ACC_VARARGS | ACC_SYNTHETIC
                        | ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED))
                {
                    REPORT_FAILED_CLASS_FORMAT(_class, " Method " 
                        << _name->bytes << _descriptor->bytes
                        << " has invalid combination of access flags "
                        << "0x" << std::hex << _access_flags);
                    return false;
                }    
            }
        }    
    } else {
        // Java VM specification
        // 4.7 Methods
        // "Class and interface initialization methods (3.9) are called
        // implicitly by the Java virtual machine; the value of their
        // access_flags item is ignored except for the settings of the
        // ACC_STRICT flag"
        _access_flags &= ACC_STRICT;
        // compiler assumes that <clinit> has ACC_STATIC
        // but VM specification does not require this flag to be present
        // so, enforce it
        _access_flags |= ACC_STATIC;
    }

    //check method attributes
    uint16 attr_count;
    if(!cfs.parse_u2_be(&attr_count)) {
        REPORT_FAILED_METHOD("truncated class file: failed to parse attributes count");
        return false;
    }

    unsigned numCode = 0;
    unsigned numExceptions = 0;
    unsigned numRuntimeVisibleAnnotations = 0;
    unsigned numRuntimeInvisibleAnnotations = 0;
    unsigned numRuntimeInvisibleParameterAnnotations = 0;
    U_32 attr_len = 0;
    ConstantPool& cp = clss->get_constant_pool();

    for (unsigned j=0; j<attr_count; j++) {
        Attributes cur_attr = parse_attribute(clss, cfs, method_attrs, &attr_len);
        switch(cur_attr) {
        case ATTR_Code:
            numCode++;
            if (numCode > 1) {
                REPORT_FAILED_METHOD(" there is more than one Code attribute");
                return false;
            }
            if(is_abstract() || is_native()) {
                REPORT_FAILED_CLASS_FORMAT(_class, " Method " << _name->bytes << _descriptor->bytes
                    << ": " << (is_abstract()?"abstract":(is_native()?"native":""))
                    << " should not have Code attribute present");
                return false;
            }
            if(!_parse_code(env, cp, attr_len, cfs))
                return false;
            break;

        case ATTR_Exceptions:
            numExceptions++;
            if(numExceptions > 1) {
                REPORT_FAILED_METHOD(" there is more than one Exceptions attribute");
                return false;
            }
            if(!_parse_exceptions(cp, attr_len, cfs))
                return false;
            break;

        case ATTR_RuntimeInvisibleParameterAnnotations:
            {
                //RuntimeInvisibleParameterAnnotations attribute is parsed only if
                //command line option -Xinvisible is set. See specification 4.8.17.
                if(env.retain_invisible_annotations) {
                    numRuntimeInvisibleParameterAnnotations++;
                    if(numRuntimeInvisibleParameterAnnotations > 1) {
                        REPORT_FAILED_CLASS_FORMAT(clss,
                            "more than one RuntimeInvisibleParameterAnnotations attribute");
                        return false;
                    }
                    if (!cfs.parse_u1(&_num_invisible_param_annotations)) {
                        REPORT_FAILED_CLASS_FORMAT(clss,
                            "truncated class file: failed to parse number of InvisibleParameterAnnotations");
                        return false;
                    }
                    U_32 read_len = 1;
                    if (_num_invisible_param_annotations) {
                        U_32 len =
                            parse_parameter_annotations(&_invisible_param_annotations,
                                        _num_invisible_param_annotations, cfs, _class);  
                        if(len == 0)
                            return false;
                        read_len += len;                        
                    }
                    if (attr_len != read_len) {
                        REPORT_FAILED_METHOD(
                            "error parsing InvisibleParameterAnnotations attribute"
                            << "; declared length " << attr_len
                            << " does not match actual " << read_len);
                        return false;
                    }
                } else {
                    if (!cfs.skip(attr_len))
                    {
                        REPORT_FAILED_CLASS_FORMAT(_class,
                            "Truncated class file");
                        return false;
                    }
                }
            }
            break;

        case ATTR_RuntimeVisibleParameterAnnotations:
            {
                // See specification 4.8.16.
                if (_param_annotations) {
                    REPORT_FAILED_METHOD(
                        "more than one RuntimeVisibleParameterAnnotations attribute");
                    return false;
                }

                if (!cfs.parse_u1(&_num_param_annotations)) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "cannot parse number of ParameterAnnotations");
                    return false;
                }
                U_32 read_len = 1;
                if (_num_param_annotations) {
                    U_32 len = parse_parameter_annotations(&_param_annotations,
                                    _num_param_annotations, cfs, _class);
                    if(len == 0)
                        return false;
                    read_len += len;
                }
                if (attr_len != read_len) {
                    REPORT_FAILED_METHOD(
                        "error parsing ParameterAnnotations attribute"
                        << "; declared length " << attr_len
                        << " does not match actual " << read_len);
                    return false;
                }
            }
            break;

        case ATTR_AnnotationDefault:
            {
                //See specification 4.8.18 about default_value
                if (_default_value) {
                    REPORT_FAILED_METHOD("more than one AnnotationDefault attribute");
                    return false;
                }
                _default_value = (AnnotationValue *)_class->get_class_loader()->Alloc(
                    sizeof(AnnotationValue));
                //FIXME: verav should throw OOM
                U_32 read_len = parse_annotation_value(*_default_value, cfs, clss);
                if (read_len == 0) {
                    return false;
                } else if (read_len != attr_len) {
                    REPORT_FAILED_METHOD(
                        "declared length " << attr_len
                        << " of AnnotationDefault attribute "
                        << " does not match actual " << read_len);
                    return false;
                }
            }
            break;

        case ATTR_Synthetic:
            {
                if(attr_len != 0) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "attribute Synthetic has non-zero length");
                    return false;
                }
                _access_flags |= ACC_SYNTHETIC;
            }
            break;

        case ATTR_Deprecated:
            {
                if(attr_len != 0) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "attribute Deprecated has non-zero length");
                    return false;
                }
                _deprecated = true;
            }
            break;

        case ATTR_Signature:
            {
                if(_signature != NULL) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "more than one Signature attribute for the class");
                    return false;
                }
                if (!(_signature = parse_signature_attr(cfs, attr_len, clss))) {
                    return false;
                }
            }
            break;

        case ATTR_RuntimeVisibleAnnotations:
            {
                //Each method_info structure may contain at most one RuntimeVisibleAnnotations attribute.
                // See specification 4.8.14.
                numRuntimeVisibleAnnotations++;
                if(numRuntimeVisibleAnnotations > 1) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "more than one RuntimeVisibleAnnotations attribute");
                    return false;
                }

                U_32 read_len = parse_annotation_table(&_annotations, cfs, clss);
                if(read_len == 0)
                    return false;
                if (attr_len != read_len) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "error parsing Annotations attribute"
                        << "; declared length " << attr_len
                        << " does not match actual " << read_len);
                    return false;
                }
            }
            break;

        case ATTR_RuntimeInvisibleAnnotations:
            {
                //Each method_info structure may contain at most one RuntimeInvisibleAnnotations attribute.
                numRuntimeInvisibleAnnotations++; 
                if(numRuntimeInvisibleAnnotations > 1) {
                    REPORT_FAILED_CLASS_FORMAT(clss,
                        "more than one RuntimeInvisibleAnnotations attribute");
                    return false;
                }
                //RuntimeInvisibleAnnotations attribute is parsed only if
                //command line option -Xinvisible is set. See specification 4.8.15.
                if(env.retain_invisible_annotations) {
                    U_32 read_len =
                        parse_annotation_table(&_invisible_annotations, cfs, clss);
                    if(read_len == 0)
                        return false;
                    if (attr_len != read_len) {
                        REPORT_FAILED_CLASS_FORMAT(clss,
                            "error parsing RuntimeInvisibleAnnotations attribute"
                            << "; declared length " << attr_len
                            << " does not match actual " << read_len);
                        return false;
                    }
                }else {
                    if(!cfs.skip(attr_len)) {
                        REPORT_FAILED_CLASS_FORMAT(clss,
                            "Truncated class file");
                        return false;
                    }
                }
            }
            break;

        case ATTR_UNDEF:
            // unrecognized attribute; skipped
            break;
        case ATTR_ERROR:
            return false;
        default:
            REPORT_FAILED_CLASS_CLASS(clss->get_class_loader(), _class, "java/lang/InternalError",
                _class->get_name()->bytes << ": unknown error occured "
                "while parsing attributes for method "
                << _name->bytes << _descriptor->bytes
                << "; unprocessed attribute " << cur_attr);
            return false;
        } // switch
    } // for

    if(!(is_abstract() || is_native()) && numCode == 0) {
        REPORT_FAILED_CLASS_FORMAT(_class, " Method " << _name->bytes << _descriptor->bytes
            << " should have Code attribute present");
        return false;
    }
    return true;
} //Method::parse


bool Class::parse_fields(Global_Env* env, ByteReader& cfs, bool is_trusted_cl)
{
    // Those fields are added by the loader even though they are nor defined
    // in their corresponding class files.
    static struct VMExtraFieldDescription {
        const String* classname;
        String* fieldname;
        String* descriptor;
        uint16 accessflags;
    } vm_extra_fields[] = {
        { env->string_pool.lookup("java/lang/Thread"),
                env->string_pool.lookup("vm_thread"),
                env->string_pool.lookup("J"), ACC_PRIVATE},
        { env->string_pool.lookup("java/lang/Throwable"),
                env->string_pool.lookup("vm_stacktrace"),
                env->string_pool.lookup("[J"), ACC_PRIVATE|ACC_TRANSIENT},
        { env->string_pool.lookup("java/lang/Class"),
                env->string_pool.lookup("vm_class"),
                env->string_pool.lookup("J"), ACC_PRIVATE},
    };
    if(!cfs.parse_u2_be(&m_num_fields)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: faield to parse number of fields");
        return false;
    }

    int num_fields_in_class_file = m_num_fields;
    int i;
    for(i = 0; i < int(sizeof(vm_extra_fields)/sizeof(VMExtraFieldDescription)); i++) {
        if(get_name() == vm_extra_fields[i].classname) {
            m_num_fields++;
        }
    }

    m_fields = new Field[m_num_fields];
    // ppervov: FIXME: should throw OOME

    m_num_static_fields = 0;
    unsigned short last_nonstatic_field = (unsigned short)num_fields_in_class_file;
    for(i=0; i < num_fields_in_class_file; i++) {
        Field fd;
        if(!fd.parse(*env, this, cfs, is_trusted_cl))
            return false;
        if(fd.is_static()) {
            m_fields[m_num_static_fields] = fd;
            m_num_static_fields++;
        } else {
            last_nonstatic_field--;
            m_fields[last_nonstatic_field] = fd;
        }
    }
    assert(last_nonstatic_field == m_num_static_fields);

    //See specification 4.6 Fields:
    //No two fields in one class file may have the same name and descriptor.
    for (int j = 0; j < num_fields_in_class_file; j++){
        for(int k = j + 1; k < num_fields_in_class_file; k++){
            if((m_fields[j].get_name() == m_fields[k].get_name())
                && (m_fields[j].get_descriptor() == m_fields[k].get_descriptor()))
            {
                REPORT_FAILED_CLASS_FORMAT(this, "duplicate field " << get_name()->bytes << "."
                    << m_fields[j].get_name()->bytes << " " << m_fields[j].get_descriptor()->bytes);
                return false;
            }
        }
    }

    for(i = 0; i < int(sizeof(vm_extra_fields)/sizeof(VMExtraFieldDescription)); i++) {
        if(get_name() == vm_extra_fields[i].classname) {
            Field& f = m_fields[num_fields_in_class_file];
            f.set(this, vm_extra_fields[i].fieldname,
                vm_extra_fields[i].descriptor, vm_extra_fields[i].accessflags);
            f.set_injected();
            TypeDesc* td = type_desc_create_from_java_descriptor(
                vm_extra_fields[i].descriptor->bytes, m_class_loader);
            if( td == NULL ) {
                // error occured
                // ppervov: FIXME: should throw OOME
                return false;
            }
            f.set_field_type_desc(td);
            num_fields_in_class_file++;
        }
    }

    return true; // success
} //class_parse_fields


long _total_method_bytes = 0;

bool Class::parse_methods(Global_Env* env, ByteReader &cfs, bool is_trusted_cl)
{
    if(!cfs.parse_u2_be(&m_num_methods)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse number of methods");
        return false;
    }

    m_methods = new Method[m_num_methods];
    //FIXME: should throw OOME

    _total_method_bytes += sizeof(Method)*m_num_methods;

    for(unsigned i = 0;  i < m_num_methods; i++) {
        if(!m_methods[i].parse(*env, this, cfs, is_trusted_cl)) {
            return false;
        }

        Method* m = &m_methods[i];
        if(m->is_clinit()) {
            // There can be at most one clinit per class.
            if(m_static_initializer) {
                REPORT_FAILED_CLASS_FORMAT(this, ": there is more than one class initialization method");
                return false;
            }
            m_static_initializer = &(m_methods[i]);
        }
        // to cache the default constructor
        if (m->get_name() == VM_Global_State::loader_env->Init_String
            && m->get_descriptor() == VM_Global_State::loader_env->VoidVoidDescriptor_String)
        {
            m_default_constructor = &m_methods[i];
        }
    }

    //See specification 4.7 Methods:
    //No two methods in one class file may have the same name and descriptor.
    for (int j = 0; j < m_num_methods; j++){
        for(int k = j + 1; k < m_num_methods; k++){
            if((m_methods[j].get_name() == m_methods[k].get_name())
                && (m_methods[j].get_descriptor() == m_methods[k].get_descriptor()))
            {
                REPORT_FAILED_CLASS_FORMAT(this, "duplicate method " << get_name()->bytes << "."
                    << m_methods[j].get_name()->bytes << m_methods[j].get_descriptor()->bytes);
                return false;
            }
        }
    }

    return true; // success
} //class_parse_methods

static inline bool
check_class_name(const char *name, unsigned len, bool old_version)
{
    if(len == 0)
        return false;
    unsigned id_len = 0;
    const char* iterator = name;
    if(name[0] == '[')
    {
        const char *next = name + 1;
        if(!is_valid_member_descriptor(name, &next, false, old_version) || *next != '\0') {
            return false;
        } else {
            return true;
        }
    } else {
        for(unsigned i = 0; i < len ; i++, iterator++)
        {
            if(*iterator != '/') {
                id_len++;
            } else {
                if(!is_valid_member_name(name, id_len, old_version, false))
                    return false;
                id_len = 0;
                name = iterator;
                name++;
            }
        }
        return is_valid_member_name(name, id_len, old_version, false);
    }
    return false; //unreachable code
}



static String* class_file_parse_utf8data(String_Pool& string_pool, ByteReader& cfs,
                                         uint16 len)
{
    // See specification 4.5.7 about CONSTANT_Utf8 string format.
    // buffer ends before len
    if(!cfs.have(len))
        return NULL;
    //define bytes of UTF8
    const U_8 HIGH_NONZERO_BIT =          0x80; // 10000000
    const U_8 HIGH_TWO_NONZERO_BITS =     0xc0; // 11000000
    const U_8 HIGH_THREE_NONZERO_BITS =   0xe0; // 11100000
    const U_8 HIGH_FOUR_NONZERO_BITS =    0xf0; // 11110000
    
    // get utf8 bytes and move buffer pointer
    U_8* utf8data = (U_8*)cfs.get_and_skip(len);

    // FIXME: decode 6-byte Java 1.5 encoding
    
    uint16 read_len = 0;
    // check utf8 correctness
    for(int i = 0; i < len; i++) {
        if((utf8data[i] & HIGH_NONZERO_BIT) == 0x00)
        {
            if(utf8data[i] == 0x00)
                return NULL;
            read_len++;
        } else if((utf8data[i] & HIGH_THREE_NONZERO_BITS) == HIGH_TWO_NONZERO_BITS) {
            read_len += 2;
            if(read_len > len)
                return NULL;
            i++;
            if((utf8data[i] & HIGH_TWO_NONZERO_BITS) != HIGH_NONZERO_BIT)
                return NULL;
        } else if((utf8data[i] & HIGH_FOUR_NONZERO_BITS) == HIGH_THREE_NONZERO_BITS) {
            read_len += 3;
            if(read_len > len)
                return NULL;
            i++;
            if(((utf8data[i] & HIGH_TWO_NONZERO_BITS) != HIGH_NONZERO_BIT))
                return NULL;
            i++;
            if(((utf8data[i] & HIGH_TWO_NONZERO_BITS) != HIGH_NONZERO_BIT))
                return NULL;
        }
        else {
            return NULL;
        }
    }
    // then lookup on utf8 bytes and return string
    return string_pool.lookup((const char*)utf8data, len);
}

static String* class_file_parse_utf8(String_Pool& string_pool,
                                     ByteReader& cfs)
{
    uint16 len;
    if(!cfs.parse_u2_be(&len))
        return NULL;

    return class_file_parse_utf8data(string_pool, cfs, len);
}


bool ConstantPool::parse(Class* clss,
                         String_Pool& string_pool,
                         ByteReader& cfs)
{
    if(!cfs.parse_u2_be(&m_size)) {
        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse constant pool size");
        return false;
    }

    unsigned char* cp_tags = new unsigned char[m_size];
    // ppervov: FIXME: should throw OOME
    m_entries = new ConstPoolEntry[m_size];
    // ppervov: FIXME: should throw OOME

    //
    // 0'th constant pool entry is a pointer to the tags array
    // See specification 4.5 about Constant Pool
    //
    m_entries[0].tags = cp_tags;
    cp_tags[0] = CONSTANT_Tags;
    for(unsigned i = 1; i < m_size; i++) {
        // parse tag into tag array
        U_8 tag;
        if(!cfs.parse_u1(&tag)) {
            REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse constant pool tag for index " << i);
            return false;
        }

        switch(cp_tags[i] = tag) {
            case CONSTANT_Class:
                if(!cfs.parse_u2_be(&m_entries[i].CONSTANT_Class.name_index)) {
                    REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse name index "
                       << "for CONSTANT_Class entry");
                    return false;
                }
                break;

            case CONSTANT_Methodref:
            case CONSTANT_Fieldref:
            case CONSTANT_InterfaceMethodref:
                if(!cfs.parse_u2_be(&m_entries[i].CONSTANT_ref.class_index)) {
                    REPORT_FAILED_CLASS_FORMAT(clss,"truncated class file: failed to parse class index "
                        << "for CONSTANT_*ref entry");
                    return false;
                }
                if(!cfs.parse_u2_be(&m_entries[i].CONSTANT_ref.name_and_type_index)) {
                    REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse "
                        << "name-and-type index for CONSTANT_*ref entry");
                    return false;
                }
                break;

            case CONSTANT_String:
                if(!cfs.parse_u2_be(&m_entries[i].CONSTANT_String.string_index)) {
                    REPORT_FAILED_CLASS_FORMAT(clss,"truncated class file: failed to parse string " 
                        << "index for CONSTANT_String entry");
                    return false;
                }
                break;

            case CONSTANT_Float:
            case CONSTANT_Integer:
                if(!cfs.parse_u4_be(&m_entries[i].int_value)) {
                    REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse value for "
                        << (tag==CONSTANT_Integer?"CONSTANT_Integer":"CONSTANT_Float") << " entry");
                    return false;
                }
                break;

            case CONSTANT_Double:
            case CONSTANT_Long:
                // longs and doubles take up two entries
                // on both IA32 & IPF, first constant pool element is used, second element - unused
                if(!cfs.parse_u4_be(&m_entries[i].CONSTANT_8byte.high_bytes)) {
                    REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse high four bytes for "
                        << (tag==CONSTANT_Long?"CONSTANT_Integer":"CONSTANT_Float") << " entry");
                    return false;
                }
                if(!cfs.parse_u4_be(&m_entries[i].CONSTANT_8byte.low_bytes)) {
                    REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to not parse low four bytes for "
                        << (tag==CONSTANT_Long?"CONSTANT_Long":"CONSTANT_Double") << " entry");
                    return false;
                }
                // skip next constant pool entry as it is used by next 4 bytes of Long/Double
                if(i + 1 < m_size) {
                    cp_tags[i+1] = CONSTANT_UnusedEntry;
                    m_entries[i+1].CONSTANT_8byte.high_bytes = m_entries[i].CONSTANT_8byte.high_bytes;
                    m_entries[i+1].CONSTANT_8byte.low_bytes = m_entries[i].CONSTANT_8byte.low_bytes;
                }    
                i++;
                break;

            case CONSTANT_NameAndType:
                if(!cfs.parse_u2_be(&m_entries[i].CONSTANT_NameAndType.name_index)) {
                    REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse name index "
                        "for CONSTANT_NameAndType entry");
                    return false;
                }
                if(!cfs.parse_u2_be(&m_entries[i].CONSTANT_NameAndType.descriptor_index)) {
                    REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: failed to parse descriptor index "
                        "for CONSTANT_NameAndType entry");
                    return false;
                }
                break;

            case CONSTANT_Utf8:
                {
                    // parse and insert string into string table
                    String* str = class_file_parse_utf8(string_pool, cfs);
                    if(!str) {
                        REPORT_FAILED_CLASS_FORMAT(clss, "truncated class file: faile to parse CONSTANT_Utf8 entry");
                        return false;
                    }
                    m_entries[i].CONSTANT_Utf8.string = str;
                }
                break;
            default:
                REPORT_FAILED_CLASS_FORMAT(clss, "unknown constant pool tag " << "0x" << std::hex << (int)cp_tags[i]);
                return false;
        }
    }
    return true;
} // ConstantPool::parse


bool ConstantPool::check(Global_Env* env, Class* clss, bool is_trusted_cl)
{
    for(unsigned i = 1; i < m_size; i++) {
        switch(unsigned char tag = get_tag(i))
        {
        case CONSTANT_Class:
        {
            unsigned name_index = get_class_name_index(i);
            if (!valid_cpi(clss, name_index, CONSTANT_Utf8, "for class name at CONSTANT_Class entry")) {
                // illegal name index
                return false;
            }
            if(!check_class_name(get_utf8_string(name_index)->bytes, get_utf8_string(name_index)->len,
                    clss->get_version() < JAVA5_CLASS_FILE_VERSION))
            {
                REPORT_FAILED_CLASS_FORMAT(clss," illegal CONSTANT_Class name "
                    << "\"" << get_utf8_string(name_index)->bytes << "\"");
                return false;
            }
            break;
        }
        case CONSTANT_Methodref:
        case CONSTANT_Fieldref:
        case CONSTANT_InterfaceMethodref:
        {
            unsigned class_index = get_ref_class_index(i);
            if (!valid_cpi(clss, class_index, CONSTANT_Class, "for class name at CONSTANT_*ref entry")) {
                return false;
            }
            unsigned name_type_index = get_ref_name_and_type_index(i);
            if (!valid_cpi(clss, name_type_index, CONSTANT_NameAndType, "for name-and-type at CONSTANT_*ref entry")) {
                return false;
            }            
            const char *next = NULL;
            String *name;
            String *descriptor;
            unsigned name_index = get_name_and_type_name_index(name_type_index);
            if(!valid_cpi(clss, name_index, CONSTANT_Utf8, "for name at CONSTANT_*ref entry")) {
                return false;
            }        
            unsigned descriptor_index = get_name_and_type_descriptor_index(name_type_index);
            if (!valid_cpi(clss, descriptor_index, CONSTANT_Utf8, "for descriptor at CONSTANT_*ref entry")) {
                return false;
            }

            name = get_utf8_string(name_index);
            descriptor = get_utf8_string(descriptor_index);

            if(tag == CONSTANT_Methodref)
            {
                //check method name
                if(is_trusted_cl) {
                    if(env->verify_all && (name != env->Init_String)
                        && !is_valid_member_name(name->bytes,name->len, clss->get_version() < JAVA5_CLASS_FILE_VERSION, true))
                    {
                        REPORT_FAILED_CLASS_FORMAT(clss, " illegal method name for CONSTANT_Methodref entry: " << name->bytes);
                        return false;
                    }
                    if(name->bytes[0] == '<' && name != env->Init_String) {
                        REPORT_FAILED_CLASS_FORMAT(clss, " illegal method name "<< name->bytes
                            << " at constant pool index " << name_index);
                        return false;
                    }
                } else { //always check method name if classloader is not system 
                    if((name != env->Init_String) 
                        && !is_valid_member_name(name->bytes,name->len, clss->get_version() < JAVA5_CLASS_FILE_VERSION, true))
                    {
                        REPORT_FAILED_CLASS_FORMAT(clss, " illegal method name for CONSTANT_Methodref entry: " 
                            << name->bytes);
                        return false;                    
                    }
                }
                //check method descriptor
                if(!check_method_descriptor(descriptor->bytes, clss->get_version() < JAVA5_CLASS_FILE_VERSION))
                {
                    REPORT_FAILED_CLASS_FORMAT(clss, " illegal method descriptor at CONSTANT_Methodref entry: "
                        << descriptor->bytes);
                    return false;
                }
                //for <init> method return type must be void
                //See specification 4.5.2
                if(name == env->Init_String)
                {
                    if(descriptor->bytes[descriptor->len - 1] != 'V')
                    {
                        REPORT_FAILED_CLASS_FORMAT(clss, " return type of <init> method"
                            " is not void at CONSTANT_Methodref entry");
                        return false;
                    }
                }
            }
            if(tag == CONSTANT_Fieldref)
            {
                //check field name
                if(is_trusted_cl) {
                    if(env->verify_all && !is_valid_member_name(name->bytes, name->len,
                                clss->get_version() < JAVA5_CLASS_FILE_VERSION, false))
                    {
                        REPORT_FAILED_CLASS_FORMAT(clss, " illegal field name for CONSTANT_Filedref entry: " 
                            << name->bytes);
                        return false;
                    }
                } else {
                    if(!is_valid_member_name(name->bytes, name->len,
                                clss->get_version() < JAVA5_CLASS_FILE_VERSION, false))
                    {
                        REPORT_FAILED_CLASS_FORMAT(clss, " illegal field name for CONSTANT_Filedref entry: " 
                            << name->bytes);
                        return false;                        
                    }            
                
                }
                //check field descriptor
                if(!is_valid_member_descriptor(descriptor->bytes, &next, false,
                        clss->get_version() < JAVA5_CLASS_FILE_VERSION) || *next != '\0' )
                {
                    REPORT_FAILED_CLASS_FORMAT(clss, " illegal field descriptor at CONSTANT_Fieldref entry: "
                        << descriptor->bytes);
                    return false;
                }
            }
            if(tag == CONSTANT_InterfaceMethodref)
            {
                //check method name, name can't be <init>
                //See specification 4.5.2 about name_and_type_index last sentence.
                if(is_trusted_cl) {
                    if(env->verify_all && (name != env->Clinit_String)
                                && !is_valid_member_name(name->bytes, name->len,
                                clss->get_version() < JAVA5_CLASS_FILE_VERSION, true))
                    {
                        REPORT_FAILED_CLASS_FORMAT(clss, " illegal method name for CONSTANT_InterfaceMethod entry: "
                            << name->bytes);
                        return false;
                    }
                } else {
                    if(!is_valid_member_name(name->bytes, name->len,
                                clss->get_version() < JAVA5_CLASS_FILE_VERSION, true))
                    {
                        REPORT_FAILED_CLASS_FORMAT(clss, " illegal method name for CONSTANT_InterfaceMethod entry: " 
                            << name->bytes);
                        return false;
                    }                    
                }
                //check method descriptor
                if(!check_method_descriptor(descriptor->bytes, clss->get_version() < JAVA5_CLASS_FILE_VERSION))
                {
                    REPORT_FAILED_CLASS_FORMAT(clss, " illegal method descriptor at CONSTANT_InterfaceMethodref entry: "
                        << descriptor->bytes);
                    return false;
                }
            }
            break;
        }
        case CONSTANT_String:
        {
            unsigned string_index = get_string_index(i);
            if (!valid_cpi(clss, string_index, CONSTANT_Utf8, "for string at CONSTANT_String entry")) {
                // illegal string index
                return false;
            }
            // set entry to the actual string
            resolve_entry(i, get_utf8_string(string_index));
            break;
        }
        case CONSTANT_Integer:
        case CONSTANT_Float:
            // not much to do here
            break;
        case CONSTANT_Long:
        case CONSTANT_Double:
            //check Long and Double indexes, n+1 index should be valid too.
            //See specification 4.5.5
            if(i + 1 == m_size){
                REPORT_FAILED_CLASS_FORMAT(clss, " illegal indexes for Long or Double " << i << " and " << i + 1);
                return false;
            }
            i++;
            break;
        case CONSTANT_NameAndType:
        {
            //See specification 4.5.6
            unsigned name_index = get_name_and_type_name_index(i);
            unsigned descriptor_index = get_name_and_type_descriptor_index(i);
            if(!valid_cpi(clss, name_index , CONSTANT_Utf8, "for name at CONSTANT_NameAndType entry")) {
                return false;
            }

            if (!valid_cpi(clss, descriptor_index, CONSTANT_Utf8, "for descriptor at CONSTANT_NameAndType entry")) {
                return false;
            }

            resolve_entry(i, get_utf8_string(name_index), get_utf8_string(descriptor_index));
            break;
        }
        case CONSTANT_Utf8:
            // nothing to do here
            break;
        default:
            REPORT_FAILED_CLASS_FORMAT(clss, " wrong constant pool tag " << get_tag(i));
            return false;
        }
    }
    return true;
} // ConstantPool::check


bool Class::parse_interfaces(ByteReader &cfs)
{
    if(!cfs.parse_u2_be(&m_num_superinterfaces)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse number of superinterfaces");
        return false;
    }

    m_superinterfaces = (Class_Super*)m_class_loader->
        Alloc(sizeof(Class_Super)*m_num_superinterfaces);
    // ppervov: FIXME: should throw OOME
    for (unsigned i=0; i<m_num_superinterfaces; i++) {
        uint16 interface_index;
        if(!cfs.parse_u2_be(&interface_index)) {
            REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse superinterface index");
            return false;
        }
        //
        //Verify that interface index is valid and entry in constant pool is of type CONSTANT_Class
        //See specification 4.2 about interfaces.
        if(!valid_cpi(this, interface_index, CONSTANT_Class, "for superinterface"))
            return false;
        if(!valid_cpi(this, m_const_pool.get_class_name_index(interface_index), CONSTANT_Utf8, "for superinterface name"))
            return false;
        m_superinterfaces[i].name = m_const_pool.get_utf8_string(m_const_pool.get_class_name_index(interface_index));
        m_superinterfaces[i].cp_index = interface_index;
    }
    return true;
} // Class::parse_interfaces


/*
 *  Parses and verifies the classfile. Format is (from JVM spec):
 *
 *    ClassFile {
 *      u4 magic;
 *      u2 minor_version;
 *      u2 major_version;
 *      u2 constant_pool_count;
 *      cp_info constant_pool[constant_pool_count-1];
 *      u2 access_flags;
 *      u2 this_class;
 *      u2 super_class;
 *      u2 interfaces_count;
 *      u2 interfaces[interfaces_count];
 *      u2 fields_count;
 *      field_info fields[fields_count];
 *      u2 methods_count;
 *      method_info methods[methods_count];
 *      u2 attributes_count;
 *      attribute_info attributes[attributes_count];
 *   }
 */
bool Class::parse(Global_Env* env,
                  ByteReader& cfs)
{
    /*
     *  find out if classloader is system or user defined
     */
    bool is_trusted_cl = is_trusted_classloader(m_class_loader->GetName());
    TRACE2("classloader.name", "classloader_name: " << m_class_loader->GetName() << " is trusted: " << is_trusted_cl);
    
    /*
     *  get and check magic number (Oxcafebabe)
     */
    U_32 magic;
    if (!cfs.parse_u4_be(&magic)) {
        REPORT_FAILED_CLASS_FORMAT(this, "class is not a valid Java class file");
        return false;
    }

    //See 4.2 in specification about value of magic number
    if (magic != CLASSFILE_MAGIC) {
        REPORT_FAILED_CLASS_FORMAT(this, "invalid magic");
        return false;
    }

    /*
     *  get and check major/minor version of classfile
     *  1.1 (45.0-3) 1.2 (46.???) 1.3 (47.???) 1.4 (48.?) 5 (49.0)
     *  See 4.2 in specification about minor_version, major_version of classfile.
     */
    uint16 minor_version;
    if (!cfs.parse_u2_be(&minor_version)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse minor version");
        return false;
    }

    if (!cfs.parse_u2_be(&m_version)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse major version");
        return false;
    }
    //See comment in specification 4.2 about supported versions.
    if (!(m_version >= CLASSFILE_MAJOR_MIN
        && m_version <= CLASSFILE_MAJOR_MAX))
    {
        REPORT_FAILED_CLASS_CLASS(m_class_loader, this, "java/lang/UnsupportedClassVersionError",
            "class has version number " << m_version);
        return false;
    }

    if(m_version == JAVA5_CLASS_FILE_VERSION && minor_version > 0)
    {
        REPORT_FAILED_CLASS_FORMAT(this, "unsupported class file version "
            << m_version << "." << minor_version);
        return false;
    }
    /*
     *  allocate and parse constant pool
     */
    if(!m_const_pool.parse(this, env->string_pool, cfs))
        return false;

    /*
     * check and preprocess the constant pool
     */
    if(!m_const_pool.check(env, this, is_trusted_cl))
        return false;

    /*
    *  parse access flags
    */
    if(!cfs.parse_u2_be(&m_access_flags)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse access flags");
        return false;
    }

    //If class is interface, it must be abstract.
    //See specification 4.2 about access_flags.
    if(is_interface()) {
        // NOTE: Fix for the statement that an interface should have
        // abstract flag set.
        // spec/harness/BenchmarkDone has interface flag, but it does not
        // have abstract flag.
        m_access_flags |= ACC_ABSTRACT;
    }

    //for class file version lower than 49 these three flags should be set to zero
    //See specification 4.5 Fields, for 1.4 Java.
    if(m_version < JAVA5_CLASS_FILE_VERSION) {
        m_access_flags &= ~(ACC_SYNTHETIC | ACC_ENUM | ACC_ANNOTATION);
    }

    /*
     *   can't be both final and interface, or both final and abstract
     *   See specification 4.2 about access_flags.
     */
    if(is_final() && is_interface())
    {
        REPORT_FAILED_CLASS_FORMAT(this, "interface cannot be final");
        return false;
    }
    // not only ACC_FINAL flag is prohibited if is_interface, also
    // ACC_SYNTHETIC and ACC_ENUM. But in Java6 there appears to be an
    // exception to this rule, the package annotation interfaces
    // package-info can be interfaces with synthetic flag. So the
    // check is different for different class file versions
    if ((m_version <= JAVA5_CLASS_FILE_VERSION && is_interface() && (is_synthetic() || is_enum())) ||
        (m_version == JAVA6_CLASS_FILE_VERSION && is_interface() && is_enum()))
    {
        REPORT_FAILED_CLASS_FORMAT(this,
            "if class is interface, no flags except ACC_ABSTRACT or ACC_PUBLIC can be set");
        return false;
    }

    if(is_final() && is_abstract()) {
        REPORT_FAILED_CLASS_FORMAT(this, "abstract class cannot be final");
        return false;
    }

    if(is_annotation() && !is_interface())
    {
        REPORT_FAILED_CLASS_FORMAT(this, "annotation type must be interface");
        return false;
    }

    if(!is_interface() && is_annotation())
    {
        REPORT_FAILED_CLASS_FORMAT(this, "not interface can't be annotation");
        return false;
    }

    /*
     * parse this_class & super_class & verify their constant pool entries
     */
    uint16 this_class;
    if (!cfs.parse_u2_be(&this_class)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse this class index");
        return false;
    }

    //See specification 4.2 about this_class.
    if(!valid_cpi(this, this_class, CONSTANT_Class, "for this class"))
        return false;
    if(!valid_cpi(this, m_const_pool.get_class_name_index(this_class), CONSTANT_Utf8, "for this class name"))
        return false;
    String * class_name = m_const_pool.get_utf8_string(m_const_pool.get_class_name_index(this_class));

    /*
     * When defineClass from byte stream, there are cases that clss->name is null,
     * so we should add a check here
     */
    if(m_name != NULL && class_name != m_name) {
        REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
            VM_Global_State::loader_env->JavaLangNoClassDefFoundError_String->bytes,
            m_name->bytes << ": class name in class data does not match class name passed");
        return false;
    }

    if(m_name == NULL) {
        m_name = class_name;
    }

    /*
     *  Mark the current class as resolved.
     */
    m_const_pool.resolve_entry(this_class, this);

    /*
     * parse the super class name
     */
    uint16 super_class;
    if (!cfs.parse_u2_be(&super_class)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse super class index");
        return false;
    }

    m_super_class.cp_index = super_class;
    if (super_class == 0) {
        //
        // This class must represent java.lang.Object
        // See 4.2 in specification about super_class.
        //
        if(m_name != env->JavaLangObject_String) {
            REPORT_FAILED_CLASS_FORMAT(this, " class does not contain super class "
                << "but is not java.lang.Object class.");
            return false;
        }
        m_super_class.name = NULL;
    } else {
        if(!valid_cpi(this, super_class, CONSTANT_Class, "for super class"))
            return false;
        if(!valid_cpi(this, m_const_pool.get_class_name_index(super_class), CONSTANT_Utf8, "for super class name"))
            return false;

        String* super_name = m_const_pool.get_utf8_string(m_const_pool.get_class_name_index(super_class));
        if(!check_class_name(super_name->bytes, super_name->len, m_version < JAVA5_CLASS_FILE_VERSION)) {
            REPORT_FAILED_CLASS_FORMAT(this, " Illegal super class name "
                    << super_name->bytes);
            return false;
        }
        m_super_class.name = super_name;

        if(is_interface() && m_super_class.name != env->JavaLangObject_String){
            REPORT_FAILED_CLASS_FORMAT(this, " the super class of interface is "
                << m_super_class.name << "; must be java/lang/Object");
            return false;
        }
    }

    /*
     * allocate and parse class' interfaces
     */
    if(!parse_interfaces(cfs))
        return false;

    /*
     *  allocate and parse class' fields
     */
    if(!parse_fields(env, cfs, is_trusted_cl))
        return false;

    /*
     *  allocate and parse class' methods
     */
    if(!parse_methods(env, cfs, is_trusted_cl))
        return false;
    /*
     *  parse attributes
     */
    uint16 n_attrs;
    if (!cfs.parse_u2_be(&n_attrs)) {
        REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse number of attributes");
        return false;
    }
    unsigned numSourceFile = 0;
    unsigned numSourceDebugExtensions = 0;
    unsigned numEnclosingMethods = 0;
    unsigned numRuntimeVisibleAnnotations = 0;
    unsigned numRuntimeInvisibleAnnotations = 0;
    U_32 attr_len = 0;

    for (unsigned i=0; i<n_attrs; i++) {
        Attributes cur_attr = parse_attribute(this, cfs, class_attrs, &attr_len);
        switch(cur_attr){
        case ATTR_SourceFile:
        {
            // a class file can have at most one source file attribute
            numSourceFile++;
            if (numSourceFile > 1) {
                REPORT_FAILED_CLASS_FORMAT(this, "there is more than one SourceFile attribute");
                return false;
            }

            // attribute length must be two (vm spec 4.8.2)
            if (attr_len != 2) {
                REPORT_FAILED_CLASS_FORMAT(this, " SourceFile attribute has incorrect length ("
                    << attr_len << " bytes, should be 2 bytes)");
                return false;
            }

            // constant value attribute
            uint16 filename_index;
            if(!cfs.parse_u2_be(&filename_index)) {
                REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse filename index"
                    << " while parsing SourceFile attribute");
                return false;
            }
            
            if(!valid_cpi(this, filename_index, CONSTANT_Utf8, "for source file name at SourceFile attribute")) {
                return false;
            }
            m_src_file_name = m_const_pool.get_utf8_string(filename_index);
            break;
        }

        case ATTR_InnerClasses:
        {
            //See specification 4.8.5 about InnerClasses Attribute
            if (m_declaring_class_index || m_innerclasses) {
                REPORT_FAILED_CLASS_FORMAT(this, "more than one InnerClasses attribute");
                return false;
            }
            bool isinner = false;
            // found_myself == 2: myself is not inner class or has passed myself when iterating inner class attribute arrays
            // found_myself == 1: myself is inner class, current index of inner class attribute arrays is just myself
            // found_myself == 0: myself is inner class, hasn't met myself in inner class attribute arrays
            int found_myself = 2;
            if(strchr(m_name->bytes, '$')){
                isinner = true;
                found_myself = 0;
            }

            unsigned read_len = 0;
            //Only handle inner class
            uint16 num_of_classes;
            if(!cfs.parse_u2_be(&num_of_classes)) {
                REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse "
                    << "number of classes while parsing InnerClasses attribute");
                return false;
            }
            read_len += 2;

            if(isinner)
                m_num_innerclasses = (uint16)(num_of_classes - 1); //exclude itself
            else
                m_num_innerclasses = num_of_classes;
            if(num_of_classes)
                m_innerclasses = (InnerClass*) m_class_loader->
                    Alloc(2*sizeof(InnerClass)*m_num_innerclasses);
                // ppervov: FIXME: should throw OOME
            int index = 0;
            for(int i = 0; i < num_of_classes; i++){
                uint16 inner_clss_info_idx;
                if(!cfs.parse_u2_be(&inner_clss_info_idx)) {
                    REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse "
                        << "inner class info index while parsing InnerClasses attribute");
                    return false;
                }
                if(inner_clss_info_idx
                    && !valid_cpi(this, inner_clss_info_idx, CONSTANT_Class, "for inner class at InnerClasses attribute"))
                {
                    return false;
                }

                if(!found_myself){
                    if(!valid_cpi(this,m_const_pool.get_class_name_index(inner_clss_info_idx),
                            CONSTANT_Utf8, "for inner class name at InnerClasses attribute"))
                        return false;
                    String* clssname = m_const_pool.get_utf8_string(m_const_pool.get_class_name_index(inner_clss_info_idx));
                    // Only handle this class
                    if(m_name == clssname)
                        found_myself = 1;
                }
                if(found_myself != 1)
                    m_innerclasses[index].index = inner_clss_info_idx;

                uint16 outer_clss_info_idx;
                if(!cfs.parse_u2_be(&outer_clss_info_idx)) {
                    REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse "
                        << "outer class info index while parsing InnerClasses attribute");
                    return false;
                }
                if(outer_clss_info_idx
                    && !valid_cpi(this, outer_clss_info_idx, CONSTANT_Class, "for outer class at InnerClasses attribute"))
                {
                    return false;
                }
                if(found_myself == 1 && outer_clss_info_idx){
                    m_declaring_class_index = outer_clss_info_idx;
                }

                uint16 inner_name_idx;
                if(!cfs.parse_u2_be(&inner_name_idx)) {
                    REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse "
                        << "inner name index while parsing InnerClasses attribute");
                    return false;
                }
                if(inner_name_idx && !valid_cpi(this, inner_name_idx, CONSTANT_Utf8,
                        "for inner name of InnerClass attribute"))
                {
                    return false;
                }
                if(found_myself == 1){
                    if (inner_name_idx) {
                        m_simple_name = m_const_pool.get_utf8_string(inner_name_idx);
                    } else {
                        //anonymous class
                        m_simple_name = env->string_pool.lookup("");
                    }
                }

                uint16 inner_clss_access_flag;
                if(!cfs.parse_u2_be(&inner_clss_access_flag)) {
                    REPORT_FAILED_CLASS_FORMAT(this, "truncated class file: failed to parse "
                        << "inner class access flags while parsing InnerClasses attribute");
                    return false;
                }
                if(found_myself == 1) {
                    found_myself = 2;
                    m_access_flags = inner_clss_access_flag;
                } else
                    m_innerclasses[index++].access_flags = inner_clss_access_flag;
            } // for num_of_classes
            read_len += num_of_classes * 8;
            if(read_len != attr_len){
                REPORT_FAILED_CLASS_FORMAT(this,
                    "unexpected length of InnerClass attribute: " << read_len << ", expected: " << attr_len);
                return false;
            }
        }break; //case ATTR_InnerClasses
        case ATTR_SourceDebugExtension:
            {
                // attribute length is already recorded in attr_len
                // now reading debug extension information
                numSourceDebugExtensions++;
                if( numSourceDebugExtensions > 1 ) {
                    REPORT_FAILED_CLASS_FORMAT(this, " there is more than one SourceDebugExtension attribute");
                    return false;
                }

                // cfs is at debug_extension[] which is:
                //      The debug_extension array holds a string, which must be in UTF-8 format.
                //      There is no terminating zero byte.
                m_sourceDebugExtension = class_file_parse_utf8data(env->string_pool, cfs, attr_len);
                if(!m_sourceDebugExtension) {
                    REPORT_FAILED_CLASS_FORMAT(this, "invalid SourceDebugExtension attribute");
                    return false;
                }
            }
            break;

        case ATTR_EnclosingMethod:
            {
                //See specification 4.8.6
                numEnclosingMethods++;
                if ( numEnclosingMethods > 1 ) {
                    REPORT_FAILED_CLASS_FORMAT(this, "more than one EnclosingMethod attribute");
                    return false;
                }
                if (attr_len != 4) {
                    REPORT_FAILED_CLASS_FORMAT(this,
                        "unexpected length of EnclosingMethod attribute: " << attr_len);
                    return false;
                }
                uint16 class_idx;
                if(!cfs.parse_u2_be(&class_idx)) {
                    REPORT_FAILED_CLASS_FORMAT(this,
                        "could not parse class index of EnclosingMethod attribute");
                    return false;
                }
                if(!valid_cpi(this, class_idx, CONSTANT_Class,
                        "for EnclosingMethod attribute"))
                {
                    return false;
                }
                m_enclosing_class_index = class_idx;
                //See specification 4.8.6 about method_index.
                uint16 method_idx;
                if(!cfs.parse_u2_be(&method_idx)) {
                    REPORT_FAILED_CLASS_FORMAT(this,
                        "could not parse method index of EnclosingMethod attribute");
                    return false;
                }
                if(method_idx && !valid_cpi(this, method_idx, CONSTANT_NameAndType,
                        "for EnclosingMethod attribute"))
                {
                    return false;
                }
                m_enclosing_method_index = method_idx;
            }
            break;

        case ATTR_Synthetic:
            {
                if(attr_len != 0) {
                    REPORT_FAILED_CLASS_FORMAT(this,
                        "attribute Synthetic has non-zero length");
                    return false;
                }
                m_access_flags |= ACC_SYNTHETIC;
            }
            break;

        case ATTR_Deprecated:
            {
                if(attr_len != 0) {
                    REPORT_FAILED_CLASS_FORMAT(this,
                        "attribute Deprecated has non-zero length");
                    return false;
                }
                m_deprecated = true;
            }
            break;

        case ATTR_Signature:
            {
                if(m_signature != NULL) {
                    REPORT_FAILED_CLASS_FORMAT(this,
                        "more than one Signature attribute for the class");
                    return false;
                }
                if (!(m_signature = parse_signature_attr(cfs, attr_len, this))) {
                    return false;
                }
            }
            break;

        case ATTR_RuntimeVisibleAnnotations:
            {
                //ClassFile may contain at most one RuntimeVisibleAnnotations attribute.
                // See specification 4.8.14.
                numRuntimeVisibleAnnotations++;
                if(numRuntimeVisibleAnnotations > 1) {
                    REPORT_FAILED_CLASS_FORMAT(this,
                        "more than one RuntimeVisibleAnnotations attribute");
                    return false;
                }
                U_32 read_len = parse_annotation_table(&m_annotations, cfs, this);
                if(attr_len == 0)
                    return false;
                if (attr_len != read_len) {
                    REPORT_FAILED_CLASS_FORMAT(this,
                        "error parsing RuntimeVisibleAnnotations attribute"
                        << "; declared length " << attr_len
                        << " does not match actual " << read_len);
                    return false;
                }
            }
            break;

        case ATTR_RuntimeInvisibleAnnotations:
            {
                if(env->retain_invisible_annotations) {
                    //ClassFile may contain at most one RuntimeInvisibleAnnotations attribute.
                    numRuntimeInvisibleAnnotations++;
                    if(numRuntimeInvisibleAnnotations > 1) {
                        REPORT_FAILED_CLASS_FORMAT(this,
                            "more than one RuntimeInvisibleAnnotations attribute");
                        return false;
                    }
                    U_32 read_len = parse_annotation_table(&m_invisible_annotations, cfs, this);
                    if(read_len == 0)
                        return false;
                    if (attr_len != read_len) {
                        REPORT_FAILED_CLASS_FORMAT(this,
                            "error parsing RuntimeInvisibleAnnotations attribute"
                            << "; declared length " << attr_len
                            << " does not match actual " << read_len);
                        return false;
                    }
                }else {
                    if(!cfs.skip(attr_len)) {
                        REPORT_FAILED_CLASS_FORMAT(this,
                            "Truncated class file");
                        return false;
                    }
                }
            }
            break;

        case ATTR_UNDEF:
            // unrecognized attribute; skipped
            break;
        case ATTR_ERROR:
            return false;
            break;
        default:
            // error occured
            REPORT_FAILED_CLASS_CLASS(m_class_loader, this, "java/lang/InternalError",
                m_name->bytes << ": unknown error occured"
                " while parsing attributes for class"
                << "; unprocessed attribute " << cur_attr);
            return false;
        } // switch
    } // for

    if (cfs.have(1)) {
        REPORT_FAILED_CLASS_FORMAT(this, "Extra bytes at the end of class file");
        return false;
    }

    if (m_enclosing_class_index && m_simple_name == NULL) {
        LWARN(3, "Attention: EnclosingMethod attribute does not imply "
            "InnerClasses presence for class {0}" << m_name->bytes);
    }


    return true;
} // Class::parse


static bool const_pool_find_entry(ByteReader& cp, uint16 cp_count, uint16 index)
{
    U_8 tag;
    // cp must be at the beginning of constant pool
    for(uint16 cp_index = 1; cp_index < cp_count; cp_index++) {
        if(cp_index == index) return true;
        if(!cp.parse_u1(&tag))
            return false;
        switch(tag) {
            case CONSTANT_Class:
                if(!cp.skip(2))
                    return false;
                break;
            case CONSTANT_Fieldref:
            case CONSTANT_Methodref:
            case CONSTANT_InterfaceMethodref:
                if(!cp.skip(4))
                    return false;
                break;
            case CONSTANT_String:
                if(!cp.skip(2))
                    return false;
                break;
            case CONSTANT_Integer:
            case CONSTANT_Float:
                if(!cp.skip(4))
                    return false;
                break;
            case CONSTANT_Long:
            case CONSTANT_Double:
                if(!cp.skip(8))
                    return false;
                cp_index++;
                break;
            case CONSTANT_NameAndType:
                if(!cp.skip(4))
                    return false;
                break;
            case CONSTANT_Utf8:
                {
                    uint16 dummy16;
                    if(!cp.parse_u2_be(&dummy16))
                        return false;
                    if(!cp.skip(dummy16))
                        return false;
                }
                break;
        }
    }

    return false; // not found
}


const String* class_extract_name(Global_Env* env,
                                 U_8* buffer, unsigned offset, unsigned length)
{
    ByteReader cfs(buffer, offset, length);

    U_32 magic;
    // check magic
    if(!cfs.parse_u4_be(&magic) || magic != CLASSFILE_MAGIC)
        return NULL;

    // skip minor_version and major_version
    if(!cfs.skip(4))
        return NULL;

    uint16 cp_count;
    // get constant pool entry number
    if(!cfs.parse_u2_be(&cp_count))
        return NULL;

    // skip constant pool
    U_8 tag;
    uint16 utf8_len;
    offset = cfs.get_offset(); // offset now contains the start of constant pool
    uint16 cp_index;
    for(cp_index = 1; cp_index < cp_count; cp_index++) {
        if(!cfs.parse_u1(&tag))
            return NULL;
        switch(tag) {
            case CONSTANT_Class:
                if(!cfs.skip(2))
                    return NULL;
                break;
            case CONSTANT_Fieldref:
            case CONSTANT_Methodref:
            case CONSTANT_InterfaceMethodref:
                if(!cfs.skip(4))
                    return NULL;
                break;
            case CONSTANT_String:
                if(!cfs.skip(2))
                    return NULL;
                break;
            case CONSTANT_Integer:
            case CONSTANT_Float:
                if(!cfs.skip(4))
                    return NULL;
                break;
            case CONSTANT_Long:
            case CONSTANT_Double:
                if(!cfs.skip(8))
                    return NULL;
                cp_index++;
                break;
            case CONSTANT_NameAndType:
                if(!cfs.skip(4))
                    return NULL;
                break;
            case CONSTANT_Utf8:
                if(!cfs.parse_u2_be(&utf8_len))
                    return NULL;
                if(!cfs.skip(utf8_len))
                    return NULL;
                break;
        }
    }

    // skip access_flags
    if(!cfs.skip(2))
        return NULL;

    // get this_index in constant pool
    uint16 this_class_idx;
    if(!cfs.parse_u2_be(&this_class_idx))
        return NULL;

    // find needed entry
    if(!cfs.go_to_offset(offset))
        return NULL;
    if(!const_pool_find_entry(cfs, cp_count, this_class_idx))
        return NULL;

    // now cfs is at CONSTANT_Class entry
    if(!cfs.parse_u1(&tag) && tag != CONSTANT_Class)
        return NULL;
    // set this_class_idx to class_name index in constant pool
    if(!cfs.parse_u2_be(&this_class_idx))
        return NULL;

    // find entry class_name
    if(!cfs.go_to_offset(offset))
        return NULL;
    if(!const_pool_find_entry(cfs, cp_count, this_class_idx))
        return NULL;

    // now cfs is at CONSTANT_Utf8 entry
    if(!cfs.parse_u1(&tag) && tag != CONSTANT_Utf8)
        return NULL;
    // parse class name
    const String* class_name = class_file_parse_utf8(env->string_pool, cfs);
    return class_name;
}

Class *class_load_verify_prepare_by_loader_jni(Global_Env* env,
                                               const String* classname,
                                               ClassLoader* cl)
{
    assert(hythread_is_suspend_enabled());
    // if no class loader passed, re-route to bootstrap
    if(!cl) cl = env->bootstrap_class_loader;
    Class* clss = cl->LoadVerifyAndPrepareClass(env, classname);
    return clss;
}


Class *class_load_verify_prepare_from_jni(Global_Env *env, const String *classname)
{
    assert(hythread_is_suspend_enabled());
    Class *clss = env->bootstrap_class_loader->LoadVerifyAndPrepareClass(env, classname);
    return clss;
}
