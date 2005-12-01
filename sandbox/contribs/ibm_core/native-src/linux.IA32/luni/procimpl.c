/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <signal.h>

#include <errno.h>

#include <sys/ioctl.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/wait.h>

#include "procimpl.h"

void
sleepFor (unsigned int nanoseconds)
{
  struct timespec delay, remDelay;
  delay.tv_sec = 0;
  delay.tv_nsec = nanoseconds;

  while (nanosleep (&delay, &remDelay) == -1)
    {
      if (errno == EINTR)
        {
          delay.tv_nsec = remDelay.tv_nsec;     /* tv_sec is zero */
        }
      else
        {
          break;    /* Oops the sleep didn't work ??? */
        }
    }
}

int
termProc (IDATA procHandle)
{
  int rc;

  rc = kill ((pid_t) procHandle, SIGTERM);
  return rc;
}

int
waitForProc (IDATA procHandle)
{
  int StatusLocation = -1;

  waitpid ((pid_t) procHandle, &StatusLocation, 0);
  if (WIFEXITED (StatusLocation) != 0)
    {
      StatusLocation = WEXITSTATUS (StatusLocation);
    }
  else
    {
      if (WIFSIGNALED (StatusLocation) != 0)
        {
          StatusLocation = WTERMSIG (StatusLocation);
        }
      else
        {
          if (WIFSTOPPED (StatusLocation) != 0)
            {
              StatusLocation = WSTOPSIG (StatusLocation);
            }
        }
    }

  return StatusLocation;
}

int
execProgram (JNIEnv * vmthread, jobject recv,
             char *command[], int commandLineLength,
             char *env[], int envSize, char *dir, IDATA * procHandle,
             IDATA * inHandle, IDATA * outHandle, IDATA * errHandle)
{
  /* It is illegal to pass JNIEnv accross threads, so get the vm while
   * we will go across another thread. The javaObject recv is used in
   * the new thread  ==> make it a globalRef.
   */

  char result;
  char *cmd;
  int grdpid, rc = 0;
  int newFD[3][2];
  int execvFailure[2];
  int forkedChildIsRunning[2];

  /* Build the new io pipes (in/out/err) */
  pipe (newFD[0]);
  pipe (newFD[1]);
  pipe (newFD[2]);

  /* pipes for synchronization */
  pipe (forkedChildIsRunning);
  pipe (execvFailure);

  cmd = command[0];
  grdpid = fork ();
  if (grdpid == 0)
    {
      /* Redirect pipes so grand-child inherits new pipes */
      char dummy = '\0';
      dup2 (newFD[0][0], 0);
      dup2 (newFD[1][1], 1);
      dup2 (newFD[2][1], 2);
      /* tells the parent that that very process is running */
      write (forkedChildIsRunning[1], &dummy, 1);

      if (dir)
        chdir (dir);

      /* ===try to perform the execv : on success, it does not return ===== */
      if (envSize == 0)
        rc = execvp (cmd, command);
      else
        rc = execve (cmd, command, env);
      /* ===================================================== */

      /* if we get here ==> tell the parent that the execv failed ! */
      write (execvFailure[1], &dummy, 1);
      /* If the exec failed, we must exit or there will be two VM processes running. */
      exit (rc);
    }
  else
    {
      /* in the child-thread (not the grand-child) */
      int stat_val = -1;
      jfieldID hid;
      jmethodID mid;
      jclass rcvClass;
      char dummy;
      int avail = 0;
      int noDataInThePipe;
      int nbLoop;

      close (newFD[0][0]);
      close (newFD[1][1]);
      close (newFD[2][1]);
      /* Store the rw handles to the childs io */
      *(inHandle) = (IDATA) newFD[0][1];
      *(outHandle) = (IDATA) newFD[1][0];
      *(errHandle) = (IDATA) newFD[2][0];
      *(procHandle) = (IDATA) grdpid;

      /* let the forked child start. */
      read (forkedChildIsRunning[0], &dummy, 1);
      close (forkedChildIsRunning[0]);
      close (forkedChildIsRunning[1]);

      /* Use the POSIX setpgid and its errno EACCES to detect the success of the execv function. When the feature is
         not present on the platform, a delay is provided after which we conclude that if the execv didn't fail, it
         must have propably succeeded. We loop on reading a pipe which will receive a byte if the execv fails. We
         also break from the loop, if we have detected the success of the execv (or past a delay if the functionaly
         is not present) */

      rc = 0;                   /* at first glance, the execv will succeed (-1 is for failure) */
      noDataInThePipe = 1;
      ioctl (execvFailure[0], FIONREAD, &avail);
      if (avail > 0)
        {
          rc = -1;              /* failure of the execv */
          noDataInThePipe = 0;
        }
      nbLoop = 0;
      while (noDataInThePipe)
        {
          int setgpidResult;
          /* =======give the child a chance to run=========== */
          sleepFor (10000000);  /* 10 ms */
          /*========== probe the child for success of the execv ========*/
          setgpidResult = setpgid (grdpid, grdpid);
          if (setgpidResult == -1)
            {
              if (errno == EACCES)
                {
                  /* fprintf(stdout,"\nSUCCESS DETECTED\n");fflush(stdout); */
                  break;        /* success of the execv */
                }
              else
                {
                  /* setgpid is probably not supported . Give some a bit of time to the child to tell us if it has
                     failed to launch the execv */
                  nbLoop++;
                  if (nbLoop > 10)
                    {
                      break;    /* well, execv has probably succeeded */
                    }
                }
            }
          /* =========Has a byte arrived in the pipe ? (failure test) ========= */
          ioctl (execvFailure[0], FIONREAD, &avail);
          if (avail > 0)
            {
              rc = -1;          /* failure of the execv */
              noDataInThePipe = 0;
            }
        } /* end of the loop. rc==-1 iff the execv failed */

      /* if (rc==-1){ fprintf(stdout,"\nFAILURE DETECTED\n");fflush(stdout); } */

      close (execvFailure[0]);
      close (execvFailure[1]);

      /* tells the parent what result it should return */
      if ((grdpid < 0) || (rc == -1))
        {
          result = (char) 0;
        }
      else
        {
          result = (char) 1;
        }
    }
  return (int) result;          /* 0 or 1 */
}

/* Stream handling support */
/* Return the number of bytes available to be read without blocking */
int
getAvailable (IDATA sHandle)
{
  int avail, rc;
  rc = ioctl ((int) sHandle, FIONREAD, &avail);
  if (rc == -1)
    return -2;
  return avail;
}

int
closeProc (IDATA procHandle)
{
  /* The procHandle (Process ID) should not be closed, as it isn't a file descriptor. */
  return 0;
}
