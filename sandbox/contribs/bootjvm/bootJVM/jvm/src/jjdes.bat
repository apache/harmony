@echo off
rem echo jjdes :  param 1 = %1 
rem echo jjdes :  param 2 = %2 
rem echo jjdes :  param 3 = %3 
rem echo jjdes :  param 4 = %4 
rem echo jjdes :  param 5 = %5 
rem echo jjdes :  param 6 = %6 

cd %1
%2\bin\jar -xfv %3%4%5 %6
