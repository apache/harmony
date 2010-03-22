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
 * @author Alexander Astapchuk
 */
 
/**
 * @file 
 * @brief Declaration of MethodInfoBlock.
 */

#if !defined(__MIB_H_INCLUDED__)
#define __MIB_H_INCLUDED__

#include "enc.h"
#include "open/rt_types.h"

namespace Jitrino {
namespace Jet {

/**
 * @brief Controls generic information about compiled method.
 *
 * The MethodInfoBlock instance manages a general (platform-independent) 
 * info about compiled method. This info includes: flags the method was 
 * compiled with, max stack depth, number of local variables and size of 
 * input argsuments, sizes of byte code and native code, and a mapping 
 * between native and byte code.
 *
 * The MethodInfoBlock instances used during both compilation and execution.
 *
 * When used during compile time, an instance of MethodInfoBlock \link #init
 * allocates \endlink memory (via malloc call) to temporary store method's 
 * data. During compilation, this temporary data is filled with method's 
 * information. Then, Compiler allocates the same amount of memory (method's
 * info block) via VM's call, and \link #save copies \endlink the data there.
 * The memory is then must be deallocated explicitly by #release call.
 *
 * When data is stored into a buffer, it stores 
 * \link MethodInfoBlock::BlockHeader a header of fixed size \endlink, and
 * then variable size data. Currently, only a mapping between byte code and
 * native addresses is stored as variable sized data.
 *
 * When used during runtime, the instances of MethodInfoBlock are normally 
 * \link #MethodInfoBlock(char * pdata) initialized\endlink from the info 
 * block.
 *
 * A mapping between native and byte code (#rt_inst_addrs) is organized as 
 * a plain array of \b char* type and with the number of elements equal to 
 * the size of bytecode.
 *
 * For each bytecode instruction, given its program counter (PC), an array 
 * element at [PC] points to the beginning of the native code for this
 * instruction. If the bytecode instruction occupies more than one byte, 
 * say, PC+1, ..., PC+N, then corresponding array elements at PC+1, ... 
 * PC+N, also point to the beginning of the native code for the instruction.
 * 
 * Finding an IP by a given PC is trivial, it's \a rt_inst_addrs[PC].
 *
 * Finding an PC provided with an IP, requires a seach in the rt_inst_addrs
 * array. Luckily, layout of the native code is exactly the same as byte 
 * code layout, so the \a rt_inst_addrs array is surely sorted in ascending
 * order. The binary search is implemented in #get_pc.
 *
 * @todo the size of info block is known at the very beginning. may 
 *       eliminate this additional malloc()+free() during compilation, so
 *       allocate the info block, and then operate directly with it.
 */
class MethodInfoBlock {
public:
    /**
     * @brief Initializes an empty instance of MethodInfoBlock.
     */
    MethodInfoBlock(void);

    /**
     * @brief Loads info about the method.
     *
     * Initializes instance of MethodInfoBlock by \link #load loading \endlink 
     * info from \a pdata, provided that \a pdata points to a valid method's 
     * data.
     *
     * @param pdata - pointer to a data to load from
     */
    MethodInfoBlock(char * pdata) {
        assert(is_valid_data(pdata));
        m_data = NULL;
		num_profiler_counters = 0;
		profiler_counters_map = NULL;
        load(pdata);
    }

    /**
     * @brief Cleanup, deallocates memory allocated for data.
     */
    ~MethodInfoBlock()
    {
		assert (m_data == NULL);
    }

    /**
     * @brief Tests whether the provided pointer points to a valid method's 
     *        data (method's info block).
     *
     * @param p - a pointer to data to test
     * @return \b true if data recognized as valid, \b false otherwise.
     */
    static bool is_valid_data(const char * p)
    {
        return MAGIC == ((BlockHeader*)p)->magic;
    }

    /**
     * @brief Initializes an instance of MethodInfoBlock, and allocates memory 
     *        to temporary store data during compilation.
     *
     * @note If #init() called, then 
     *       <span style="color: red; text-decoration: underline">caller
     *       must explicitly call #release()</span> method before object
     *       goes out of scope. This is to avoid call of 'delete[]' in 
     *       destructor which slows down runtime stuff, i.e. #rt_unwind().
     *
     * @param bc_size - size of byte code
     * @param stack_max - max stack depth
     * @param num_locals - number of slots occupied by local variables
     * @param in_slots - number of slots occupied by input args
     * @param flags - flags, the method was compiled with
     */
    void init(unsigned bc_size, unsigned stack_max, unsigned num_locals,
              unsigned in_slots, unsigned flags);
    /**
     * @brief Releases resources allocated by #init().
     *
     * @note If #init() called, then 
     *       <span style="color: red; text-decoration: underline">caller
     *       must explicitly call #release()</span> method before object
     *       goes out of scope. This is to avoid call of 'delete[]' in 
     *       destructor which slows down runtime stuff, i.e. #rt_unwind().
     */
    void release(void)
    {
        delete[] m_data;
        m_data = NULL;
    }

    /**
     * @brief Sets a native address for the given bytecode PC.
     * @param pc - bytecode program counter
     * @param addr - native address for the given bytecode
     */
    void set_code_info(unsigned pc, const char * addr)
    {
        rt_inst_addrs[pc] = addr;
    }

    /**
     * @brief Returns an IP previously stored by #set_code_info.
     * @param pc - byte code PC
     * @return an IP stored for the given PC
     */
    const char* get_code_info(unsigned pc) const
    {
        return rt_inst_addrs[pc];
    }

    /**
     * @brief Returns number of slots occupied by input args.
     * @return number of slots occupied by input args.
     */
    unsigned get_in_slots(void) const
    {
        return rt_header->m_in_slots;
    }

    /**
     * @brief Returns size of bytecode.
     * @return size of bytecode, in bytes
     */
    unsigned get_bc_size(void) const
    {
        return rt_header->m_bc_size;
    }

    /**
     * @brief Returns number of slots occupied by local variables.
     * @return number of slots occupied by local variables
     */
    unsigned get_num_locals(void) const
    {
        return rt_header->m_num_locals;
    }

    /**
     * @brief Returns max stack depth.
     * @return max stack depth
     */
    unsigned get_stack_max(void) const
    {
        return rt_header->m_max_stack_depth;
    }

    /**
     * @brief Returns flags the method compiled with.
     * @return flags the method was compiled with
     */
    unsigned get_flags(void) const
    {
        return rt_header->m_flags;
    }
    
    /**
     * @brief Sets flags the method flags.
     * @param _flags - flags the method compiled with
     */
    void set_flags(unsigned _flags) const
    {
        rt_header->m_flags = _flags;
    }

    /**
     * @brief Returns total size needed to store the whole runtime info.
     * @return size needed to store the whole runtime info
     */
    unsigned get_total_size(void) const
    {
        return get_hdr_size() + get_bcmap_size() + get_profile_counters_map_size();
    }
    /**
     * @brief Sets a length of 'warm-up code'.
     * @see BlockHeader::warmup_len
     */
    void set_warmup_len(unsigned len)
    {
        rt_header->warmup_len = len;
    }
    
    /**
     * @brief Returns a length of the 'warm-up code'.
     * @see BlockHeader::warmup_len
     */
    unsigned get_warmup_len(void) const
    {
        return rt_header->warmup_len;
    }
    
    /**
     * @brief Sets code start address.
     */
    void set_code_start(char* code)
    {
        rt_header->code_start = code;
    }
    
    /**
     * @brief Returns code start address.
     */
    char* get_code_start(void)
    {
        return rt_header->code_start;
    }

    /**
     * @brief Sets size of native code, in bytes.
     */
    void set_code_len(unsigned len)
    {
        rt_header->m_code_len = len;
    }
    
    /**
     * @brief Returns size of native code, in bytes.
     */
    unsigned get_code_len(void)
    {
        return rt_header->m_code_len;
    }

    void set_compile_params(const OpenMethodExecutionParams& compileParams)
    {
        rt_header->compileParams = compileParams;
    }

    OpenMethodExecutionParams get_compile_params(void) const
    {
        return rt_header->compileParams;
    }
    
    /**
     * @brief Copies MethodInfoBlock's data into a buffer.
     *
     * @note The provided buffer must be not less than #get_total_size
     *       bytes long. It's the caller's responsibility to ensure the 
     *       proper capacity.
     *
     * @param to - points to a buffer to copy data into
     */
    void    save(char * to);

    /**
     * @brief Loads MethodInfoBlock's data from a buffer.
     *
     * @note The \a from must point to \link #is_valid_data a valid 
     *       data \endlink. If this condition not met, behavior is 
     *       unpredictable.
     * @param from - points to a buffer to load data from
     */
    void    load(char * from)
    {
        // Actually, there must be no ways in Compiler when a 
        // MethodInfoBlock's instance initialized via init() call (which 
        // sets m_data) is later used with a load() call. 
        // This is not a problem itself, just *highly* unexpected usage of 
        // MethodInfoBlock. So, this assert() just to force this presumption.
        // If something changes, and there is a need to reuse the same 
        // MethodInfoBlock instance, then feel free to remove this assert().
        // and **UNCOMMENT the delete[]**
        assert(m_data == NULL);
		assert(profiler_counters_map == NULL);
		assert(num_profiler_counters == 0);
        //delete[] m_data; m_data = NULL; // just for sure
        
        rt_header = (BlockHeader*)from;
        rt_inst_addrs = (const char**)(from + get_hdr_size());
        
       // load profiling counters info
        char* countersInfo =  from + get_hdr_size() + get_bcmap_size();
        num_profiler_counters = *(U_32*)countersInfo;
		if (num_profiler_counters > 0) {
			profiler_counters_map = (U_32*)(countersInfo + sizeof(U_32));
		}
        profiler_counters_map = (U_32*)(countersInfo + sizeof(U_32));
    }

    /**
     * @brief Returns an IP for a given PC.
     * 
     * Returns address of the first native instruction generated for byte 
     * code instruction located at the provided program counter (PC).
     *
     * @note The provided PC must be valid (means be \link #get_bc_size in 
     *       range\endlink). Otherwise, behavior is unpredictable.
     * 
     * @param pc - bytecode program counter (PC)
     * @return IP for the given PC
     */
    const char *    get_ip(unsigned pc) const;

    /**
     * @brief Returns PC for a given IP.
     *
     * Returns a byte code program counter (PC) for a given native 
     * address (IP).
     *
     * @note The provided address must be valid (means must belong to the 
     *       code generated for this method). Otherwise, behavior is 
     *       unpredictable.
     *
     * @param ip - native IP
     * @return found PC
     */
    unsigned    get_pc(const char * ip) const;
    
    /**
     * @brief Sets a flag that the given AR was spilled during method's 
     *        prolog.
     */
    void saved(AR gr)
    {
        assert(word_no(ar_idx(gr))<COUNTOF(rt_header->saved_regs));
        unsigned * p = rt_header->saved_regs;
        set(p, ar_idx(gr));
    }
    
    /**
     * @brief Tests whether the given AR was spilled during method's 
     *        prolog.
     */
    bool is_saved(AR gr) const
    {
        assert(word_no(ar_idx(gr))<COUNTOF(rt_header->saved_regs));
        unsigned * p = rt_header->saved_regs;
        return tst(p, ar_idx(gr));
    }
    
    /**
     * @brief Returns direct pointer to the bit array with of saved
     *        registers.
     */
    const char * saved_map(void) const
    {
        return (const char*)rt_header->saved_regs;
    }
private:
    /**
     * @brief Returns size occupied by header.
     *
     * The header size is the same for all MethodInfoBlock instances.
     *
     * @return size occupied by header
     */
    static unsigned get_hdr_size(void)
    {
        return sizeof(BlockHeader);
    }

    /**
     * @brief Calculates and returns size occupied by bc-map related variable part of 
     *        MethodInfoBlock.
     *
     * @note Obviously, all fields used to calculate the variable size 
     *       must be initialized at this point. Currently, the variable
     *       part depends only on the value of BlockHeader::m_bc_size.
     *
     * @return size needed to store variable part of MethodInfoBlock.
     */
    unsigned        get_bcmap_size(void) const
    {
        return sizeof(rt_inst_addrs[0])*rt_header->m_bc_size;
    }

   /**
   * @brief Calculates and returns size occupied by profiling counters info in 
   *        MethodInfoBlock.
   *
   */
    unsigned get_profile_counters_map_size() const {
        return sizeof (U_32) + num_profiler_counters * sizeof(U_32);
    }

    /**
     * @brief Disallows copying.
     */
    MethodInfoBlock(const MethodInfoBlock&);
    
    /**
     * @brief Disallows copying.
     */
    MethodInfoBlock& operator=(const MethodInfoBlock&);

    /**
     * @brief Initializes data from a buffer.
     *
     * The method simply stores addresses, which point to a header and to
     * variable data.
     *
     * @note The buffer is not checked for validity.
     *
     * @param ptr - pointer to a buffer
     */
    void init_from_ptr(char * ptr);

    /**
     * @brief A signature used to control whether the info block is valid.
     *
     * Here, 'valid' means 'previously written via #save call' which, in turn,
     * implies 'the method was compiled by Jitrino.JET'.
     * 
     * The value itself is string \e .JET in hex.
     */
    static const unsigned MAGIC = 0x2E4A4554;

    /**
     * @brief A fixed header for the info controlled by #MethodInfoBlock.
     */
    struct BlockHeader {
        /**
         * @brief Data signature.
         *
         * This field is used to control whether the info is valid (means
         * stored by MethodInfoBlock). The valid data must have #MAGIC in 
         * this field.
         */
        unsigned  magic;
        /**
         * @brief Start address of native code.
         */
        char *          code_start;
        /**
         * @brief Size of native code, in bytes.
         */
        unsigned        m_code_len;
        /**
         * @brief Size of bytecode, in bytes.
         */
        unsigned        m_bc_size;
        /**
         * @brief Length of array of local variables.
         */
        unsigned        m_num_locals;
        /**
         * @brief Max depth of operand stack of the method.
         */
        unsigned        m_max_stack_depth;
        /**
         * @brief Number of slots occupied by input args.
         */
        unsigned        m_in_slots;
        /**
         * @brief Flags the method was compiled with.
         */
        unsigned    m_flags;
        /**
         * @brief A bit array of the flags whether a register was spilled 
         *        in method's prolog (and thus need to be restored during
         *        unwinding).
         */
        unsigned    saved_regs[words(ar_total+1)];
        /**
         * @brief Length of 'warm up code sequence'.
         *
         * The 'warm up code sequence' is very beginning of every method
         * compiled by Jitrino.JET. This sequence prepares native stack 
         * frame and spills out necessary callee-save registers. The code
         * does not change callee-save regs.
         *
         * If something terrible (i.e. stack overflow) happens during 
         * this sequence, then special unwinding procedure is performed - 
         * callee-save registers are not restored (as they are untouched).
         *
         * Beyond this point, a regular unwinding procedure is performed.
         */
        unsigned    warmup_len;

        OpenMethodExecutionParams compileParams;
    };
    /**
     * @brief Pointer to a temporary buffer. The buffer is allocated in 
     *        #init, and then deallocated in #~MethodInfoBlock.
     */
    char *          m_data;
    /**
     * @brief A header which is used during compile-time, to store the 
     *        info.
     */
    BlockHeader     m_header;
    /**
     * @brief Pointer to a header structure.
     * 
     * During compilation time it point to #m_header (set in #MethodInfoBlock), 
     * and during runtime it points into the buffer provided to #load.
     */
    BlockHeader *   rt_header;
    /**
     * @brief A mapping between byte code and native code addresses.
     *
     * The size is [bc_size].
     *
     * During compilation time, it points to a buffer in #m_data (set in 
     * #init). During runtime, it points into the buffer provided to #load.
     */
    const char **   rt_inst_addrs;  // [bc_size]

public:
   /**
   * Number of profile counters in the method
   */
    U_32   num_profiler_counters;
   /**
   * Profiling counters information for patching: counter size and offset
   */
    U_32*  profiler_counters_map;
};


}}; // ~namespace Jitrino::Jet

#endif      // ~__MIB_H_INCLUDED__

