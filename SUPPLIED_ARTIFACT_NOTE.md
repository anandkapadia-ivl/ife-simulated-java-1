# Supplied reference artifacts

The user supplied:
- heapdump.hprof
- ThreadDump.jstack

These were reviewed as reference material. They are intentionally NOT bundled
into this project because they do not represent the same JVM/application.

The project is designed to generate new, internally consistent artifacts:
- artifacts/ThreadDump-correlated.jstack
- artifacts/heapdump-correlated.hprof
- artifacts/application-correlated.log

Generate those artifacts from one running JVM using scripts/capture-dumps.ps1
(or the equivalent jstack/jmap commands in README.md).
