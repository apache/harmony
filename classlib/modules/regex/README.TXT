KNOWN ISSUES
------------

- Unit tests for the beans package may fail in the Harmony self-hosting 
  environment due to the incomplete functionality of stub classes in awt
  and applet packages. The failing tests are:
      java.beans.EventHandlerTest
      java.beans.SimpleBeanInfoTest   

- The find() and findBack() methods in regex nodes may work incorrectly with back references. 
The issue may show up for such patterns as below: 
    .*(.)\\1
    (.)\\1

- The Node.match() method is not completely optimized in regex. It may call the CharSequence.toString() method 
even if CharacterSequence is not a string, which leads to a String creation during each call to 
this node’s match() method.

- Methods implementing bitwise operations (and, or, and xor) in the BigInteger class might seem complicated 
although they work rather efficiently. These methods might be rewritten in a more comprehensive form.  


TODO
----

- Implement canonical equivalence support in regex.

- Simplify multistep operations in the BigInteger class, which create 
multiple BigInteger objects on each step. The methods gcd (), isProbablePrime(), 
modInverse(), and modPow()can be speeded up with implementing safe in-place computations. 

- Enhance the multistep BigInteger.pow() method. To reduce the number of created 
objects, the method can be implemented by using the square-and-multiply algorithm. 
However, the proposed implementation works longer due to longer multiplication of large numbers.
The enhancement may consist of implementing safe in-place multiplication 
and possible combination of the two algorithms.

- Implement special cases for more efficient exponentiation of BigInteger.TEN and 
multiplication by using BigInteger.TEN. These mechanisms are widely used in the BigDecimal implementation.

- Optimize the BigInteger.toString() method.
