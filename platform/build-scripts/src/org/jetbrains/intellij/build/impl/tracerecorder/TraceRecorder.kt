// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build.impl.tracerecorder

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.intellij.build.CompilationContext
import org.jetbrains.intellij.build.TestingOptions
import java.nio.file.Files
import java.nio.file.Path

/**
 * Enables attaching Trace Recorder java agent to the tests JVM.
 *
 * The agent argument format:
 * -javaagent:<agentJarPath>=<classFqn>,<methodName>,<traceDumpPath>
 */
@ApiStatus.Internal
interface TraceRecorder {
  fun enable(jvmOptions: MutableList<String>)

  fun publishTraceDump()
}

@ApiStatus.Internal
internal class TraceRecorderImpl(
  private val context: CompilationContext,
  private val options: TestingOptions,
) : TraceRecorder {

  override fun enable(jvmOptions: MutableList<String>) {
    val agentJar = requireNotNull(options.traceRecorderAgentJarPath) {
      "No Trace Recorder agent jar specified (intellij.build.test.trace.recorder.agentJar)"
    }
    val className = requireNotNull(options.traceRecorderClassName) {
      "No class name is specified for trace recording (intellij.build.test.trace.recorder.className)"
    }
    val methodName = requireNotNull(options.traceRecorderMethodName) {
      "No method name is specified for trace recording (intellij.build.test.trace.recorder.methodName)"
    }
    val dumpFile = requireNotNull(options.traceRecorderDumpFilePath) {
      "No trace dump file provided for trace recording (intellij.build.test.trace.recorder.traceDump)"
    }

    val params = "class=$className,method=$methodName,output=$dumpFile,format=text,formatOption=verbose"
    jvmOptions += "-javaagent:$agentJar=${params}"
    jvmOptions += "-Dlincheck.traceRecorderMode=true"
  }

  override fun publishTraceDump() {
    val dump = options.traceRecorderDumpFilePath
      if (dump != null) {
        val dumpPath = Path.of(dump)
        if (Files.exists(dumpPath)) {
          context.notifyArtifactBuilt(dumpPath)
        }
        else {
          context.messages.warning("Trace Recorder dump file '$dump' not found; no artifact published")
        }
      }
  }
}
