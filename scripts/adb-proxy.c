/*
 * WSL adb proxy (ELF) for Android Gradle / React Native on WSL.
 * - devices/version/get-state: Windows adb.exe, stdout CRLF stripped (AGP parsing)
 * - install/shell/reverse/...: Windows adb.exe, direct exec (binary-safe)
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

static const char *WIN_ADB =
    "/mnt/c/Users/odera/AppData/Local/Android/Sdk/platform-tools/adb.exe";

static char **build_exec_argv(int argc, char **argv) {
  char **exec_argv = malloc((size_t)(argc + 1) * sizeof(char *));
  if (!exec_argv) {
    return NULL;
  }
  exec_argv[0] = (char *)WIN_ADB;
  for (int i = 1; i < argc; i++) {
    exec_argv[i] = argv[i];
  }
  exec_argv[argc] = NULL;
  return exec_argv;
}

static int has_subcommand(int argc, char **argv, const char *cmd) {
  for (int i = 1; i < argc; i++) {
    if (strcmp(argv[i], cmd) == 0) {
      return 1;
    }
  }
  return 0;
}

static int needs_stdout_normalize(int argc, char **argv) {
  return has_subcommand(argc, argv, "devices") ||
         has_subcommand(argc, argv, "version") ||
         has_subcommand(argc, argv, "get-state") ||
         has_subcommand(argc, argv, "get-serialno") ||
         has_subcommand(argc, argv, "get-devpath");
}

static int run_piped(int argc, char **argv) {
  char **exec_argv = build_exec_argv(argc, argv);
  if (!exec_argv) {
    return 1;
  }

  int pipefd[2];
  if (pipe(pipefd) != 0) {
    perror("pipe");
    free(exec_argv);
    return 1;
  }

  pid_t child = fork();
  if (child < 0) {
    perror("fork");
    free(exec_argv);
    return 1;
  }

  if (child == 0) {
    close(pipefd[0]);
    dup2(pipefd[1], STDOUT_FILENO);
    close(pipefd[1]);
    execv(WIN_ADB, exec_argv);
    perror("execv");
    _exit(127);
  }

  close(pipefd[1]);
  FILE *in = fdopen(pipefd[0], "r");
  if (!in) {
    perror("fdopen");
    free(exec_argv);
    return 1;
  }

  int c;
  while ((c = fgetc(in)) != EOF) {
    if (c != '\r') {
      fputc(c, stdout);
    }
  }
  fclose(in);

  int status = 0;
  waitpid(child, &status, 0);
  free(exec_argv);

  if (WIFEXITED(status)) {
    return WEXITSTATUS(status);
  }
  return 1;
}

int main(int argc, char **argv) {
  char **exec_argv = build_exec_argv(argc, argv);
  if (!exec_argv) {
    return 1;
  }

  if (needs_stdout_normalize(argc, argv)) {
    free(exec_argv);
    return run_piped(argc, argv);
  }

  execv(WIN_ADB, exec_argv);
  perror("execv");
  free(exec_argv);
  return 127;
}
